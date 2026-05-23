"""
IRAS AI Service — 数据库连接模块。

使用 SQLAlchemy 管理 iras_v2 MySQL 数据库的连接池和会话。

组件：
- engine: 全局数据库引擎，配置连接池预热和自动回收
- SessionLocal: 线程安全的 Session 工厂
- get_db(): FastAPI 依赖注入用 Session 生成器

连接池参数说明：
- pool_pre_ping=True: 每次从池中取出连接时先 ping 一下，确保连接未被 MySQL 超时断开
- pool_recycle=3600: 每小时回收连接，防止 MySQL 默认 8 小时 wait_timeout 导致 stale connection
"""

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from config import MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DB

# MySQL 连接 URL（pymysql 驱动 + utf8mb4 编码）
DATABASE_URL = f"mysql+pymysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}?charset=utf8mb4"

# 全局数据库引擎（单例，整个应用共享一个连接池）
engine = create_engine(DATABASE_URL, pool_pre_ping=True, pool_recycle=3600)

# Session 工厂，每次调用生成一个新的数据库会话
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db() -> Session:
    """
    FastAPI 依赖注入生成器。

    用法：
        @app.get("/")
        def endpoint(db: Session = Depends(get_db)):
            ...

    每次请求创建一个新 Session，请求结束后自动关闭（无论成功或异常）。

    返回：
        SQLAlchemy Session 实例（yield 后自动 close）
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
