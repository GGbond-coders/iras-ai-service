"""
IRAS AI Service — 应用入口模块。

本模块是 FastAPI 应用的组装入口，负责：
1. 创建 FastAPI 实例并配置 CORS 中间件
2. 管理应用生命周期（启动时创建目录、关闭时清理资源）
3. 挂载路由（Dify 兼容的 /v1 端点 + 管理端点）
4. 暴露健康检查和管理接口

启动方式：
    python -m uvicorn main:app --host 127.0.0.1 --port 8000
"""

import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes import workflows, files
from config import UPLOAD_DIR, CHROMA_PERSIST_DIR

# 全局日志配置：时间戳 + 级别 + 模块名 + 消息
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI 生命周期管理器。

    启动阶段（yield 之前）：
    - 创建上传目录和 ChromaDB 持久化目录
    - ChromaDB 不在此自动重建（需要 embedding API，可能启动时不可达）

    关闭阶段（yield 之后）：
    - 目前为空，预留资源清理入口
    """
    os.makedirs(UPLOAD_DIR, exist_ok=True)
    os.makedirs(CHROMA_PERSIST_DIR, exist_ok=True)
    logger.info(f"Upload dir: {UPLOAD_DIR}")
    logger.info(f"ChromaDB dir: {CHROMA_PERSIST_DIR}")
    yield


# FastAPI 应用实例
app = FastAPI(title="IRAS AI Service", version="1.0.0", lifespan=lifespan)

# CORS 中间件：允许所有来源跨域访问（开发环境）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载 Dify 兼容路由（前缀 /v1）
app.include_router(workflows.router, prefix="/v1")
app.include_router(files.router, prefix="/v1")


@app.get("/health")
async def health():
    """健康检查端点，供外部监控探活。"""
    return {"status": "ok"}


@app.post("/admin/chromadb/sync")
async def sync_chromadb():
    """
    增量同步 ChromaDB 向量库。

    从 MySQL 读取上次同步后有变化的岗位，upsert 到 ChromaDB，
    同时清理 MySQL 中已不存在的孤儿向量。

    由管理员通过前端管理面板手动触发。
    """
    from kb.vector_store import sync_jobs
    try:
        result = sync_jobs()
        # 构建可读的响应消息：更新 X 条 + 清理 Y 条残留
        parts = [f"更新 {result['jobs_upserted']} 条"]
        if result.get("orphans_cleaned", 0):
            parts.append(f"清理 {result['orphans_cleaned']} 条残留")
        return {"status": "ok", "message": "，".join(parts)}
    except Exception as e:
        return {"status": "error", "message": str(e)}
