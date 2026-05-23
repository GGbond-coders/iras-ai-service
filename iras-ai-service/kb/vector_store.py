"""
IRAS AI Service — ChromaDB 向量库管理模块。

封装 ChromaDB 的初始化、增删改查和增量同步逻辑，是 AI 工作流
与岗位知识库之间的核心桥梁。

核心功能：
- 向量库生命周期管理（客户端创建、Collection 获取）
- 增量同步：基于时间戳，仅同步自上次同步以来有变化的岗位
- 孤儿清理：检测并删除 MySQL 中已不存在的向量（硬删除场景）
- 语义检索：给定查询文本，返回 top-k 最相似岗位

架构设计：
- 使用 ChromaDB PersistentClient（本地磁盘持久化）
- 嵌入函数使用阿里云 text-embedding-v3（via OpenAI 兼容接口）
- 每批最多 10 条文档调用嵌入 API（DashScope 硬限制）
- 全局单例模式：_client 和 _collection 进程级复用

增量同步策略（v1）：
- 首次同步（无 last_sync.txt）：upsert 全部 active 岗位
- 后续同步：仅 upsert create_time/update_time >= last_sync 的岗位
- 软删除：status != "active" 且 update_time >= last_sync 的从向量库删除
- 硬删除：对比 ChromaDB 现有 ID 与 MySQL active ID 集合，清理孤儿向量

同步触发方式：
- Spring Boot 管理后台 → HTTP 代理 → Python /admin/chromadb/sync
- 管理员在后台手动点击"同步向量库"按钮触发
"""

import os
import logging
import time
import chromadb
from chromadb.config import Settings
from chromadb.utils.embedding_functions import OpenAIEmbeddingFunction
from config import CHROMA_PERSIST_DIR, LLM_API_KEY, LLM_BASE_URL, EMBEDDING_MODEL

logger = logging.getLogger(__name__)

# ChromaDB Collection 名称（整个服务共用一个 collection）
COLLECTION_NAME = "iras_jobs"

# 嵌入 API 批次大小（DashScope text-embedding-v3 限制每次最多 10 条文本）
BATCH_SIZE = 10

# 全局单例缓存
_client = None
_collection = None


def _get_embedding_fn():
    """
    创建 OpenAI 兼容的嵌入函数实例。

    使用阿里云 DashScope 的 text-embedding-v3 模型，
    通过 OpenAI 兼容接口调用，无需额外的 DashScope SDK。

    返回：
        OpenAIEmbeddingFunction 实例
    """
    return OpenAIEmbeddingFunction(
        api_key=LLM_API_KEY,
        api_base=LLM_BASE_URL,
        model_name=EMBEDDING_MODEL,
    )


def _get_client():
    """
    获取或创建 ChromaDB 持久化客户端（懒加载单例）。

    数据存储在 CHROMA_PERSIST_DIR 目录下，进程重启后数据不丢失。
    anonymized_telemetry=False 禁用遥测数据上报。
    """
    global _client
    if _client is None:
        _client = chromadb.PersistentClient(
            path=CHROMA_PERSIST_DIR,
            settings=Settings(anonymized_telemetry=False),
        )
    return _client


def get_collection():
    """
    获取或创建 ChromaDB Collection（懒加载单例）。

    使用余弦相似度（cosine）作为向量距离度量，适合文本语义匹配场景。
    首次调用时自动创建 collection 并绑定嵌入函数。
    """
    global _collection
    if _collection is None:
        client = _get_client()
        emb_fn = _get_embedding_fn()
        _collection = client.get_or_create_collection(
            name=COLLECTION_NAME,
            embedding_function=emb_fn,
            metadata={"hnsw:space": "cosine"},  # 余弦距离，语义匹配效果最佳
        )
    return _collection


def _sync_file():
    """获取增量同步时间戳文件的路径。"""
    return os.path.join(CHROMA_PERSIST_DIR, "last_sync.txt")


def _read_last_sync() -> float:
    """
    读取上次同步时间戳。

    返回：
        float: Unix 时间戳（秒），首次同步返回 0.0（表示全量同步）
    """
    path = _sync_file()
    if os.path.exists(path):
        try:
            return float(open(path).read().strip())
        except (ValueError, OSError):
            pass
    return 0.0


def _write_last_sync(ts: float):
    """写入本次同步完成的时间戳。"""
    with open(_sync_file(), "w") as f:
        f.write(str(ts))


def _batch_upsert(jobs):
    """
    分批 upsert 岗位向量到 ChromaDB。

    将岗位列表按 BATCH_SIZE（10）分批，每批调用一次 ChromaDB upsert。
    ChromaDB 的 upsert 操作是幂等的：ID 存在则更新向量，不存在则新增。

    参数：
        jobs: JobInfo ORM 实例列表
    """
    collection = get_collection()
    for i in range(0, len(jobs), BATCH_SIZE):
        batch = jobs[i : i + BATCH_SIZE]
        ids = [str(j.id) for j in batch]
        documents = [j.jd_text or "" for j in batch]
        metadatas = [
            {
                "job_name": j.job_name or "",
                "company_name": j.company_name or "",
                "city": j.city or "",
                "salary": j.salary or "",
            }
            for j in batch
        ]
        collection.upsert(ids=ids, documents=documents, metadatas=metadatas)


