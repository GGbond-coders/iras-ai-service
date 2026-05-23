# IRAS AI Service 设计方案

> 版本：v1.0 | 日期：2026-05-23 | 状态：待审核

## 1. 背景与目标

当前 IRAS 系统依赖 Dify 平台提供 AI 能力，存在两个问题：

1. Dify 知识库无法程序化自动更新，岗位数据时效性无法保证
2. 职位数据依赖手动录入，缺乏自动采集能力

**目标**：新建独立 Python 服务 `iras-ai-service`，使用 LangChain + 通义千问复现 Dify 的功能，同时内置开放数据聚合器自动维护岗位数据。

**约束**：
- 原 IRAS 项目（`iras-project-main`）除 `application.yml` 中一行 URL 外不做任何改动
- 独立数据库，不污染原项目数据
- 答辩前保持系统稳定

## 2. 整体架构

```
iras-project-extended/
├── iras-project-main/               # 原项目（不动）
│   └── backend/src/main/resources/
│       └── application.yml          # 只改 dify.base-url 一行
│
└── iras-ai-service/                 # 新 Python AI 服务
    ├── main.py                      # FastAPI 入口
    ├── config.py                    # 配置（LLM key、DB、向量库路径）
    ├── routes/
    │   ├── workflows.py             # POST /v1/workflows/run（SSE 流式）
    │   └── files.py                 # POST /v1/files/upload
    ├── services/
    │   ├── job_profile.py           # 职能画像 LangChain 工作流
    │   ├── resume_diagnosis.py      # 简历诊断 LangChain 工作流
    │   └── aggregator/
    │       ├── engine.py            # APScheduler 调度引擎
    │       ├── base.py              # 采集器抽象基类
    │       ├── sources/ncss.py      # 24365 国家就业平台采集器
    │       ├── dedup.py             # (job_name + company) 去重
    │       └── pipeline.py          # 采集 → 清洗 → 入库 → 同步向量库
    ├── kb/
    │   ├── vector_store.py          # ChromaDB 管理
    │   └── loader.py               # 文档加载、分块、向量化
    ├── models/
    │   └── schemas.py               # Pydantic 模型
    ├── requirements.txt
    └── tests/
```

### 运行时架构

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Vue 3 前端   │ ──► │  Spring Boot 后端 │ ──► │  Python AI 服务   │
│  (不变)       │     │  (只改 base-url)  │     │  FastAPI :8000    │
└──────────────┘     └──────┬───────────┘     └────────┬─────────┘
                            │                          │
                            ▼                          ▼
                     ┌────────────┐           ┌──────────────┐
                     │ MySQL (原)  │           │ MySQL (新)    │
                     │ iras       │           │ iras_v2      │
                     └────────────┘           └──────┬───────┘
                                                     │ 读取 JD
                                                     ▼
                                            ┌──────────────┐
                                            │ ChromaDB     │
                                            │ (向量库)      │
                                            └──────────────┘
```

## 3. API 层 —— Dify 接口兼容

Spring Boot 改动：`application.yml` 中 `dify.base-url` 从 `https://api.dify.ai/v1` 改为 `http://localhost:8000/v1`。API Key 改为服务自定义 token。

### 3.1 POST /v1/workflows/run

请求格式（与 Dify 完全一致）：

```json
{
  "inputs": { "job_name": "Java开发工程师" },
  "response_mode": "streaming",
  "user": "iras-user"
}
```

响应：SSE 流式，兼容 Dify 事件格式：

```
data: {"event":"workflow_started","data":{"id":"xxx"}}

data: {"event":"text_chunk","data":{"text":"{"}}

data: {"event":"text_chunk","data":{"text":"\"硬技能\""}}

...

data: {"event":"workflow_finished","data":{"outputs":{"result":"{\"硬技能\": ...}"}}}

data: [DONE]
```

职能画像输入 `job_name`，简历诊断输入 `resume_text`（文件引用数组格式，见下文）。

### 3.2 POST /v1/files/upload

请求：multipart/form-data，字段 `file` + `user`

响应：

```json
{
  "id": "file-uuid-xxxxx"
}
```

