"""プッシュ通知APIの単体テスト。"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from starlette.exceptions import HTTPException

from gateway.api.push import send_webhook
from gateway.domain.models import WebhookPayload

# ============================================================================
# Fixtures
# ============================================================================


@pytest.fixture
def mock_webhook_payload():
    """テスト用Webhookペイロード。"""
    return {
        "type": "task_completed",
        "session_id": "agent-0001",
        "title": "タスク完了",
        "body": "タスクが正常に完了しました",
    }


@pytest.fixture
def mock_config():
    """テスト用設定。"""
    config = MagicMock()
    config.default_user_id = "test_user"
    config.webhook_secret = MagicMock()
    config.webhook_secret.get_secret_value.return_value = "test_webhook_secret_32_bytes"
    return config


@pytest.fixture
def mock_request():
    """テスト用リクエスト。"""
    request = MagicMock()
    request.headers = {"X-Webhook-Secret": "test_webhook_secret_32_bytes"}
    request.json = AsyncMock()
    return request


# ============================================================================
# send_webhookテスト
# ============================================================================


class TestSendWebhook:
    """send_webhookエンドポイントのテスト。"""

    @pytest.mark.asyncio
    async def test_send_webhook_missing_secret_returns_401(self, mock_request):
        """Webhookシークレットが欠落している場合に401エラーを返すことを確認する。"""
        # Arrange
        mock_request.headers = {}  # シークレットを含まない

        with patch("gateway.api.push.verify_webhook_secret") as mock_verify:
            mock_verify.side_effect = HTTPException(
                status_code=401, detail="Missing webhook secret"
            )

            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 401
            assert "Missing webhook secret" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_invalid_secret_returns_401(self, mock_request):
        """無効なWebhookシークレットの場合に401エラーを返すことを確認する。"""
        # Arrange
        with patch("gateway.api.push.verify_webhook_secret") as mock_verify:
            mock_verify.side_effect = HTTPException(
                status_code=401, detail="Invalid webhook secret"
            )

            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 401
            assert "Invalid webhook secret" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_invalid_json_returns_400(self, mock_request):
        """無効なJSONの場合に400エラーを返すことを確認する。"""
        # Arrange
        mock_request.json.side_effect = Exception("Invalid JSON")

        with patch("gateway.api.push.verify_webhook_secret"):
            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 400
            assert "invalid_body" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_missing_type_returns_400(
        self, mock_request, mock_webhook_payload
    ):
        """typeが欠落している場合に400エラーを返すことを確認する。"""
        # Arrange
        invalid_payload = mock_webhook_payload.copy()
        del invalid_payload["type"]
        mock_request.json.return_value = invalid_payload

        with patch("gateway.api.push.verify_webhook_secret"):
            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 400
            assert "invalid_payload" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_missing_session_id_returns_400(
        self, mock_request, mock_webhook_payload
    ):
        """session_idが欠落している場合に400エラーを返すことを確認する。"""
        # Arrange
        invalid_payload = mock_webhook_payload.copy()
        del invalid_payload["session_id"]
        mock_request.json.return_value = invalid_payload

        with patch("gateway.api.push.verify_webhook_secret"):
            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 400
            assert "invalid_payload" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_missing_title_returns_400(
        self, mock_request, mock_webhook_payload
    ):
        """titleが欠落している場合に400エラーを返すことを確認する。"""
        # Arrange
        invalid_payload = mock_webhook_payload.copy()
        del invalid_payload["title"]
        mock_request.json.return_value = invalid_payload

        with patch("gateway.api.push.verify_webhook_secret"):
            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 400
            assert "invalid_payload" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_missing_body_returns_400(
        self, mock_request, mock_webhook_payload
    ):
        """bodyが欠落している場合に400エラーを返すことを確認する。"""
        # Arrange
        invalid_payload = mock_webhook_payload.copy()
        del invalid_payload["body"]
        mock_request.json.return_value = invalid_payload

        with patch("gateway.api.push.verify_webhook_secret"):
            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 400
            assert "invalid_payload" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_empty_title_returns_400(
        self, mock_request, mock_webhook_payload
    ):
        """titleが空文字列の場合に400エラーを返すことを確認する。"""
        # Arrange
        invalid_payload = mock_webhook_payload.copy()
        invalid_payload["title"] = ""  # 空文字列はmin_length=1でバリデーションエラー
        mock_request.json.return_value = invalid_payload

        with patch("gateway.api.push.verify_webhook_secret"):
            # Act & Assert
            with pytest.raises(HTTPException) as exc_info:
                await send_webhook(mock_request)

            assert exc_info.value.status_code == 400
            assert "invalid_payload" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_send_webhook_success_returns_200(
        self, mock_request, mock_webhook_payload, mock_config
    ):
        """正常なリクエストで200レスポンスを返すことを確認する。"""
        # Arrange
        mock_request.json.return_value = mock_webhook_payload

        with (
            patch("gateway.api.push.verify_webhook_secret"),
            patch("gateway.api.push.get_config", return_value=mock_config),
            patch("gateway.api.push.get_fcm_service") as mock_get_fcm,
        ):
            # モックの設定
            mock_fcm_service = MagicMock()
            mock_fcm_service.handle_webhook = AsyncMock(
                return_value={
                    "success_count": 1,
                    "failure_count": 0,
                    "invalid_tokens": [],
                }
            )
            mock_get_fcm.return_value = mock_fcm_service

            # Act
            response = await send_webhook(mock_request)

            # Assert
            assert response.status_code == 200
            body = response.body.decode()
            import json

            result = json.loads(body)
            assert result["success_count"] == 1
            assert result["failure_count"] == 0
            assert result["invalid_tokens"] == []

    @pytest.mark.asyncio
    async def test_send_webhook_calls_fcm_service_with_correct_args(
        self, mock_request, mock_webhook_payload, mock_config
    ):
        """FCMサービスが正しい引数で呼び出されることを確認する。"""
        # Arrange
        mock_request.json.return_value = mock_webhook_payload

        with (
            patch("gateway.api.push.verify_webhook_secret"),
            patch("gateway.api.push.get_config", return_value=mock_config),
            patch("gateway.api.push.get_fcm_service") as mock_get_fcm,
        ):
            # モックの設定
            mock_fcm_service = MagicMock()
            mock_fcm_service.handle_webhook = AsyncMock(
                return_value={
                    "success_count": 1,
                    "failure_count": 0,
                    "invalid_tokens": [],
                }
            )
            mock_get_fcm.return_value = mock_fcm_service

            # Act
            await send_webhook(mock_request)

            # Assert
            mock_fcm_service.handle_webhook.assert_called_once()
            call_args = mock_fcm_service.handle_webhook.call_args
            payload = call_args[0][0]
            user_id = call_args[0][1]

            assert isinstance(payload, WebhookPayload)
            assert payload.type == "task_completed"
            assert payload.session_id == "agent-0001"
            assert payload.title == "タスク完了"
            assert payload.body == "タスクが正常に完了しました"
            assert user_id == "test_user"

    @pytest.mark.asyncio
    async def test_send_webhook_with_partial_failure_returns_200(
        self, mock_request, mock_webhook_payload, mock_config
    ):
        """一部のデバイスで送信が失敗しても200を返すことを確認する。"""
        # Arrange
        mock_request.json.return_value = mock_webhook_payload

        with (
            patch("gateway.api.push.verify_webhook_secret"),
            patch("gateway.api.push.get_config", return_value=mock_config),
            patch("gateway.api.push.get_fcm_service") as mock_get_fcm,
        ):
            # モックの設定
            mock_fcm_service = MagicMock()
            mock_fcm_service.handle_webhook = AsyncMock(
                return_value={
                    "success_count": 1,
                    "failure_count": 1,
                    "invalid_tokens": ["invalid_token"],
                }
            )
            mock_get_fcm.return_value = mock_fcm_service

            # Act
            response = await send_webhook(mock_request)

            # Assert
            assert response.status_code == 200
            body = response.body.decode()
            import json

            result = json.loads(body)
            assert result["success_count"] == 1
            assert result["failure_count"] == 1
            assert "invalid_token" in result["invalid_tokens"]
