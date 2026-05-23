"""
IRAS AI Service — AI 工作流路由模块。

本模块实现了与 Dify 100% 兼容的 SSE 流式响应接口，
是 IRAS 主系统与本服务之间的核心通信桥梁。

核心功能：
- POST /v1/workflows/run：执行 AI 工作流（职能画像 / 简历诊断）
- SSE 事件编码：将 AI 输出封装为 Dify 格式的 SSE 事件流

SSE 事件格式（必须精确匹配）：
    data: {"event":"workflow_started","data":{"id":"..."}}\n\n
    data: {"event":"text_chunk","data":{"text":"..."}}\n\n
    data: {"event":"workflow_finished","data":{"outputs":{"result":"..."}}}\n\n
    data: [DONE]\n\n

关键设计决策：
- "收集再分发"模式：先收集 LLM 全部输出，最后一片作为 final 放入
  workflow_finished，其余作为 text_chunk 逐片发给前端实现打字机效果
- 异常通过 workflow_finished 事件传递，不抛出 HTTP 错误（SSE 流已启动后
  无法修改 HTTP 状态码）
"""

import json
import asyncio
import logging
from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import StreamingResponse
from models.schemas import (
    WorkflowRequest,
    WorkflowStartedEvent,
    TextChunkEvent,
    WorkflowFinishedEvent,
)
from config import SERVICE_TOKEN

logger = logging.getLogger(__name__)
router = APIRouter()


def sse_encode(event) -> str:
    """
    将事件对象编码为 Dify 兼容的 SSE 格式字符串。

    参数：
        event: WorkflowStartedEvent / TextChunkEvent / WorkflowFinishedEvent 实例

    返回：
        "data: {json}\n\n" 格式的 SSE 数据行，
        ensure_ascii=False 确保中文不转义为 \\uXXXX
    """
    data = json.dumps(event.__dict__, ensure_ascii=False)
    return f"data: {data}\n\n"


async def verify_token(request: Request) -> None:
    """
    验证请求中的 Bearer Token。

    从 Authorization 头中提取 token，与 SERVICE_TOKEN 比对。
    不匹配时抛出 401 HTTPException。

    参数：
        request: FastAPI Request 对象

    异常：
        HTTPException(401): token 缺失或无效
    """
    authorization = request.headers.get("Authorization", "")
    if not authorization.startswith("Bearer "):
        logger.warning("Token verification failed: missing or malformed Authorization header")
        raise HTTPException(status_code=401, detail="Missing or invalid token")
    if authorization.removeprefix("Bearer ") != SERVICE_TOKEN:
        logger.warning("Token verification failed: invalid token")
        raise HTTPException(status_code=401, detail="Invalid token")
    logger.info("Token verification passed")


@router.post("/workflows/run")
async def run_workflow(request: WorkflowRequest, req: Request):
    """
    Dify 兼容的 Workflow 执行入口。

    根据 inputs 内容自动路由到对应工作流：
    - 含 job_name → 职能画像（_run_job_profile）
    - 含 resume_text → 简历诊断（_run_resume_diagnosis）
    - 都不含 → 400 错误

    返回 SSE 流式响应（text/event-stream）。
    """
    await verify_token(req)
    inputs = request.inputs
    logger.info(f"Workflow request received, inputs keys: {list(inputs.keys())}")

    # 路由分发：根据 inputs 的 key 决定执行哪个工作流
    if "job_name" in inputs:
        job_name = inputs["job_name"]
        logger.info(f"Routing to job_profile workflow, job_name='{job_name}'")
        generator = _run_job_profile(job_name)
    elif "resume_text" in inputs:
        # Dify 的 resume_text 是文件引用数组：[{"upload_file_id": "xxx", ...}]
        file_ref = inputs["resume_text"]
        file_id = None
        if isinstance(file_ref, list) and len(file_ref) > 0:
            file_id = file_ref[0].get("upload_file_id", "")
        logger.info(f"Routing to resume_diagnosis workflow, file_id='{file_id}'")
        generator = _run_resume_diagnosis(file_id)
    else:
        logger.warning(f"Unknown workflow inputs: {list(inputs.keys())}")
        raise HTTPException(status_code=400, detail="Unknown workflow inputs")

    async def event_stream():
        """
        SSE 事件流生成器。

        逐块产出 AI 输出，最后发送 [DONE] 结束信号。
        使用 async for 确保不阻塞事件循环。
        """
        async for chunk in generator:
            yield chunk
        # Dify SSE 协议：流必须以 [DONE] 结束
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",       # 禁止浏览器缓存
            "Connection": "keep-alive",         # 保持连接
            "X-Accel-Buffering": "no",          # 禁用 Nginx 代理缓冲
        },
    )


