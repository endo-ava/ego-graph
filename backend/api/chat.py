"""Conversational chat endpoint with LLM.

LLMとの会話を通じてデータを分析・取得できるエンドポイントです。
LLMが必要に応じてツールを呼び出し、データにアクセスします。
"""

import asyncio
import json
import logging
from datetime import datetime
from typing import Any, Optional
from zoneinfo import ZoneInfo

import duckdb
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from backend.api.deps import get_chat_db, get_config, get_db_connection, verify_api_key
from backend.config import BackendConfig
from backend.database.connection import DuckDBConnection
from backend.llm import LLMClient, Message, ToolCall
from backend.services.thread_service import ThreadService
from backend.tools import GetListeningStatsTool, GetTopTracksTool, ToolRegistry

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/chat", tags=["chat"])

# 定数
MAX_ITERATIONS = 5
TOTAL_TIMEOUT = 30.0
JST = ZoneInfo("Asia/Tokyo")

# スレッド作成の競合状態を防ぐためのロック
_thread_creation_lock = asyncio.Lock()


class ChatRequest(BaseModel):
    """チャットリクエスト。"""

    messages: list[Message]
    stream: bool = False  # 将来のストリーミング対応用
    thread_id: Optional[str] = None  # 既存スレッドの場合はUUID、新規の場合はNone


class ChatResponseModel(BaseModel):
    """チャットレスポンス。"""

    id: str
    message: Message
    tool_calls: Optional[list[dict]] = None
    usage: Optional[dict] = None
    thread_id: str  # スレッドのUUID（新規作成時も含む）


