"""
IRAS AI Service — CSV 岗位数据导入脚本。

独立脚本，用于将 CSV 格式的岗位数据批量导入 iras_v2 数据库。

用法：
    python import_jobs.py              # 追加模式（跳过已存在的岗位）
    python import_jobs.py --replace    # 替换模式（删除旧 CSV 数据后重新导入）

CSV 格式要求：
    必须包含列：job_name, company_name, city, salary, jd_text
    编码：UTF-8（支持 BOM 头，使用 utf-8-sig 自动处理）

去重策略：
    以 (job_name, company_name) 组合为唯一键，追加模式下跳过已存在的组合。
    替换模式下先删除所有 source='csv' 的记录再导入。

批次写入：
    每 200 条提交一次事务，平衡内存占用和数据库 I/O 性能。

薪资处理：
    CSV 中 salary 可能为浮点数格式（如 "15000.0"），转换为整数后存储为字符串。
"""

import csv
import sys
import logging
from db.database import engine, SessionLocal
from db.models import Base, JobInfo
from sqlalchemy import delete

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

# CSV 文件路径（相对于 iras-ai-service 目录，文件位于项目根目录）
CSV_PATH = "../sampled_3000_jobs.csv"


def import_jobs(replace: bool = False):
    """
    从 CSV 文件导入岗位数据到 iras_v2 数据库。

    流程：
    1. 检查 CSV 文件是否存在
    2. 替换模式：删除所有 source='csv' 的旧记录
    3. 构建内存中的去重索引（(job_name, company_name) → 已存在）
    4. 逐行读取 CSV，跳过重复，构建 JobInfo 对象
    5. 每 200 条批量提交一次

    参数：
        replace: True=先删除旧数据再导入，False=跳过重复数据追加导入
    """
    # 检查 CSV 文件存在性
    if not __import__("os").path.exists(CSV_PATH):
        logger.error(f"CSV file not found: {CSV_PATH}")
        sys.exit(1)

    db = SessionLocal()
    try:
        # 替换模式：清理旧 CSV 导入数据
        if replace:
            count = db.query(JobInfo).filter(JobInfo.source == "csv").delete()
            logger.info(f"Deleted {count} existing CSV-imported jobs")
            db.commit()

        # 构建去重索引（以 job_name + company_name 为唯一键）
        existing = set()
        for row in db.query(JobInfo.job_name, JobInfo.company_name).all():
            existing.add((row.job_name.strip(), row.company_name.strip()))

        inserted = 0
        skipped = 0
        batch = []
        batch_size = 200  # 每批 200 条提交一次

        # 使用 utf-8-sig 编码自动跳过 BOM 头
        with open(CSV_PATH, encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            for row in reader:
                key = (row["job_name"].strip(), row["company_name"].strip())
                if key in existing:
                    skipped += 1
                    continue
                existing.add(key)  # 加入索引防止同批次内重复

                # 薪资处理：浮点数转整数再转字符串（"15000.0" → "15000"）
                salary = row.get("salary", "").strip()
                if salary:
                    try:
                        salary = str(int(float(salary)))
                    except ValueError:
                        pass  # 非数字薪资保持原样

                job = JobInfo(
                    job_name=row["job_name"].strip(),
                    company_name=row["company_name"].strip(),
                    city=row.get("city", "").strip(),
                    salary=salary,
                    jd_text=row.get("jd_text", "").strip(),
                    type="全职",
                    source="csv",        # 标记数据来源，便于后续区分和清理
                    status="active",     # 导入即有效，参与检索和同步
                )
                batch.append(job)
                inserted += 1

                # 批次写满时提交
                if len(batch) >= batch_size:
                    db.add_all(batch)
                    db.commit()
                    logger.info(f"Inserted {inserted} / skipped {skipped}")
                    batch = []

            # 提交剩余不足一批的数据
            if batch:
                db.add_all(batch)
                db.commit()

        logger.info(f"Done: {inserted} inserted, {skipped} skipped (duplicates)")
    finally:
        db.close()


if __name__ == "__main__":
    replace = "--replace" in sys.argv
    import_jobs(replace=replace)
