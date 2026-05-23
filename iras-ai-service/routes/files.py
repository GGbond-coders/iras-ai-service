"""
IRAS AI Service — 文件上传路由模块。

实现 Dify 兼容的文件上传接口，用于接收用户上传的简历文件。

接口：
- POST /v1/files/upload：上传简历文件（multipart/form-data）

认证方式：
- 通过 Authorization Header 传递 Bearer Token
- 使用 Header(default="") 而非 Header(...)，因为 multipart 请求中
  Header 参数解析可能延迟，default="" 确保容错
"""

import os
import logging
from uuid import uuid4
from fastapi import APIRouter, UploadFile, File, Form, Header, HTTPException
from models.schemas import FileUploadResponse
from config import UPLOAD_DIR, SERVICE_TOKEN

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/files/upload")
async def upload_file(
    file: UploadFile = File(...),           # 上传的文件二进制
    user: str = Form("iras-user"),           # 用户标识（Dify 兼容字段）
    authorization: str = Header(default=""), # Bearer Token（手动校验）
):
    """
    上传简历文件。

    接收 multipart/form-data 请求，将文件保存到 UPLOAD_DIR，
    返回文件 ID 供后续 workflow 调用使用。

    参数：
        file: 上传的文件（PDF/DOCX/TXT）
        user: 用户标识，默认 "iras-user"
        authorization: Authorization 请求头

    返回：
        {"id": "uuid-xxxxx"} — Dify 兼容格式，id 用于 workflow 的 resume_text 参数

    异常：
        HTTPException(401): token 缺失或无效
    """
    # 手动校验 Bearer Token
    logger.info(f"File upload request: filename='{file.filename}', user='{user}'")
    if not authorization.startswith("Bearer ") or authorization.removeprefix("Bearer ") != SERVICE_TOKEN:
        logger.warning("File upload: token verification failed")
        raise HTTPException(status_code=401, detail="Missing or invalid token")

    # 确保上传目录存在
    os.makedirs(UPLOAD_DIR, exist_ok=True)

    # 生成唯一文件名：{uuid}{原始扩展名}
    file_id = str(uuid4())
    ext = os.path.splitext(file.filename or "resume")[1]
    save_path = os.path.join(UPLOAD_DIR, f"{file_id}{ext}")

    # 写入文件
    content = await file.read()
    with open(save_path, "wb") as f:
        f.write(content)

    logger.info(f"File saved: file_id='{file_id}', path='{save_path}', size={len(content)} bytes")
    return FileUploadResponse(id=file_id)
