"""
IRAS AI Service — 简历诊断服务模块。

根据候选人简历文本，从 ChromaDB 向量库中检索 top-10 匹配岗位，
调用 LLM 对每个匹配岗位生成诊断报告（匹配分 + 原因 + 差距分析 + 面试建议）。

核心函数：
- diagnose_resume(resume_text, vector_store): 异步生成器，流式产出诊断文本

诊断输出格式：
    {"result": "<think>推理过程</think>[{...匹配岗位1...}, {...匹配岗位2...}]"}

外层 {"result": ...} 包装是前端历史记录展示的约定格式，
内层数组是岗位匹配结果列表，每个元素包含 9 个字段供前端渲染。

Prompt 设计要点：
- 使用 <think>...</think> 标签分离推理过程和结构化输出，
  前端可选择展示/隐藏推理过程
- 匹配 1-3 个岗位，每个岗位 9 个维度，覆盖招聘顾问全流程
"""

import json
import re
import logging
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from config import LLM_MODEL, LLM_API_KEY, LLM_BASE_URL

logger = logging.getLogger(__name__)

# 关键词提取 Prompt：从简历中提取用于岗位检索的核心关键词
# 要求输出一句简洁的搜索字符串，直接送入 ChromaDB 做语义检索
KEYWORD_EXTRACTION_PROMPT = ChatPromptTemplate.from_template("""你是一位招聘搜索引擎专家。请从以下简历中提取用于岗位匹配检索的核心关键词。

候选人简历：
{resume_text}

请提取以下维度的关键词，并拼接成一段简洁的检索文本（不要用列表，用空格分隔的关键词短语）：

1. 技术技能：编程语言、框架、工具、平台
2. 职位方向：目标岗位名称
3. 行业领域：熟悉的业务领域
4. 经验资历：工作年限、学历、证书

要求：
- 输出一段20-50字的检索文本
- 用空格或逗号分隔不同的关键词短语
- 包含英文缩写和中文全称
- 不要加任何前缀说明

输出示例：
Java Spring Boot MySQL 后端开发 3年经验 金融科技 本科 计算机""")

# 简历诊断 Prompt 模板
# {resume_text} 由简历解析结果填充，{job_list} 由 ChromaDB 检索结果填充
DIAGNOSIS_PROMPT = ChatPromptTemplate.from_template("""你是一位资深的招聘顾问和简历分析师。请根据候选人的简历内容，从数据库岗位列表中匹配最合适的岗位，并生成详细的诊断报告。

注意：请先将你的推理过程放在 <think>...</think> 标签内，然后在标签外输出最终的JSON数组。不要在JSON数组外添加任何其他文字。

## 候选人简历
{resume_text}

## 可匹配的岗位列表
{job_list}

## 分析要求
对简历与每个岗位进行逐一匹配，匹配1-3个最合适的岗位。每个岗位输出以下字段：
1. "matched_job"：匹配的岗位名称（含公司名）
2. "matched_score"：匹配分数（0-100的整数，综合考虑技能、经验、学历匹配度）
3. "matched_reason"：匹配原因（2-3句话，说明为什么匹配）
4. "gap_points"：差距分析（列出2-4条候选人欠缺的技能或经验）
5. "interview_advice"：面试建议（列出2-4条面试准备方向）
6. "type"：岗位类型
7. "company"：公司名称
8. "city"：工作城市
9. "salary"：薪资范围

## 输出格式
<think>
（在这里写你的推理过程：分析候选人技能、逐一对比岗位要求、计算匹配度）
</think>
[
  {{
    "matched_job": "...（公司名）",
    "matched_score": 85,
    "matched_reason": "...",
    "gap_points": ["...", "..."],
    "interview_advice": ["...", "..."],
    "type": "全职",
    "company": "...",
    "city": "...",
    "salary": "..."
  }}
]

如果岗位库中没有匹配的岗位，请在think中说明原因，并输出空数组 []。""")


def _build_llm():
    """
    构建通义千问 LLM 实例。

    参数：
        temperature=0.3: 低温度确保诊断格式稳定、JSON 可解析
    """
    return ChatOpenAI(
        model=LLM_MODEL,
        api_key=LLM_API_KEY,
        base_url=LLM_BASE_URL,
        temperature=0.3,
    )


def _build_search_llm():
    """
    构建关键词提取专用 LLM 实例。

    参数：
        temperature=0.1: 极低温度，确保关键词提取结果稳定、可复现
    """
    return ChatOpenAI(
        model=LLM_MODEL,
        api_key=LLM_API_KEY,
        base_url=LLM_BASE_URL,
        temperature=0.1,
    )


