"""Terminal API ルート。

tmux セッション一覧の取得などを提供します。
"""

import json
import logging
import re
import secrets

import anyio
from starlette.exceptions import HTTPException
from starlette.requests import Request
from starlette.responses import JSONResponse
from starlette.routing import Route, WebSocketRoute
from starlette.websockets import WebSocket

from gateway.dependencies import get_config, verify_gateway_token
from gateway.domain.models import SessionStatus
from gateway.infrastructure.tmux import list_sessions
from gateway.services.websocket_handler import TerminalWebSocketHandler

logger = logging.getLogger(__name__)


# 接続状態のキャッシュ（簡易実装、プロセス内メモリのみ）
_active_connections: set[str] = set()


async def get_sessions(request: Request) -> JSONResponse:
    """tmux セッション一覧を取得します。

    `tmux list-sessions` を使用してセッション情報を取得し、
    `^agent-[0-9]{4}$` パターンに一致するセッションのみを返します。
    接続状態はプロセス内メモリで管理されます。

    Args:
        request: Starlette リクエストオブジェクト

    Returns:
        セッション一覧を含む JSONResponse

    Raises:
        HTTPException: tmux コマンドが失敗した場合
    """
    await verify_gateway_token(request)

    try:
        tmux_sessions = await anyio.to_thread.run_sync(list_sessions)
    except OSError as e:
        logger.error("Failed to list tmux sessions: %s", e)
        raise HTTPException(status_code=500, detail="Failed to list sessions") from e
    except Exception as e:
        logger.exception("Unexpected error listing sessions")
        raise HTTPException(status_code=500, detail="Unexpected error") from e

    # セッション情報を API レスポンス形式に変換
    sessions = [
        _build_session_response(session.name, session) for session in tmux_sessions
    ]

    return JSONResponse(
        {
            "sessions": sessions,
            "count": len(sessions),
        }
    )


async def get_session(request: Request) -> JSONResponse:
    """指定された tmux セッション情報を取得します。"""
    await verify_gateway_token(request)

    session_id = request.path_params.get("session_id")
    if not session_id or not _validate_session_id(session_id):
        raise HTTPException(status_code=400, detail="Invalid session_id format")

    try:
        tmux_sessions = await anyio.to_thread.run_sync(list_sessions)
    except OSError as e:
        logger.error("Failed to list tmux sessions: %s", e)
        raise HTTPException(status_code=500, detail="Failed to list sessions") from e
    except Exception as e:
        logger.exception("Unexpected error listing sessions")
        raise HTTPException(status_code=500, detail="Unexpected error") from e

    target = next(
        (session for session in tmux_sessions if session.name == session_id), None
    )
    if target is None:
        raise HTTPException(status_code=404, detail="Session not found")

    return JSONResponse(_build_session_response(session_id, target))


def mark_session_connected(session_name: str) -> None:
    """セッションを接続中としてマークします。

    Args:
        session_name: セッション名
    """
    _active_connections.add(session_name)
    logger.info("Session marked as connected: %s", session_name)


def mark_session_disconnected(session_name: str) -> None:
    """セッションを切断としてマークします。

    Args:
        session_name: セッション名
    """
    _active_connections.discard(session_name)
    logger.info("Session marked as disconnected: %s", session_name)


def get_terminal_routes() -> list[Route]:
    """Terminal API ルートを取得します。

    Returns:
        ルート定義のリスト
    """
    return [
        Route("/v1/terminal/sessions", get_sessions, methods=["GET"]),
        Route("/v1/terminal/sessions/{session_id}", get_session, methods=["GET"]),
        WebSocketRoute("/ws/terminal", terminal_websocket),
    ]