文件上传后存储到本地临时目录，返回文件 ID 供后续 workflow 引用。简历诊断 workflow 的 `resume_text` 参数格式保持与 Dify 一致：

```json
{
  "inputs": {
    "resume_text": [
      { "transfer_method": "local_file", "upload_file_id": "xxx", "type": "document" }
    ]
  },
  "response_mode": "streaming",
  "user": "iras-user"
}
```

### 3.3 认证

验证 `Authorization: Bearer <token>`，token 在 `config.py` 中配置。

## 4. AI 工作流设计

### 4.1 职能画像

```
用户输入职位名称
    │
    ▼
ChromaDB 向量检索 → 获取 top-5 相关 JD 作为参考
    │
    ▼
LLM 提示词模板（含参考 JD）→ 生成结构化画像
    │
    ▼
SSE 流式输出 → {"硬技能": [...], "软技能": [...], "工具": [...], "经验": "...", "学历": "..."}
```

### 4.2 简历诊断

```
上传简历文件 → 解析文本（PyPDF2 / python-docx / txt）
    │
    ▼
ChromaDB 向量检索 → top-5 匹配岗位
    │
    ▼
LLM 提示词模板（含简历全文 + 岗位 JD）→ 逐岗位评估
    │
    ▼
SSE 流式输出 → [
    {
      "matched_job": "...",
      "matched_score": 85,
      "matched_reason": "...",
      "gap_points": ["..."],
      "interview_advice": ["..."],
      "type": "全职",
      "company": "...",
      "city": "...",
      "salary": "..."
    }
  ]
```

LLM 选择：通义千问 qwen-plus（性价比），长文本场景可切换 qwen-max。

## 5. 数据库设计

新建数据库 `iras_v2`，表结构与原 `iras` 一致，仅 `job_info` 表新增两个字段：

```sql
CREATE DATABASE IF NOT EXISTS `iras_v2`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- job_info 表新增字段（在现有结构基础上）
ALTER TABLE `job_info` ADD COLUMN `source` varchar(50) DEFAULT NULL
  COMMENT '数据来源: 24365/manual/university/gov';

ALTER TABLE `job_info` ADD COLUMN `source_url` varchar(500) DEFAULT NULL
  COMMENT '原始链接';

ALTER TABLE `job_info` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'active'
  COMMENT '状态: active-有效, expired-已下架';
```

其他表（`user`, `diagnosis_record`）结构不变。

## 6. 知识库设计

### 6.1 向量化流程

```
MySQL job_info (status = 'active')
    │ 读取 jd_text
    ▼
文本分块（chunk_size=500, overlap=50）
    │
    ▼
嵌入模型: text-embedding-v3（通义千问）
    │
    ▼
存入 ChromaDB
```

### 6.2 增量更新

每次数据聚合器写入新职位后，触发增量同步：

1. 查 MySQL `update_time > last_sync_time` 的职位
2. 新 active → 向量化并插入
3. 新 expired → 从 ChromaDB 删除对应向量

ChromaDB 持久化存储在本地 `./chroma_data/` 目录。

## 7. 数据聚合器设计

### 7.1 数据源

第一版只接入 **24365 国家大学生就业服务平台**（国家级、稳定、无反爬）。

### 7.2 采集参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 每次页数 | 3-5 页 | 约 60-100 条原始数据 |
| 关键词轮换 | 计算机/开发/前端/后端/AI | 每次一个方向 |
| 单次新增上限 | 50 条 | 去重后入库上限 |
| 执行频率 | 每 3 天一次 | 凌晨 3:00 |
| 请求间隔 | 2-3 秒 | 避免触发频率限制 |

### 7.3 数据管道

```
24365 列表页 → 提取标题/公司/城市/链接
    │
    ▼
逐条进入详情页 → 提取完整 JD 描述
    │
    ▼
清洗标准化 → 统一字段名，去除 HTML 标签，规范化薪资格式
    │
    ▼
去重 (job_name + company_name) → 与 MySQL 现有数据比对
    │
    ▼
写入 MySQL（Upsert）：
  - 新出现 → INSERT, status = 'active'
  - 已存在 → UPDATE 覆盖字段, status = 'active'
  - 本次未采集到的旧数据 → UPDATE status = 'expired'
    │
    ▼
触发 ChromaDB 增量同步
```

