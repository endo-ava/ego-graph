"""Terminal API の統合テスト。

WebSocket接続とメッセージ handling をテストする。
"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from gateway.api.terminal import terminal_websocket
from gateway.domain.models import (
    WSErrorMessage,
    WSInputMessage,
    WSOutputMessage,
    WSPingMessage,
    WSPongMessage,
    WSResizeMessage,
    WSStatusMessage,
)

# ============================================================================
# Fixtures
# ============================================================================


@pytest.fixture
def mock_token_verifier():
    """TokenVerifierのモック。"""
    verifier = MagicMock()
    verifier.verify = MagicMock(return_value=True)
    return verifier


@pytest.fixture
def valid_token():
    """有効な認証トークン。"""
    return "valid_token_12345678901234567890"


@pytest.fixture
def invalid_token():
    """無効な認証トークン。"""
    return "invalid_token"


@pytest.fixture
def valid_session_id():
    """有効なセッションID。"""
    return "agent-0001"


@pytest.fixture
def invalid_session_id():
    """無効なセッションID。"""
    return "invalid-session"


# ============================================================================
# WebSocket認証テスト
# ============================================================================


class TestWebSocketAuthentication:
    """WebSocket認証のテスト。"""

    @pytest.mark.asyncio
    async def test_websocket_accepts_valid_token(
        self, mock_token_verifier, valid_token, valid_session_id
    ):
        """有効なトークンでWebSocket接続が確立されることを確認する。"""
        # Arrange
        mock_websocket = MagicMock()
        mock_websocket.query_params = {"session_id": valid_session_id}
        mock_websocket.accept = AsyncMock()
        mock_websocket.receive_text = AsyncMock(
            return_value=f'{{"type":"auth","api_key":"{valid_token}"}}'
        )

        with (
            patch("gateway.api.terminal._is_valid_api_key", return_value=True),
            patch(
                "gateway.api.terminal.TerminalWebSocketHandler"
            ) as mock_handler_class,
        ):
            mock_handler = MagicMock()
            mock_handler.handle = AsyncMock()
            mock_handler_class.return_value = mock_handler

            # Act
            await terminal_websocket(mock_websocket)

            # Assert
            mock_websocket.accept.assert_called_once()
            mock_handler.handle.assert_called_once()

    @pytest.mark.asyncio
    async def test_websocket_rejects_invalid_token(
        self, invalid_token, valid_session_id
    ):
        """無効なトークンでWebSocket接続が拒否されることを確認する。"""
        # Arrange
        mock_websocket = MagicMock()
        mock_websocket.query_params = {"session_id": valid_session_id}
        mock_websocket.receive_text = AsyncMock(
            return_value=f'{{"type":"auth","api_key":"{invalid_token}"}}'
        )
        mock_websocket.accept = AsyncMock()
        mock_websocket.close = AsyncMock()

        with patch("gateway.api.terminal._is_valid_api_key", return_value=False):
            # Act
            await terminal_websocket(mock_websocket)

            # Assert
            mock_websocket.accept.assert_called_once()
            mock_websocket.close.assert_called_once_with(
                code=1008, reason="Authentication failed"
            )

    @pytest.mark.asyncio
    async def test_websocket_rejects_missing_token(self, valid_session_id):
        """トークンが欠落している場合、WebSocket接続が拒否されることを確認する。"""
        # Arrange
        mock_websocket = MagicMock()
        mock_websocket.query_params = {"session_id": valid_session_id}
        mock_websocket.receive_text = AsyncMock(return_value='{"type":"auth"}')
        mock_websocket.accept = AsyncMock()
        mock_websocket.close = AsyncMock()

        # Act
        await terminal_websocket(mock_websocket)

        # Assert
        mock_websocket.accept.assert_called_once()
        mock_websocket.close.assert_called_once()


# ============================================================================
# セッションIDバリデーションテスト
# ============================================================================


class TestSessionIdValidation:
    """セッションIDバリデーションのテスト。"""

    @pytest.mark.asyncio
    async def test_websocket_rejects_invalid_session_id(
        self, valid_token, invalid_session_id
    ):
        """無効なセッションIDでWebSocket接続が拒否されることを確認する。"""
        # Arrange
        mock_websocket = MagicMock()
        mock_websocket.query_params = {"session_id": invalid_session_id}
        mock_websocket.close = AsyncMock()

        with patch("gateway.api.terminal._is_valid_api_key", return_value=True):
            # Act
            await terminal_websocket(mock_websocket)

            # Assert
            mock_websocket.accept.assert_not_called()
            mock_websocket.close.assert_called_once_with(
                code=1008, reason="Invalid session_id format"
            )


# ============================================================================
# WebSocketメッセージ handling テスト
# ============================================================================


class TestWebSocketMessageHandling:
    """WebSocketメッセージ handling のテスト。"""

    @pytest.mark.asyncio
    async def test_input_message_handling(self):
        """入力メッセージが正しく処理されることを確認する。"""
        # Arrange
        message = WSInputMessage(data_b64="SGVsbG8=")  # "Hello" in base64

        # Act
        data = message.decode_data()

        # Assert
        assert data == b"Hello"

    @pytest.mark.asyncio
    async def test_output_message_creation(self):
        """出力メッセージが正しく作成されることを確認する。"""
        # Arrange
        test_data = b"World"

        # Act
        message = WSOutputMessage.from_bytes(test_data)

        # Assert
        assert message.type == "output"
        assert message.data_b64 == "V29ybGQ="  # "World" in base64

    @pytest.mark.asyncio
    async def test_invalid_base64_rejected(self):
        """無効なBase64エンコーディングが拒否されることを確認する。"""
        # Arrange & Act & Assert
        with pytest.raises(ValueError, match="Invalid base64"):
            WSInputMessage(data_b64="invalid_base64!@")


# ============================================================================
# WebSocketメッセージスキーマテスト
# ============================================================================


class TestWebSocketMessageSchemas:
    """WebSocketメッセージスキーマのテスト。"""

    def test_input_message_schema(self):
        """入力メッセージスキーマを検証する。"""
        # Arrange
        data = {"type": "input", "data_b64": "SGVsbG8="}

        # Act
        message = WSInputMessage(**data)

        # Assert
        assert message.type == "input"
        assert message.data_b64 == "SGVsbG8="

    def test_resize_message_schema(self):
        """画面サイズ変更メッセージスキーマを検証する。"""
        # Arrange
        data = {"type": "resize", "cols": 120, "rows": 30}

        # Act
        message = WSResizeMessage(**data)

        # Assert
        assert message.type == "resize"
        assert message.cols == 120
        assert message.rows == 30

    def test_ping_message_schema(self):
        """Pingメッセージスキーマを検証する。"""
        # Arrange
        data = {"type": "ping"}

        # Act
        message = WSPingMessage(**data)

        # Assert
        assert message.type == "ping"

    def test_status_message_schema(self):
        """状態メッセージスキーマを検証する。"""
        # Arrange
        data = {"type": "status", "state": "connected"}

        # Act
        message = WSStatusMessage(**data)

        # Assert
        assert message.type == "status"
        assert message.state == "connected"

    def test_error_message_schema(self):
        """エラーメッセージスキーマを検証する。"""
        # Arrange
        data = {"type": "error", "code": "test_error", "message": "Test error message"}

        # Act
        message = WSErrorMessage(**data)

        # Assert
        assert message.type == "error"
        assert message.code == "test_error"
        assert message.message == "Test error message"

    def test_pong_message_schema(self):
        """Pongメッセージスキーマを検証する。"""
        # Arrange
        data = {"type": "pong"}

        # Act
        message = WSPongMessage(**data)

        # Assert
        assert message.type == "pong"
