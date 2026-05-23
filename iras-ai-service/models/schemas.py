"""
IRAS AI Service — Pydantic 数据模型模块。

定义所有 API 请求/响应的数据结构，以及 SSE 事件对象。
与 Dify 的请求体和 SSE 事件格式 100% 兼容。

模型分类：
- 请求模型：WorkflowRequest（对应 Dify workflow 执行请求）
- 响应模型：FileUploadResponse（文件上传返回）
- SSE 事件模型：WorkflowStartedEvent / TextChunkEvent / WorkflowFinishedEvent
  （使用普通类而非 Pydantic BaseModel，因为 __dict__ 序列化即可满足 SSE 需求，
   且普通类构造更轻量，无 Pydantic 校验开销）
"""

from __future__ import annotations
import uuid
from typing import Any
from pydantic import BaseModel, Field


class WorkflowRequest(BaseModel):
    """
    Dify 兼容的 Workflow 执行请求体。

    字段：
        inputs: 工作流输入参数，key 由 Dify 工作流定义决定。
                - 含 "job_name" → 路由到职能画像
                - 含 "resume_text" → 路由到简历诊断（值为文件引用数组）
        response_mode: 响应模式，固定 "streaming"（SSE 流式）
        user: 用户标识，默认 "iras-user"
    """
    inputs: dict[str, Any]
    response_mode: str = "streaming"
    user: str = "iras-user"


class FileUploadResponse(BaseModel):
    """
    文件上传成功响应，与 Dify /v1/files/upload 返回格式一致。

    字段：
        id: 文件唯一标识（UUID），后续 workflow 请求中 resume_text 字段
            引用此 ID 来定位已上传的简历文件
    """
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))


class WorkflowStartedEvent:
    """
    SSE workflow_started 事件。

    Dify 格式：{"event": "workflow_started", "data": {"id": "..."}}
    必须作为 SSE 流的第一个事件发送，告知前端工作流已开始执行。
    """

    def __init__(self, workflow_id: str | None = None):
        self.event = "workflow_started"
        self.data = {"id": workflow_id or str(uuid.uuid4())}


class TextChunkEvent:
    """
    SSE text_chunk 事件。

    Dify 格式：{"event": "text_chunk", "data": {"text": "..."}}
    用于向前端推送 LLM 流式输出的增量文本片段，实现打字机效果。
    """

    def __init__(self, text: str):
        self.event = "text_chunk"
        self.data = {"text": text}


class WorkflowFinishedEvent:
    """
    SSE workflow_finished 事件。

    Dify 格式：{"event": "workflow_finished", "data": {"outputs": {"result": "..."}}}
    作为 SSE 流的倒数第二个事件发送（最后一个事件是 [DONE]），
    携带工作流的最终输出结果。前端收到此事件后停止等待新内容。
    """

    def __init__(self, result: str):
        self.event = "workflow_finished"
        self.data = {"outputs": {"result": result}}