@router.post("", response_model=ChatResponseModel)
async def chat(
    request: ChatRequest,
    _db_connection: DuckDBConnection = Depends(get_db_connection),
    chat_db: duckdb.DuckDBPyConnection = Depends(get_chat_db),
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

    logger.info("Received chat request with %s messages", len(request.messages))

    # MVP: ユーザーIDは固定値
    user_id = "default_user"

    # スレッドサービスの初期化
    thread_service = ThreadService(chat_db)

    # スレッドID処理（新規 or 既存）
    thread_id: str
    try:
        if request.thread_id is None:
            # 新規スレッド: 初回ユーザーメッセージから作成
            first_user_message = next(
                (msg for msg in request.messages if msg.role == "user"), None
            )
            if first_user_message is None:
                raise HTTPException(
                    status_code=400,
                    detail="At least one user message is required for new thread",
                )

            # 複数リクエストが同時に到達した場合の重複スレッド作成を防ぐ
            async with _thread_creation_lock:
                thread = thread_service.create_thread(
                    user_id, first_user_message.content or ""
                )
                thread_id = thread.thread_id
                logger.info("Created new thread: thread_id=%s", thread_id)

            # 初回ユーザーメッセージを保存
            thread_service.add_message(
                thread_id=thread_id,
                user_id=user_id,
                role="user",
                content=first_user_message.content or "",
            )
        else:
            # 既存スレッド: 存在確認
            thread = thread_service.get_thread(request.thread_id)
            if thread is None:
                raise HTTPException(
                    status_code=404,
                    detail=f"Thread not found: {request.thread_id}",
                )
            thread_id = request.thread_id
            logger.info("Using existing thread: thread_id=%s", thread_id)

            # 最新のユーザーメッセージを保存
            last_user_message = next(
                (msg for msg in reversed(request.messages) if msg.role == "user"), None
            )
            if last_user_message:
                thread_service.add_message(
                    thread_id=thread_id,
                    user_id=user_id,
                    role="user",
                    content=last_user_message.content or "",
                )
    except duckdb.Error as e:
        logger.error(
            "Database error during thread/message handling: %s", type(e).__name__
        )
        raise HTTPException(
            status_code=500, detail=f"Database error: {type(e).__name__}"
        ) from e

    try:
        # LLMクライアントの初期化
        llm = LLMClient(
            provider_name=config.llm.provider,
            api_key=config.llm.api_key.get_secret_value(),
            model_name=config.llm.model_name,
            enable_web_search=config.llm.enable_web_search,
        )

        # ツールレジストリの準備
        tool_registry = ToolRegistry()
        if config.r2:
            tool_registry.register(GetTopTracksTool(config.r2))
            tool_registry.register(GetListeningStatsTool(config.r2))

        tools = tool_registry.get_all_schemas()

        # ツール実行ループ
        conversation_history = request.messages.copy()

        # システムメッセージに現在日を追加（まだ含まれていない場合）
        if not any(msg.role == "system" for msg in conversation_history):
            now = datetime.now(JST)
            current_date = now.strftime("%Y-%m-%d")
            current_time = now.strftime("%H:%M:%S")
            system_message = Message(
                role="system",
                content=f"""あなたはユーザーのデジタルライフログを分析するアシスタントです。

現在の日時情報:
- 今日の日付: {current_date}
- 現在時刻: {current_time} JST (日本標準時)
""",
            )
            conversation_history.insert(0, system_message)
            logger.debug("Added system message with current date: %s", current_date)

        iteration = 0
        loop = asyncio.get_running_loop()
        start_time = loop.time()

        while iteration < MAX_ITERATIONS:
            iteration += 1

            # 残り時間計算
            elapsed = loop.time() - start_time
            remaining_timeout = TOTAL_TIMEOUT - elapsed

            if remaining_timeout <= 0:
                logger.error("Total timeout exceeded after %s seconds", TOTAL_TIMEOUT)
                raise HTTPException(
                    status_code=504,
                    detail=f"Request timed out after {TOTAL_TIMEOUT} seconds",
                )

            # LLMリクエスト送信
            try:
                response = await asyncio.wait_for(
                    llm.chat(
                        messages=conversation_history,
                        tools=tools,
                        temperature=config.llm.temperature,
                        max_tokens=config.llm.max_tokens,
                    ),
                    timeout=remaining_timeout,
                )
            except asyncio.TimeoutError:
                logger.error("LLM request timed out")
                raise HTTPException(
                    status_code=504, detail="LLM request timed out"
                ) from None

            # ツール呼び出しがなければ終了
            if not response.tool_calls:
                logger.info("Chat completed after %s iteration(s)", iteration)

                # アシスタント応答をDB保存
                thread_service.add_message(
                    thread_id=thread_id,
                    user_id=user_id,
                    role="assistant",
                    content=response.message.content or "",
                )

                return ChatResponseModel(
                    id=response.id,
                    message=response.message,
                    tool_calls=None,
                    usage=response.usage,
                    thread_id=thread_id,
                )

            # assistant メッセージを履歴に追加
            conversation_history.append(response.message)
            logger.debug(
                "Iteration %s: LLM requested %s tool call(s)",
                iteration,
                len(response.tool_calls),
            )

            # 残り時間を再計算（ツール実行にもタイムアウトを適用）
            elapsed = loop.time() - start_time
            remaining_timeout = TOTAL_TIMEOUT - elapsed

            if remaining_timeout <= 0:
                logger.error("Total timeout exceeded before tool execution")
                raise HTTPException(
                    status_code=504,
                    detail=f"Request timed out after {TOTAL_TIMEOUT} seconds",
                )

            # ツールを並列実行（タイムアウト付き）
            try:
                tool_results = await asyncio.wait_for(
                    _execute_tools_parallel(tool_registry, response.tool_calls),
                    timeout=remaining_timeout,
                )
            except asyncio.TimeoutError:
                logger.error("Tool execution timed out")
                raise HTTPException(
                    status_code=504, detail="Tool execution timed out"
                ) from None

            # ツール結果を履歴に追加
            # 長さの不一致を検出するために strict=True を使用（Python 3.10+）
            for tool_call, result in zip(
                response.tool_calls, tool_results, strict=True
            ):
                tool_message = _create_tool_result_message(tool_call, result)
                conversation_history.append(tool_message)

        # 最大イテレーション到達
        logger.error("Reached maximum iterations: %s", MAX_ITERATIONS)
        raise HTTPException(
            status_code=500,
            detail=(
                f"Reached maximum iterations ({MAX_ITERATIONS}) without final answer"
            ),
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Chat request failed")
        raise HTTPException(status_code=502, detail=f"LLM API error: {str(e)}") from e


async def _execute_tools_parallel(
    tool_registry: ToolRegistry, tool_calls: list[ToolCall]
) -> list[dict[str, Any]]:
    """複数ツールを並列実行する。

    Args:
        tool_registry: ツールレジストリ
        tool_calls: ツール呼び出しリスト

    Returns:
        実行結果のリスト。成功時は {"success": True, "result": ...}、
        失敗時は {"success": False, "error": ..., "error_type": ...} を含む。
    """
    loop = asyncio.get_running_loop()

    async def execute_single_tool(tool_call: ToolCall) -> dict[str, Any]:
        """単一ツールを実行する。

        Args:
            tool_call: ツール呼び出し

        Returns:
            実行結果の辞書
        """
        try:
            # ToolRegistry.execute は同期関数なので run_in_executor を使用
            result = await loop.run_in_executor(
                None,
                lambda: tool_registry.execute(tool_call.name, **tool_call.parameters),
            )
            return {"success": True, "result": result}
        except (KeyError, ValueError) as e:
            # 想定内のエラー: LLMに詳細を返す
            logger.error(
                "Expected error in tool %s (%s): %s",
                tool_call.name,
                type(e).__name__,
                str(e),
            )
            return {
                "success": False,
                "error": str(e),
                "error_type": type(e).__name__,
            }
        except Exception as e:
            # 予期しないエラー: ログに記録し、汎用メッセージを返す（機密情報漏洩を防ぐ）
            logger.exception(
                "Unexpected error in tool %s: %s", tool_call.name, type(e).__name__
            )
            return {
                "success": False,
                "error": (
                    "Internal tool execution error. Check server logs for details."
                ),
                "error_type": "InternalError",
            }

    # 並列実行
    results = await asyncio.gather(*[execute_single_tool(tc) for tc in tool_calls])
    return results


def _create_tool_result_message(tool_call: ToolCall, result: dict[str, Any]) -> Message:
    """ツール実行結果からメッセージを生成する。

    Args:
        tool_call: ツール呼び出し
        result: 実行結果（success, result/error を含む辞書）

    Returns:
        tool role のメッセージ
    """
    if result["success"]:
        # 成功時: result を JSON シリアライズ
        content = json.dumps(result["result"], ensure_ascii=False)
    else:
        # 失敗時: error と error_type を JSON シリアライズ
        content = json.dumps(
            {
                "error": result["error"],
                "error_type": result["error_type"],
            },
            ensure_ascii=False,
        )

    return Message(
        role="tool",
        content=content,
        tool_call_id=tool_call.id,
        name=tool_call.name,
    )