async def extract_keywords(resume_text: str) -> str:
    """
    从简历中提取检索关键词，用于 ChromaDB 语义搜索。

    使用 LLM 将简历蒸馏为 20-50 字的检索关键词串，
    过滤掉简历中的噪音信息（个人信息、公司名、学校名等），
    保留技能、职位方向、行业领域、资历级别等核心维度。

    参数：
        resume_text: 简历解析后的纯文本

    返回：
        检索关键词串（如 "Java Spring Boot 后端开发 3年 本科"）
    """
    llm = _build_search_llm()
    prompt = KEYWORD_EXTRACTION_PROMPT.format(resume_text=resume_text)
    response = await llm.ainvoke(prompt)
    keywords = response.content.strip()
    logger.info(f"Extracted keywords: {keywords}")
    return keywords


async def diagnose_resume(resume_text: str, vector_store=None):
    """
    异步执行简历诊断，流式返回诊断报告。

    流程（改进版）：
    1. LLM 从简历中提取检索关键词（过滤噪音，保留技能/职位/行业/资历）
    2. 用关键词在 ChromaDB 语义检索 top-10 匹配岗位
    3. 构建 Prompt（简历文本 + 岗位列表）
    4. 流式调用 LLM：逐 token yield
    5. 对完整输出清洗去 markdown 标记
    6. 包装为 {"result": "..."} 格式 yield（前端约定的展示格式）

    参数：
        resume_text: 简历解析后的纯文本内容
        vector_store: ChromaDB 向量库模块（可选，为 None 时使用 LLM 通用知识匹配）

    Yields:
        str: LLM 生成的文本片段（逐 token），最后 yield 包装后的完整 JSON
    """
    # Step 1: LLM 提取检索关键词（替代直接用简历全文检索的旧方案）
    job_list_text = "（暂无岗位数据，请基于通用市场知识进行分析）"
    if vector_store:
        try:
            logger.info("[Diagnosis] Extracting search keywords from resume...")
            search_query = await extract_keywords(resume_text)
            logger.info(f"[Diagnosis] Searching ChromaDB with keywords (top_k=10): '{search_query}'")
            results = vector_store.search(search_query, top_k=10)
            if results:
                logger.info(f"[Diagnosis] ChromaDB returned {len(results)} matching jobs")
                items = []
                for i, r in enumerate(results):
                    meta = r.get("metadata", {})
                    items.append(
                        f"{i + 1}. {meta.get('job_name', '')} | {meta.get('company_name', '')} "
                        f"| {meta.get('city', '')} | {meta.get('salary', '')}\n"
                        f"   JD: {r.get('text', '')[:600]}"  # 每个 JD 截取前 600 字符
                    )
                job_list_text = "\n\n".join(items)
            else:
                logger.info("[Diagnosis] ChromaDB: no matching jobs found")
        except Exception as e:
            # 向量检索失败不阻塞主流程
            logger.warning(f"[Diagnosis] ChromaDB search failed: {e}, using LLM knowledge only")
    else:
        logger.info("[Diagnosis] No vector_store available, using LLM knowledge only")

    llm = _build_llm()
    prompt = DIAGNOSIS_PROMPT.format(resume_text=resume_text, job_list=job_list_text)

    # 流式调用 LLM
    logger.info("[Diagnosis] Starting LLM streaming diagnosis generation...")
    full_text = ""
    chunk_count = 0
    async for chunk in llm.astream(prompt):
        text = chunk.content if hasattr(chunk, "content") else str(chunk)
        full_text += text
        chunk_count += 1
        yield text

    logger.info(f"[Diagnosis] LLM streaming done: {chunk_count} chunks, {len(full_text)} chars total")

    # 清洗 markdown 代码块标记
    clean = full_text.strip()
    if clean.startswith("```"):
        logger.info("[Diagnosis] Stripping markdown code block markers from output")
        lines = clean.split("\n")
        lines = [l for l in lines if not l.startswith("```")]
        clean = "\n".join(lines).strip()

    # 包装为前端约定的 {"result": "..."} 格式
    # 前端 History.vue 和 Diagnosis.vue 均按此格式解析
    wrapped = json.dumps({"result": clean}, ensure_ascii=False)
    logger.info(f"[Diagnosis] Final wrapped result: {len(wrapped)} chars")
    yield wrapped