async def _run_job_profile(job_name: str):
    """
    执行职能画像工作流并产出 SSE 事件。

    流程：
    1. 发送 workflow_started 事件
    2. 调用 generate_job_profile() 收集所有 LLM 输出片段
    3. 除最后一片外，逐片发送 text_chunk 事件（前端打字机效果）
    4. 最后一片作为 workflow_finished 事件的 result 发出
    5. 异常时发送含 error 信息的 workflow_finished 事件

    参数：
        job_name: 职位名称（如 "Java开发工程师"）
    """
    import uuid
    from services.job_profile import generate_job_profile

    workflow_id = str(uuid.uuid4())
    logger.info(f"[JobProfile] workflow_id={workflow_id}, job_name='{job_name}' — started")
    yield sse_encode(WorkflowStartedEvent(workflow_id))

    from kb import vector_store
    try:
        # 收集全部 LLM 输出片段
        all_pieces = []
        async for piece in generate_job_profile(job_name, vector_store=vector_store):
            all_pieces.append(piece)

        logger.info(f"[JobProfile] LLM completed, collected {len(all_pieces)} pieces")

        # 除最后一片外，其余作为 text_chunk 逐片发出
        for piece in all_pieces[:-1]:
            yield sse_encode(TextChunkEvent(piece))
            await asyncio.sleep(0.01)  # 让出事件循环，确保 SSE 及时 flush

        # 最后一片（完整清洗后的 JSON）作为最终结果
        final_result = all_pieces[-1] if all_pieces else ""
        logger.info(f"[JobProfile] finished, result length={len(final_result)} chars")
        yield sse_encode(WorkflowFinishedEvent(final_result))
    except Exception as e:
        # 异常通过 workflow_finished 事件传递，不打断 SSE 流
        logger.error(f"[JobProfile] error: {e}")
        error_result = json.dumps({"error": str(e)}, ensure_ascii=False)
        yield sse_encode(WorkflowFinishedEvent(error_result))


async def _run_resume_diagnosis(file_id: str | None):
    """
    执行简历诊断工作流并产出 SSE 事件。

    流程：
    1. 发送 workflow_started 事件
    2. 校验 file_id → 为空直接返回错误
    3. 解析简历文件（PDF/DOCX/TXT → 纯文本）→ 失败返回错误
    4. ChromaDB 检索 top-10 匹配岗位 → 构建诊断 Prompt
    5. LLM 流式生成诊断报告 → 收集所有片段
    6. 除最后一片外作为 text_chunk，最后一片作为 workflow_finished 结果

    参数：
        file_id: 上传简历时返回的文件 UUID
    """
    import uuid
    from services.resume_parser import parse_resume
    from services.resume_diagnosis import diagnose_resume

    workflow_id = str(uuid.uuid4())
    logger.info(f"[ResumeDiagnosis] workflow_id={workflow_id}, file_id='{file_id}' — started")
    yield sse_encode(WorkflowStartedEvent(workflow_id))

    # 文件 ID 校验
    if not file_id:
        logger.warning("[ResumeDiagnosis] file_id is empty, returning error")
        yield sse_encode(WorkflowFinishedEvent(json.dumps(
            [{"matched_job": "错误", "matched_score": 0, "matched_reason": "未提供文件ID",
              "gap_points": [], "interview_advice": []}],
            ensure_ascii=False)))
        return

    # 简历文件解析（支持 PDF/DOCX/TXT）
    try:
        logger.info("[ResumeDiagnosis] Step 1/3: parsing resume file...")
        resume_text = parse_resume(file_id)
        logger.info(f"[ResumeDiagnosis] Step 1/3: resume parsed, {len(resume_text)} chars")
    except Exception as e:
        logger.error(f"[ResumeDiagnosis] Step 1/3 failed: {e}")
        yield sse_encode(WorkflowFinishedEvent(json.dumps(
            [{"matched_job": "错误", "matched_score": 0, "matched_reason": f"简历解析失败: {e}",
              "gap_points": [], "interview_advice": []}],
            ensure_ascii=False)))
        return

    # LLM 诊断（含 ChromaDB 岗位检索）
    from kb import vector_store
    try:
        logger.info("[ResumeDiagnosis] Step 2/3: LLM keyword extraction + ChromaDB search...")
        all_pieces = []
        async for piece in diagnose_resume(resume_text, vector_store=vector_store):
            all_pieces.append(piece)

        logger.info(f"[ResumeDiagnosis] Step 2/3: diagnosis completed, {len(all_pieces)} pieces")

        for piece in all_pieces[:-1]:
            yield sse_encode(TextChunkEvent(piece))
            await asyncio.sleep(0.01)

        final_result = all_pieces[-1] if all_pieces else ""
        logger.info(f"[ResumeDiagnosis] Step 3/3: finished, result length={len(final_result)} chars")
        yield sse_encode(WorkflowFinishedEvent(final_result))
    except Exception as e:
        logger.error(f"[ResumeDiagnosis] LLM error: {e}")
        error_result = json.dumps({"error": str(e)}, ensure_ascii=False)
        yield sse_encode(WorkflowFinishedEvent(error_result))