async def terminal_websocket(websocket: WebSocket) -> None:
    """端末WebSocketエンドポイント。

    tmuxセッションとの双方向通信を提供する。

    Args:
        websocket: WebSocket接続

    Query Parameters:
        session_id: tmuxセッションID (例: agent-0001)

    メッセージ形式 (Client -> Server):
        - {"type": "auth", "api_key": "..."}
        - {"type": "input", "data_b64": "..."}
        - {"type": "resize", "cols": 120, "rows": 30}
        - {"type": "ping"}

    メッセージ形式 (Server -> Client):
        - {"type": "output", "data_b64": "..."}
        - {"type": "status", "state": "connected|reconnecting|closed"}
        - {"type": "error", "code": "...", "message": "..."}
        - {"type": "ping"}
    """
    # クエリパラメータを取得
    session_id = websocket.query_params.get("session_id")

    # パラメータ検証
    if not session_id:
        logger.warning("Missing required parameter: session_id")
        await websocket.close(
            code=1008, reason="Missing required parameter: session_id"
        )
        return

    # セッションIDのバリデーション (形式: agent-XXXX)
    if not _validate_session_id(session_id):
        logger.warning("Invalid session_id format: %s", session_id)
        await websocket.close(code=1008, reason="Invalid session_id format")
        return

    # WebSocket接続を確立してから認証メッセージを受け取る
    await websocket.accept()

    if not await _authenticate_websocket(websocket, session_id):
        return

    # セッションが実際に存在するか確認
    if not await anyio.to_thread.run_sync(session_exists, session_id):
        logger.warning("Session does not exist: %s", session_id)
        await websocket.close(code=1008, reason="Session does not exist")
        return

    # 接続状態を更新
    mark_session_connected(session_id)

    logger.info("WebSocket connection established for session: %s", session_id)

    # ハンドラーを作成して実行
    handler = TerminalWebSocketHandler(websocket, session_id)

    try:
        await handler.handle()
    except Exception as e:
        logger.error("Error in terminal websocket for session %s: %s", session_id, e)
    finally:
        # 接続状態を更新
        mark_session_disconnected(session_id)
        logger.info("WebSocket connection closed for session: %s", session_id)


def _validate_session_id(session_id: str) -> bool:
    """セッションIDの形式を検証する。

    Args:
        session_id: 検証するセッションID

    Returns:
        形式が正しい場合はTrue、そうでない場合はFalse
    """
    # 形式: agent-XXXX (XXXXは4桁の数字)
    pattern = r"^agent-[0-9]{4}$"
    return bool(re.match(pattern, session_id))


def _build_session_response(session_id: str, session) -> dict[str, str]:
    """tmux セッション情報を API レスポンス形式に変換する。"""
    status = (
        SessionStatus.CONNECTED
        if session_id in _active_connections
        else SessionStatus.DISCONNECTED
    )
    return {
        "session_id": session_id,
        "name": session.name,
        "last_activity": session.last_activity.isoformat(),
        "created_at": session.created_at.isoformat(),
        "status": status.value,
    }


def _is_valid_api_key(api_key: str) -> bool:
    expected_api_key = get_config().api_key.get_secret_value()
    return secrets.compare_digest(api_key, expected_api_key)


async def _authenticate_websocket(websocket: WebSocket, session_id: str) -> bool:
    """WebSocket接続の初回認証を行う。"""
    try:
        auth_message = await websocket.receive_text()
        payload = json.loads(auth_message)
    except Exception:
        logger.warning("Authentication message is invalid for session: %s", session_id)
        await websocket.close(code=1008, reason="Invalid authentication message")
        return False

    if payload.get("type") != "auth":
        logger.warning(
            "Authentication message type mismatch for session: %s", session_id
        )
        await websocket.close(code=1008, reason="Authentication required")
        return False

    api_key = payload.get("api_key")
    if not isinstance(api_key, str) or not api_key:
        logger.warning("Missing api_key in auth message for session: %s", session_id)
        await websocket.close(code=1008, reason="Missing api_key")
        return False

    if not _is_valid_api_key(api_key):
        logger.warning("Authentication failed for session: %s", session_id)
        await websocket.close(code=1008, reason="Authentication failed")
        return False

    return True
