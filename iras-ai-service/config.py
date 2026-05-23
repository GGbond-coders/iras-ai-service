"""
IRAS AI Service — 全局配置文件。

所有配置项均支持通过同名环境变量覆盖（os.getenv），
未设置环境变量时使用代码中的默认值。

配置分类：
- LLM：大语言模型和嵌入模型
- API 认证：服务间通信 Token
- MySQL：数据库连接
- ChromaDB：向量库持久化
- 文件上传：简历存储
"""

import os

# =============================================================================
# LLM 配置（通义千问 via OpenAI 兼容接口）
# =============================================================================

# 大语言模型名称（qwen-plus 性价比最优，适合批量任务）
LLM_MODEL = "qwen-plus"
# API Key（从阿里云 DashScope 控制台获取）
LLM_API_KEY = os.getenv("LLM_API_KEY", "*************")
# OpenAI 兼容接口地址
LLM_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
# 嵌入模型（用于文本向量化，每批最多 10 条）
EMBEDDING_MODEL = "text-embedding-v3"

# =============================================================================
# API 认证配置
# =============================================================================

# 服务间通信 Token，需与 Spring Boot 后端 application.yml 中的
# dify.job-profile-key / dify.resume-diagnosis-key 保持一致
SERVICE_TOKEN = os.getenv("SERVICE_TOKEN", "iras-ai-service-token-2024")

# =============================================================================
# MySQL 数据库配置（iras_v2 库，独立于原 IRAS 的 iras 库）
# =============================================================================

MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "1234")
MYSQL_DB = os.getenv("MYSQL_DB", "iras_v2")

# =============================================================================
# ChromaDB 向量库配置
# =============================================================================

# 向量数据持久化目录
CHROMA_PERSIST_DIR = os.getenv("CHROMA_PERSIST_DIR", "./chroma_data")

# =============================================================================
# 文件上传配置
# =============================================================================

# 简历文件存储目录
UPLOAD_DIR = os.getenv("UPLOAD_DIR", "./uploads")
