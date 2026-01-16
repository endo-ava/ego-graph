"""Conversational chat endpoint with LLM.

LLMとの会話を通じてデータを分析・取得できるエンドポイントです。
LLMが必要に応じてツールを呼び出し、データにアクセスします。
"""

import asyncio
import logging

import duckdb
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from backend.api.schemas import DEFAULT_MODEL, get_all_models, get_model
from backend.config import BackendConfig
from backend.dependencies import get_chat_db, get_config, verify_api_key
from backend.infrastructure.llm import Message
from backend.infrastructure.repositories import DuckDBThreadRepository
from backend.usecases.chat import (
    ChatRequest as UseCaseChatRequest,
)
from backend.usecases.chat import (
    ChatUseCase,
    MaxIterationsExceeded,
    NoUserMessageError,
    ThreadNotFoundError,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/chat", tags=["chat"])

# MVP: ユーザーIDは固定値
DEFAULT_USER_ID = "default_user"


class ChatRequest(BaseModel):
    """チャットリクエスト。"""

    messages: list[Message]
    stream: bool = False  # 将来のストリーミング対応用
    thread_id: str | None = None  # 既存スレッドの場合はUUID、新規の場合はNone
    model_name: str | None = None  # 追加: モデル名


class ChatResponseModel(BaseModel):
    """チャットレスポンス。"""

    id: str
    message: Message
    tool_calls: list[dict] | None = None
    usage: dict | None = None
    thread_id: str  # スレッドのUUID（新規作成時も含む）
    model_name: str | None = None  # 追加: 使用したモデル名


@router.get("/models")
async def get_models_endpoint(_: None = Depends(verify_api_key)):
    """利用可能なモデル一覧を取得する。

    Returns:
        モデル情報のリストを含む辞書
    """

    return {
        "models": get_all_models(),
        "default_model": DEFAULT_MODEL,
    }


@router.post("", response_model=ChatResponseModel)
async def chat(
    request: ChatRequest,
    chat_db: duckdb.DuckDBPyConnection = Depends(get_chat_db),
    config: BackendConfig = Depends(get_config),
    _: None = Depends(verify_api_key),
):
    """LLMエージェント向けチャットエンドポイント。

    ユーザーのメッセージを受け取り、LLMがツールを使用して
    データにアクセスしながら応答を生成します。

    Args:
        request: チャットリクエスト
        chat_db: チャットDB接続
        config: バックエンド設定
        _: API Key検証結果(未使用)

    Returns:
        ChatResponseModel: チャット応答

    Raises:
        HTTPException: LLM設定が不足している場合(501)
        HTTPException: モデル名が無効な場合(400)
        HTTPException: スレッドが見つからない場合(404)
        HTTPException: 最大イテレーション到達(500)
        HTTPException: タイムアウト(504)
        HTTPException: LLM APIエラー(502)

    Example:
        POST /v1/chat
        {
            "messages": [
                {"role": "user", "content": "先月の再生回数トップ5は？"}
            ]
        }
    """
    # 1. LLM設定検証
    if not config.llm:
        raise HTTPException(
            status_code=501,
            detail="LLM configuration is missing. Chat endpoint is unavailable.",
        )

    logger.info("Received chat request with %s messages", len(request.messages))

    # 2. モデル名検証
    model_name = request.model_name or config.llm.model_name
    try:
        get_model(model_name)
    except ValueError as e:
        logger.exception("Invalid model name: %s", model_name)
        raise HTTPException(status_code=400, detail=str(e)) from e

    # 3. UseCase実行
    thread_repository = DuckDBThreadRepository(chat_db)
    use_case = ChatUseCase(thread_repository, config.llm, config.r2)

    try:
        result = await use_case.execute(
            UseCaseChatRequest(
                messages=request.messages,
                thread_id=request.thread_id,
                model_name=model_name,
                user_id=DEFAULT_USER_ID,
            )
        )
        return ChatResponseModel(
            id=result.response_id,
            message=result.message,
            tool_calls=None,
            usage=result.usage,
            thread_id=result.thread_id,
            model_name=result.model_name,
        )
    except NoUserMessageError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except ThreadNotFoundError as e:
        raise HTTPException(status_code=404, detail=str(e)) from e
    except MaxIterationsExceeded as e:
        raise HTTPException(status_code=500, detail=str(e)) from e
    except asyncio.TimeoutError:
        logger.exception("Request timed out")
        raise HTTPException(status_code=504, detail="Request timed out") from None
    except Exception as e:
        logger.exception("Chat request failed")
        raise HTTPException(status_code=502, detail=f"LLM API error: {str(e)}") from e
