"""スレッド管理API。

チャット履歴のスレッド一覧取得、詳細取得、メッセージ取得を提供します。
"""

import logging

import duckdb
from fastapi import APIRouter, Depends, HTTPException, Query

from backend.api.deps import get_chat_db, verify_api_key
from backend.models.thread import (
    Thread,
    ThreadListResponse,
    ThreadMessagesResponse,
)
from backend.services.thread_service import ThreadService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/threads", tags=["threads"])

# MVP: ユーザーIDは固定値
DEFAULT_USER_ID = "default_user"


@router.get("", response_model=ThreadListResponse)
async def get_threads(
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    chat_db: duckdb.DuckDBPyConnection = Depends(get_chat_db),
    _: None = Depends(verify_api_key),
):
    """スレッド一覧を取得します。

    最終メッセージ日時の降順で取得します。

    Args:
        limit: 1ページあたりの件数（デフォルト: 20）
        offset: オフセット（デフォルト: 0）

    Returns:
        ThreadListResponse

    Example:
        GET /v1/threads?limit=20&offset=0
    """
    logger.info(
        "Retrieving threads for user_id=%s, limit=%s, offset=%s",
        DEFAULT_USER_ID,
        limit,
        offset,
    )

    thread_service = ThreadService(chat_db)
    threads, total = thread_service.get_threads(
        user_id=DEFAULT_USER_ID, limit=limit, offset=offset
    )

    return ThreadListResponse(
        threads=threads,
        total=total,
        limit=limit,
        offset=offset,
    )


@router.get("/{thread_id}", response_model=Thread)
async def get_thread(
    thread_id: str,
    chat_db: duckdb.DuckDBPyConnection = Depends(get_chat_db),
    _: None = Depends(verify_api_key),
):
    """スレッドの詳細を取得します。

    Args:
        thread_id: スレッドのUUID

    Returns:
        Thread

    Raises:
        HTTPException: スレッドが存在しない場合（404）

    Example:
        GET /v1/threads/{thread_id}
    """
    logger.info("Retrieving thread: thread_id=%s", thread_id)

    thread_service = ThreadService(chat_db)
    thread = thread_service.get_thread(thread_id)

    if thread is None:
        raise HTTPException(
            status_code=404,
            detail=f"Thread not found: {thread_id}",
        )

    return thread


@router.get("/{thread_id}/messages", response_model=ThreadMessagesResponse)
async def get_thread_messages(
    thread_id: str,
    chat_db: duckdb.DuckDBPyConnection = Depends(get_chat_db),
    _: None = Depends(verify_api_key),
):
    """スレッドのメッセージ一覧を取得します。

    作成日時の昇順で取得します。

    Args:
        thread_id: スレッドのUUID

    Returns:
        ThreadMessagesResponse

    Raises:
        HTTPException: スレッドが存在しない場合（404）

    Example:
        GET /v1/threads/{thread_id}/messages
    """
    logger.info("Retrieving messages for thread_id=%s", thread_id)

    thread_service = ThreadService(chat_db)

    # スレッドの存在確認
    thread = thread_service.get_thread(thread_id)
    if thread is None:
        raise HTTPException(
            status_code=404,
            detail=f"Thread not found: {thread_id}",
        )

    # メッセージ取得
    messages = thread_service.get_messages(thread_id)

    return ThreadMessagesResponse(
        thread_id=thread_id,
        messages=messages,
    )
