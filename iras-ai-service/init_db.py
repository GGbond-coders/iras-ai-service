"""
IRAS AI Service — 数据库初始化脚本。

独立脚本，用于首次部署时创建 iras_v2 数据库、建表并插入种子数据。

用法：
    python init_db.py

流程：
1. 连接 MySQL（不指定库），创建 iras_v2 数据库（如不存在）
2. 使用 SQLAlchemy ORM 创建全部表（Base.metadata.create_all）
3. 检查 job_info 表是否为空，为空则插入 8 条种子数据
4. 如果已存在数据则跳过种子插入（避免重复初始化）
"""

from sqlalchemy import text
from db.database import engine, SessionLocal
from db.models import Base, JobInfo


def init():
    """
    初始化数据库：建库 → 建表 → 种子数据。

    幂等性：数据库和表使用 IF NOT EXISTS / create_all，
    种子数据仅在 job_info 表为空时插入。
    """
    # Step 1: 创建数据库（如不存在）
    import pymysql
    from config import MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD

    conn = pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASSWORD,
    )
    conn.cursor().execute(
        "CREATE DATABASE IF NOT EXISTS `iras_v2` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )
    conn.close()

    # Step 2: 创建全部表（ORM 映射的 user / job_info / diagnosis_record）
    Base.metadata.create_all(bind=engine)
    print("Tables created successfully.")

    # Step 3: 插入种子岗位数据
    db = SessionLocal()
    try:
        existing = db.query(JobInfo).count()
        if existing > 0:
            print(f"Database already has {existing} jobs, skipping seed.")
            return

        # 8 条种子数据，覆盖主流技术岗位，确保系统初始化后即可演示全流程
        seeds = [
            JobInfo(job_name="Java后台开发工程师", company_name="上海联泉智能科技有限公司",
                    city="上海", salary="15000",
                    jd_text="5年及以上 本科 Java 后台开发 java 软件开发 软件工程师 开发 java开发 开发工程师 软件 工程师 五险一金 年终奖金 员工旅游 绩效奖金 餐饮补贴 交通补贴 通讯补贴",
                    type="全职", source="seed", status="active"),
            JobInfo(job_name="前端开发工程师", company_name="北京字节跳动科技有限公司",
                    city="北京", salary="20000",
                    jd_text="3年及以上 本科 前端开发 JavaScript TypeScript Vue React HTML CSS Web开发 五险一金 弹性工作 免费三餐",
                    type="全职", source="seed", status="active"),
            JobInfo(job_name="Python测试开发工程师", company_name="深圳腾讯计算机系统有限公司",
                    city="深圳", salary="18000",
                    jd_text="3年及以上 本科 Python 测试开发 自动化测试 性能测试 Selenium pytest 接口测试 五险一金 股票期权",
                    type="全职", source="seed", status="active"),
            JobInfo(job_name="Android开发工程师", company_name="杭州阿里巴巴网络科技有限公司",
                    city="杭州", salary="22000",
                    jd_text="3年及以上 本科 Android Java Kotlin 移动开发 MVVM Jetpack 五险一金 补充医疗 带薪年假",
                    type="全职", source="seed", status="active"),
            JobInfo(job_name="iOS开发工程师", company_name="北京小米科技有限公司",
                    city="北京", salary="20000",
                    jd_text="3年及以上 本科 iOS Swift Objective-C 移动开发 UIKit SwiftUI 五险一金 年终奖金",
                    type="全职", source="seed", status="active"),
            JobInfo(job_name="后端开发工程师", company_name="上海拼多多信息技术有限公司",
                    city="上海", salary="25000",
                    jd_text="5年及以上 本科 Java Go 微服务 分布式 高并发 Spring Cloud Kubernetes 五险一金 股票期权",
                    type="全职", source="seed", status="active"),
            JobInfo(job_name="数据分析师", company_name="广州网易计算机系统有限公司",
                    city="广州", salary="15000",
                    jd_text="2年及以上 本科 SQL Python 数据分析 Tableau 数据可视化 统计学 五险一金 年终奖金",
                    type="全职", source="seed", status="active"),
            JobInfo(job_name="算法工程师", company_name="北京百度网讯科技有限公司",
                    city="北京", salary="30000",
                    jd_text="3年及以上 硕士 机器学习 深度学习 NLP CV Python TensorFlow PyTorch 五险一金 股票期权",
                    type="全职", source="seed", status="active"),
        ]
        db.add_all(seeds)
        db.commit()
        print(f"Inserted {len(seeds)} seed jobs.")
    finally:
        db.close()


if __name__ == "__main__":
    init()
