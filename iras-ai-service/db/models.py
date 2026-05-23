"""
IRAS AI Service — 数据库 ORM 模型模块。

使用 SQLAlchemy 声明式映射定义 iras_v2 库中的三张核心表：

1. user：用户表（与 Spring Boot 后端共享）
2. job_info：岗位信息表（IRAS 核心数据，含 source/status 扩展字段）
3. diagnosis_record：简历诊断记录表（存储每次诊断的原始结果）

注意：本模块只定义 AI 服务需要访问的表，其他表（如 interview_record）
由 Spring Boot 后端管理，此处不重复定义。

新增字段说明（相对于原 iras 库）：
- job_info.source: 数据来源（csv/manual/seed）
- job_info.source_url: 原始数据链接（预留）
- job_info.status: 数据状态（active/expired），用于增量同步时的软删除和过期判断
"""

from datetime import datetime
from sqlalchemy import Column, BigInteger, String, Text, Integer, DateTime, ForeignKey
from sqlalchemy.orm import declarative_base

# 声明式基类，所有 ORM 模型继承自此
Base = declarative_base()


class User(Base):
    """
    用户表，映射 iras_v2.user。

    与 Spring Boot 后端的 User 实体对应，AI 服务仅读取用户信息，
    不执行写入操作（用户注册/登录由 Spring Boot 处理）。
    """
    __tablename__ = "user"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    username = Column(String(50), unique=True, nullable=False)
    password = Column(String(255), nullable=False)  # BCrypt 加密存储
    email = Column(String(100), default=None)
    role = Column(String(20), default="user")  # user / admin
    create_time = Column(DateTime, default=datetime.now)


class JobInfo(Base):
    """
    岗位信息表，映射 iras_v2.job_info。

    IRAS 系统的核心数据表，存储所有可检索和匹配的职位信息。
    每条记录的 jd_text 经向量化后存入 ChromaDB，供语义检索使用。

    字段说明：
        source:  数据来源 — "seed"（初始化种子）/"csv"（CSV导入）/"manual"（管理后台手动添加）
        status:  数据状态 — "active"（有效，参与检索和同步）/ "expired"（失效，从向量库中移除）
        type:    岗位类型 — "全职"/"实习"/"兼职" 等
    """
    __tablename__ = "job_info"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    job_name = Column(String(100), nullable=False)       # 职位名称
    company_name = Column(String(100), nullable=False)   # 公司名称
    city = Column(String(50), default=None)               # 工作城市
    salary = Column(String(50), default=None)              # 薪资范围（字符串格式，如 "15000-20000"）
    jd_text = Column(Text, nullable=False)                 # 岗位描述全文（用于向量化和检索）
    type = Column(String(50), default=None)                # 岗位类型
    create_time = Column(DateTime, default=datetime.now)   # 创建时间
    update_time = Column(DateTime, default=datetime.now, onupdate=datetime.now)  # 更新时间
    source = Column(String(50), default=None)              # 数据来源
    source_url = Column(String(500), default=None)          # 原始数据 URL
    status = Column(String(20), default="active")           # 数据状态


class DiagnosisRecord(Base):
    """
    诊断记录表，映射 iras_v2.diagnosis_record。

    存储每次简历诊断的完整结果，包括：
    - 原始简历文件信息（文件名 + 文本内容）
    - AI 诊断结果 JSON（含匹配岗位、评分、差距分析、面试建议）
    - AI 推理过程（<think> 标签内内容）

    TEXT(length=16777215) 使用 MEDIUMTEXT 类型，最大 16MB，
    确保诊断结果 JSON 不会因字段长度限制被截断。
    """
    __tablename__ = "diagnosis_record"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, ForeignKey("user.id", ondelete="CASCADE"), nullable=False)
    resume_filename = Column(String(255), nullable=False)               # 上传的简历文件名
    resume_content = Column(Text(length=16777215), default=None)        # 简历解析后的纯文本（MEDIUMTEXT）
    diagnosis_result = Column(Text(length=16777215), default=None)      # 诊断结果 JSON（MEDIUMTEXT）
    think_content = Column(Text(length=16777215), default=None)         # AI 推理过程（MEDIUMTEXT）
    match_count = Column(Integer, default=0)                             # 匹配到的岗位数量
    create_time = Column(DateTime, default=datetime.now)                # 诊断时间
