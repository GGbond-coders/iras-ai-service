"""
IRAS AI Service — 职能画像服务模块。

根据用户输入的职位名称，结合 ChromaDB 向量库中相似岗位的知识，
调用 LLM 生成包含硬技能、软技能、工具、经验、学历五个维度的
结构化 JSON 岗位画像。

核心函数：
- generate_job_profile(job_name, vector_store): 异步生成器，流式产出文本片段

Prompt 设计要点：
- 要求 LLM 输出纯 JSON（不使用 markdown 代码块）
- 五个维度覆盖招聘全要素，便于前端结构化展示
- 可选注入 ChromaDB 检索到的相似 JD 作为上下文（Few-shot 增强）
"""

import logging
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from config import LLM_MODEL, LLM_API_KEY, LLM_BASE_URL

logger = logging.getLogger(__name__)

# 职能画像 Prompt 模板
# {job_name} 由用户输入填充，{job_context} 由 ChromaDB 检索结果填充（可为空）
JOB_PROFILE_PROMPT = ChatPromptTemplate.from_template("""你是一位资深的HR专家和职业分析师。请根据以下职位名称，生成该职位的完整职能画像。

职位名称：{job_name}

{job_context}

请严格按照以下JSON格式输出，不要添加任何额外的说明文字或markdown代码块标记：

{{
  "job_title": "{job_name}",
  "hard_skills": ["技能1", "技能2", ...],
  "soft_skills": ["素质1", "素质2", ...],
  "tools": ["工具1", "工具2", ...],
  "experience": "经验要求描述",
  "education": "学历要求描述"
}}

要求：
- hard_skills：该职位需要的专业技术能力（列出4-8项）
- soft_skills：该职位需要的通用素质和软实力（列出3-5项）
- tools：该职位常用的软件、平台、工具（列出5-8项）
- experience：该职位通常要求的工作年限和项目经验（一句话描述）
- education：该职位通常要求的学历和专业背景（一句话描述）
- job_title：保持与输入一致""")


def _build_llm():
    """
    构建通义千问 LLM 实例。

    使用 langchain-openai 的 ChatOpenAI 封装，通过 OpenAI 兼容接口
    调用阿里云 DashScope 的通义千问模型。

    参数：
        temperature=0.3: 低温度确保输出格式稳定、JSON 结构可靠
    """
    return ChatOpenAI(
        model=LLM_MODEL,
        api_key=LLM_API_KEY,
        base_url=LLM_BASE_URL,
        temperature=0.3,
    )


async def generate_job_profile(job_name: str, vector_store=None):
    """
    异步生成职位职能画像。

    流程：
    1. 可选地从 ChromaDB 检索相似岗位 JD 作为上下文
    2. 构建 Prompt（职位名 + 参考 JD 上下文）
    3. 流式调用 LLM：逐 token yield 返回给调用方
    4. LLM 完成后，对完整输出做清洗去 markdown 标记
    5. 将清洗后的完整 JSON 作为最终输出 yield

    参数：
        job_name: 职位名称（如 "Java开发工程师"）
        vector_store: ChromaDB 向量库模块（可选，为 None 时仅基于 LLM 知识生成）

    Yields:
        str: LLM 生成的文本片段（逐 token），最后 yield 完整清洗后的 JSON
    """
    # 构建参考 JD 上下文：从 ChromaDB 检索 top-5 相似岗位
    job_context = ""
    if vector_store:
        try:
            logger.info(f"[JobProfile] Searching ChromaDB for '{job_name}' (top_k=5)...")
            results = vector_store.search(job_name, top_k=5)
            if results:
                jd_texts = []
                for r in results:
                    meta = r.get("metadata", {})
                    jd_texts.append(f"- {meta.get('job_name', '')} @ {meta.get('company_name', '')}: {r.get('text', '')[:500]}")
                job_context = "以下是与该职位相关的参考岗位描述（来自数据库）：\n" + "\n".join(jd_texts)
                logger.info(f"[JobProfile] ChromaDB returned {len(results)} similar jobs as context")
            else:
                logger.info("[JobProfile] ChromaDB: no similar jobs found, using LLM knowledge only")
        except Exception as e:
            # 向量检索失败不阻塞主流程，降级为无上下文生成
            logger.warning(f"[JobProfile] ChromaDB search failed: {e}, falling back to LLM-only generation")
    else:
        logger.info("[JobProfile] No vector_store available, using LLM knowledge only")

    llm = _build_llm()
    prompt = JOB_PROFILE_PROMPT.format(job_name=job_name, job_context=job_context)

    # 流式调用 LLM，逐 token 产出
    logger.info("[JobProfile] Starting LLM streaming generation...")
    full_text = ""
    chunk_count = 0
    async for chunk in llm.astream(prompt):
        text = chunk.content if hasattr(chunk, "content") else str(chunk)
        full_text += text
        chunk_count += 1
        yield text

    logger.info(f"[JobProfile] LLM streaming done: {chunk_count} chunks, {len(full_text)} chars total")

    # 清洗 LLM 输出：去掉可能存在的 markdown 代码块标记（```json ... ```）
    clean = full_text.strip()
    if clean.startswith("```"):
        logger.info("[JobProfile] Stripping markdown code block markers from output")
        lines = clean.split("\n")
        lines = [l for l in lines if not l.startswith("```")]
        clean = "\n".join(lines).strip()

    # 最终产出：清洗后的完整 JSON（作为 workflow_finished 的 result）
    logger.info(f"[JobProfile] Final output: {len(clean)} chars")
    yield clean