### 7.4 调度

使用 APScheduler：

```python
from apscheduler.schedulers.background import BackgroundScheduler

scheduler = BackgroundScheduler()
scheduler.add_job(
    run_pipeline,
    trigger="cron",
    hour=3,
    minute=0,
    day="*/3"  # 每 3 天
)
scheduler.start()
```

### 7.5 扩展性

新增数据源只需：
1. 继承 `BaseCollector` 实现 `fetch()` 和 `parse_item()`
2. 在 `engine.py` 注册
3. 配置中启用

## 8. 采集器接口规范

```python
from abc import ABC, abstractmethod

class BaseCollector(ABC):
    name: str                          # 采集器名称
    list_url: str                      # 列表页 URL
    page_size: int = 20                # 每页条数
    max_pages: int = 5                 # 最大采集页数
    request_interval: float = 2.0      # 请求间隔（秒）

    @abstractmethod
    def build_params(self, page: int, keyword: str) -> dict:
        """构建请求参数"""
        ...

    @abstractmethod
    def fetch_list(self, params: dict) -> list[dict]:
        """获取列表页，返回摘要列表"""
        ...

    @abstractmethod
    def fetch_detail(self, item_id: str) -> dict:
        """获取详情页，返回完整 JD"""
        ...

    @abstractmethod
    def normalize(self, raw: dict) -> dict:
        """清洗标准化为 job_info 表字段"""
        ...
```

## 9. 配置项（config.py）

```python
# LLM
LLM_MODEL = "qwen-plus"
LLM_API_KEY = "sk-xxx"
LLM_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
EMBEDDING_MODEL = "text-embedding-v3"

# API 认证
SERVICE_TOKEN = "iras-ai-service-token-2024"

# 数据库
MYSQL_HOST = "localhost"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASSWORD = "xxx"
MYSQL_DB = "iras_v2"

# 向量库
CHROMA_PERSIST_DIR = "./chroma_data"
CHUNK_SIZE = 500
CHUNK_OVERLAP = 50

# 聚合器
AGGREGATOR_MAX_PAGES = 5
AGGREGATOR_INSERT_LIMIT = 50
AGGREGATOR_CRON = "0 3 */3 * *"  # 每 3 天凌晨 3 点
AGGREGATOR_KEYWORDS = ["计算机", "开发", "前端", "后端", "人工智能"]
```

## 10. 与原项目的集成变更点

**唯一改动**：`iras-project-main/backend/src/main/resources/application.yml`

```yaml
# 改前
dify:
  base-url: https://api.dify.ai/v1
  job-profile-key: app-xxx
  resume-diagnosis-key: app-xxx

# 改后
dify:
  base-url: http://localhost:8000/v1
  job-profile-key: iras-ai-service-token-2024
  resume-diagnosis-key: iras-ai-service-token-2024
```

前端、Controller、Service、Mapper 等全部不动。

## 11. 技术栈

| 组件 | 技术 | 用途 |
|------|------|------|
| Web 框架 | FastAPI | HTTP API + SSE 流式响应 |
| LLM | 通义千问 qwen-plus | 职能画像、简历诊断 |
| LLM 编排 | LangChain | Prompt 模板、Chain 编排 |
| 嵌入模型 | text-embedding-v3 | 文本向量化 |
| 向量数据库 | ChromaDB | 岗位 JD 语义检索 |
| 调度器 | APScheduler | 定时数据采集 |
| HTTP 客户端 | httpx / aiohttp | 数据采集请求 |
| 文档解析 | PyPDF2, python-docx | 简历文件解析 |

## 12. 非功能需求

- **稳定性**：AI 工作流调用 LLM 失败时，返回 SSE error 事件而非崩溃
- **性能**：ChromaDB 向量检索 < 200ms（500 条职位规模）
- **可观测性**：采集日志记录每次运行的时间、新增数、错误信息

## 13. 待定项

- 通义千问 API Key 的具体申请方式（答辩前准备好）
- 24365 网站如需登录才能看详情，降级为仅用列表页摘要
- 后续可扩展的高校就业网采集器列表