def sync_jobs():
    """
    增量同步岗位到 ChromaDB（核心入口函数）。

    同步逻辑：
    1. 读取上次同步时间戳
    2. 首次同步（时间戳为 0）：upsert 全部 status=active 的岗位
    3. 增量同步：upsert 自上次以来有变化（create_time/update_time >= last_sync）的岗位
    4. 软删除处理：status 变为非 active 的从向量库中删除
    5. 孤儿清理：对比 ChromaDB 现有 ID 与 MySQL active ID 集合，
       删除 MySQL 中已硬删除的岗位对应的向量
    6. 更新同步时间戳

    返回：
        dict: {"jobs_upserted": N, "orphans_cleaned": N}
        - jobs_upserted: 本次更新的岗位数（含新增和变更）
        - orphans_cleaned: 清理的孤儿向量数（MySQL 中已删除的）

    注意：
        此函数由 main.py 的 /admin/chromadb/sync 端点调用，
        也由 Spring Boot 管理后台通过代理接口间接调用。
    """
    from db.database import SessionLocal
    from db.models import JobInfo
    from sqlalchemy import or_

    db = SessionLocal()
    try:
        last_sync = _read_last_sync()

        if last_sync == 0.0:
            # 首次同步：全量 upsert 所有 active 岗位
            jobs = db.query(JobInfo).filter(JobInfo.status == "active").all()
            logger.info(f"First sync: upserting all {len(jobs)} active jobs")
        else:
            # 增量同步：仅处理自上次同步以来有变化的岗位
            last_dt = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(last_sync))
            jobs = db.query(JobInfo).filter(
                JobInfo.status == "active",
                or_(
                    JobInfo.create_time >= last_dt,
                    JobInfo.update_time >= last_dt,
                ),
            ).all()
            logger.info(f"Incremental sync: {len(jobs)} changed jobs")

            # 处理软删除：状态变为非 active 的从向量库中移除
            expired = db.query(JobInfo).filter(
                JobInfo.status != "active",
                JobInfo.update_time >= last_dt,
            ).all()
            if expired:
                delete_jobs([j.id for j in expired])
                logger.info(f"Removed {len(expired)} expired jobs")

        # 分批写入向量库
        if jobs:
            _batch_upsert(jobs)

        # 孤儿清理：MySQL 中已硬删除的，向量库中也要删除
        orphans_cleaned = 0
        active_ids = {str(r[0]) for r in db.query(JobInfo.id).filter(JobInfo.status == "active").all()}
        collection = get_collection()
        existing = collection.get()
        if existing and existing["ids"]:
            orphans = [vid for vid in existing["ids"] if vid not in active_ids]
            if orphans:
                collection.delete(ids=orphans)
                orphans_cleaned = len(orphans)
                logger.info(f"Cleaned {orphans_cleaned} orphan vectors")

        # 记录本次同步时间戳
        now = time.time()
        _write_last_sync(now)
        logger.info(f"Sync complete: {len(jobs)} upserted, {orphans_cleaned} cleaned")

        return {"jobs_upserted": len(jobs), "orphans_cleaned": orphans_cleaned}
    finally:
        db.close()


def add_jobs(job_ids: list[int]):
    """
    新增指定岗位的向量（按 ID 列表）。

    从 MySQL 读取指定 ID 的岗位，批量 upsert 到 ChromaDB。
    适用于管理后台新增岗位后的即时同步场景。

    参数：
        job_ids: 需要添加向量的岗位 ID 列表
    """
    from db.database import SessionLocal
    from db.models import JobInfo

    db = SessionLocal()
    try:
        jobs = db.query(JobInfo).filter(JobInfo.id.in_(job_ids)).all()
        if jobs:
            _batch_upsert(jobs)
            logger.info(f"ChromaDB upserted: {len(jobs)} jobs")
    finally:
        db.close()


def delete_jobs(job_ids: list[int]):
    """
    从 ChromaDB 删除指定岗位的向量。

    参数：
        job_ids: 需要删除的岗位 ID 列表
    """
    collection = get_collection()
    collection.delete(ids=[str(jid) for jid in job_ids])
    logger.info(f"ChromaDB deleted: {len(job_ids)} jobs")


def search(query: str, top_k: int = 5) -> list[dict]:
    """
    语义检索最相似的岗位。

    使用 ChromaDB 的 query 方法，基于余弦相似度检索与查询文本
    最相关的前 top_k 个岗位向量。

    参数：
        query: 查询文本（职位名或简历全文）
        top_k: 返回结果数量，默认 5（简历诊断时使用 top_k=10）

    返回：
        list[dict]: 搜索结果列表，每项包含：
            - id: 岗位 ID（字符串，对应 MySQL job_info.id）
            - text: 岗位描述全文（用于构建 Prompt 上下文）
            - metadata: 岗位元数据（job_name, company_name, city, salary）
            - distance: 向量距离（越小越相似，用于调试）

    注意：
        返回列表可能为空（向量库为空或 ChromaDB 尚未初始化时）。
    """
    collection = get_collection()
    results = collection.query(query_texts=[query], n_results=top_k)

    items = []
    if results and results["ids"] and results["ids"][0]:
        for i, doc_id in enumerate(results["ids"][0]):
            items.append({
                "id": doc_id,
                "text": results["documents"][0][i] if results.get("documents") else "",
                "metadata": results["metadatas"][0][i] if results.get("metadatas") else {},
                "distance": results["distances"][0][i] if results.get("distances") else None,
            })
    return items
