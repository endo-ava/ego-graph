"""Conversational chat endpoint with LLM.

LLMとの会話を通じてデータを分析・取得できるエンドポイントです。
LLMが必要に応じてツールを呼び出し、データにアクセスします。
"""

import asyncio
import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from backend.api.deps import get_config, get_db_connection, verify_api_key
from backend.config import BackendConfig
from backend.database.connection import DuckDBConnection
from backend.database.queries import get_parquet_path
from backend.llm import LLMClient, Message
from backend.tools import GetListeningStatsTool, GetTopTracksTool, ToolRegistry

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/chat", tags=["chat"])


class ChatRequest(BaseModel):
    """チャットリクエスト。"""

    messages: list[Message]
    stream: bool = False  # 将来のストリーミング対応用


class ChatResponseModel(BaseModel):
    """チャットレスポンス。"""

    id: str
    message: Message
    tool_calls: Optional[list[dict]] = None
    usage: Optional[dict] = None


@router.post("", response_model=ChatResponseModel)
async def chat(
    request: ChatRequest,
    db_connection: DuckDBConnection = Depends(get_db_connection),
    config: BackendConfig = Depends(get_config),
    _: None = Depends(verify_api_key),
):
    """LLMエージェント向けチャットエンドポイント。

    ユーザーのメッセージを受け取り、LLMがツールを使用して
    データにアクセスしながら応答を生成します。

    Args:
        request: チャットリクエスト

    Returns:
        ChatResponseModel

    Raises:
        HTTPException: LLM設定が不足している場合（501）
        HTTPException: LLM APIエラー（502）

    Example:
        POST /v1/chat
        {
            "messages": [
                {"role": "user", "content": "先月の再生回数トップ5は？"}
            ]
        }
    """
    # LLM設定の確認
    if not config.llm:
        raise HTTPException(
            status_code=501,
            detail="LLM configuration is missing. Chat endpoint is unavailable.",
        )

    logger.info(f"Received chat request with {len(request.messages)} messages")

    try:
        # LLMクライアントの初期化
        llm = LLMClient(
            provider_name=config.llm.provider,
            api_key=config.llm.api_key.get_secret_value(),
            model_name=config.llm.model_name,
        )

        # ツールレジストリの準備
        parquet_path = get_parquet_path(config.r2.bucket_name, config.r2.events_path)
        tool_registry = ToolRegistry()
        tool_registry.register(GetTopTracksTool(db_connection, parquet_path))
        tool_registry.register(GetListeningStatsTool(db_connection, parquet_path))

        tools = tool_registry.get_all_schemas()

        # LLMリクエスト送信（タイムアウト設定: 30秒）
        try:
            response = await asyncio.wait_for(
                llm.chat(
                    messages=request.messages,
                    tools=tools,
                    temperature=config.llm.temperature,
                    max_tokens=config.llm.max_tokens,
                ),
                timeout=30.0,
            )
        except asyncio.TimeoutError:
            logger.error("LLM request timed out after 30 seconds")
            raise HTTPException(
                status_code=504, detail="LLM request timed out"
            ) from None

        # TODO: ツール呼び出しがあれば実行して再度LLMに渡す
        # 現在はシンプルに1回の応答のみ返す（MVPでは十分）

        return ChatResponseModel(
            id=response.id,
            message=response.message,
            tool_calls=[tc.model_dump() for tc in response.tool_calls]
            if response.tool_calls
            else None,
            usage=response.usage,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Chat request failed")
        raise HTTPException(status_code=502, detail=f"LLM API error: {str(e)}") from e
