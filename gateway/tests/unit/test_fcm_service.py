"""FCMサービスの単体テスト。"""

import logging
from unittest.mock import MagicMock, patch

import pytest

from gateway.domain.models import (
    PushNotificationRequest,
)
from gateway.infrastructure.repositories import PushTokenRepository
from gateway.services.fcm_service import FcmService

logger = logging.getLogger(__name__)


class TestFcmService:
    """FcmServiceクラスの単体テスト。"""

    def test_init_without_project_id(self) -> None:
        """FCMプロジェクトID未指定時の初期化テスト。

        Args:
            なし

        Returns:
            なし

        Note:
            プロジェクトID未指定時はFCMが無効化されます。
        """
        # Arrange
        mock_repo = MagicMock(spec=PushTokenRepository)

        # Act
        service = FcmService(token_repository=mock_repo, fcm_project_id=None)

        # Assert
        assert service._initialized is False

    @patch("gateway.services.fcm_service.firebase_admin")
    @patch("gateway.services.fcm_service.credentials")
    def test_init_with_project_id(
        self, mock_credentials: MagicMock, mock_firebase_admin: MagicMock
    ) -> None:
        """FCMプロジェクトID指定時の初期化テスト。

        Args:
            mock_credentials: モックされたcredentialsモジュール
            mock_firebase_admin: モックされたfirebase_adminモジュール

        Returns:
            なし
        """
        # Arrange
        mock_repo = MagicMock(spec=PushTokenRepository)
        mock_firebase_admin._apps = []
        mock_cred = MagicMock()
        mock_credentials.ApplicationDefault.return_value = mock_cred

        # Act
        service = FcmService(token_repository=mock_repo, fcm_project_id="test-project")

        # Assert
        assert service._initialized is True
        mock_firebase_admin.initialize_app.assert_called_once()

    @pytest.mark.asyncio
    async def test_send_notification_not_initialized(self) -> None:
        """FCM未初期化時の通知送信テスト。

        Args:
            なし

        Returns:
            なし

        Note:
            未初期化時は失敗カウントのみ返されます。
        """
        # Arrange
        mock_repo = MagicMock(spec=PushTokenRepository)
        service = FcmService(token_repository=mock_repo, fcm_project_id=None)

        # Act
        result = await service.send_notification(
            device_tokens=["token1"], title="Test", body="Test Body"
        )

        # Assert
        assert result["success_count"] == 0
        assert result["failure_count"] == 1
        assert result["invalid_tokens"] == []

    @pytest.mark.asyncio
    async def test_send_notification_no_tokens(self) -> None:
        """トークン未指定時の通知送信テスト。

        Args:
            なし

        Returns:
            なし
        """
        # Arrange
        mock_repo = MagicMock(spec=PushTokenRepository)
        service = FcmService(token_repository=mock_repo, fcm_project_id=None)

        # Act
        result = await service.send_notification(
            device_tokens=[], title="Test", body="Test Body"
        )

        # Assert
        assert result["success_count"] == 0
        assert result["failure_count"] == 0

    @pytest.mark.asyncio
    @patch("gateway.services.fcm_service.messaging")
    async def test_send_notification_success(self, mock_messaging: MagicMock) -> None:
        """通知送信成功のテスト。

        Args:
            mock_messaging: モックされたmessagingモジュール

        Returns:
            なし
        """
        # Arrange
        mock_repo = MagicMock(spec=PushTokenRepository)
        service = FcmService(token_repository=mock_repo, fcm_project_id="test")
        service._initialized = True

        # モックの設定
        mock_response = MagicMock()
        mock_response.success_count = 1
        mock_response.failure_count = 0
        mock_response.responses = []
        mock_messaging.send_each_for_multicast.return_value = mock_response

        # Act
        result = await service.send_notification(
            device_tokens=["token1"],
            title="Test",
            body="Test Body",
            data={"key": "value"},
        )

        # Assert
        assert result["success_count"] == 1
        assert result["failure_count"] == 0
        mock_messaging.MulticastMessage.assert_called_once()

    @pytest.mark.asyncio
    @patch("gateway.services.fcm_service.messaging")
    async def test_send_notification_invalid_token(
        self, mock_messaging: MagicMock
    ) -> None:
        """無効トークンの処理テスト。

        Args:
            mock_messaging: モックされたmessagingモジュール

        Returns:
            なし

        Note:
            無効トークンはリポジトリで無効化されます。
        """
        # Arrange
        mock_repo = MagicMock(spec=PushTokenRepository)
        service = FcmService(token_repository=mock_repo, fcm_project_id="test")
        service._initialized = True

        # モックの設定
        mock_exception = MagicMock()
        mock_exception.exception = Exception("InvalidRegistration")
        mock_response = MagicMock()
        mock_response.success_count = 0
        mock_response.failure_count = 1
        mock_response.responses = [mock_exception]
        mock_messaging.send_each_for_multicast.return_value = mock_response

        # Act
        result = await service.send_notification(
            device_tokens=["invalid_token"], title="Test", body="Test Body"
        )

        # Assert
        assert result["success_count"] == 0
        assert result["failure_count"] == 1
        assert "invalid_token" in result["invalid_tokens"]
        mock_repo.disable_token.assert_called_once_with("invalid_token")

    @pytest.mark.asyncio
    async def test_send_to_user_no_devices(self) -> None:
        """デバイス未登録ユーザーへの通知送信テスト。

        Args:
            なし

        Returns:
            なし
        """
        # Arrange
        mock_repo = MagicMock(spec=PushTokenRepository)
        mock_repo.get_tokens.return_value = []
        service = FcmService(token_repository=mock_repo, fcm_project_id=None)

        notification = PushNotificationRequest(
            title="Test", body="Test Body", data={"key": "value"}
        )

        # Act
        result = await service.send_to_user("user123", notification)

        # Assert
        assert result["success_count"] == 0
        assert result["failure_count"] == 0
        mock_repo.get_tokens.assert_called_once_with("user123")


class TestPushTokenRepository:
    """PushTokenRepositoryクラスの単体テスト。"""

    @pytest.fixture
    def mock_connection(self) -> MagicMock:
        """モックされたデータベース接続を作成します。

        Returns:
            モックされたDuckDB接続
        """
        conn = MagicMock()
        conn.execute.return_value.fetchone.return_value = None
        return conn

    @patch("gateway.infrastructure.repositories.get_db_connection")
    def test_save_token_new(
        self, mock_get_db_connection: MagicMock, mock_connection: MagicMock
    ) -> None:
        """新規トークン登録のテスト。

        Args:
            mock_get_db_connection: モックされたDB接続関数
            mock_connection: モックされたDB接続

        Returns:
            なし
        """
        # Arrange
        mock_get_db_connection.return_value.__enter__.return_value = mock_connection
        mock_connection.execute.return_value.fetchone.return_value = None

        # 既存トークンなし
        mock_connection.execute.return_value.fetchall.return_value = []

        # 新規登録後のデータ
        mock_connection.execute.return_value.fetchone.return_value = (
            1,
            "user123",
            "token1",
            "android",
            "My Device",
            True,
            "2025-02-08 12:00:00",
            "2025-02-08 12:00:00",
        )

        repo = PushTokenRepository()

        # Act
        result = repo.save_token(
            user_id="user123",
            device_token="token1",
            platform="android",
            device_name="My Device",
        )

        # Assert
        assert result.user_id == "user123"
        assert result.device_token == "token1"
        assert result.platform == "android"
        assert result.device_name == "My Device"

    @patch("gateway.infrastructure.repositories.get_db_connection")
    def test_get_tokens(
        self, mock_get_db_connection: MagicMock, mock_connection: MagicMock
    ) -> None:
        """トークン取得のテスト。

        Args:
            mock_get_db_connection: モックされたDB接続関数
            mock_connection: モックされたDB接続

        Returns:
            なし
        """
        # Arrange
        mock_get_db_connection.return_value.__enter__.return_value = mock_connection
        mock_connection.execute.return_value.fetchall.return_value = [
            (
                1,
                "user123",
                "token1",
                "android",
                "My Device",
                True,
                "2025-02-08 12:00:00",
                "2025-02-08 12:00:00",
            )
        ]

        repo = PushTokenRepository()

        # Act
        results = repo.get_tokens("user123")

        # Assert
        assert len(results) == 1
        assert results[0].user_id == "user123"
        assert results[0].device_token == "token1"

    @patch("gateway.infrastructure.repositories.get_db_connection")
    def test_update_last_seen(
        self, mock_get_db_connection: MagicMock, mock_connection: MagicMock
    ) -> None:
        """最終確認日時更新のテスト。

        Args:
            mock_get_db_connection: モックされたDB接続関数
            mock_connection: モックされたDB接続

        Returns:
            なし
        """
        # Arrange
        mock_get_db_connection.return_value.__enter__.return_value = mock_connection

        repo = PushTokenRepository()

        # Act
        repo.update_last_seen("token1")

        # Assert
        mock_connection.execute.assert_called_once()

    @patch("gateway.infrastructure.repositories.get_db_connection")
    def test_disable_token(
        self, mock_get_db_connection: MagicMock, mock_connection: MagicMock
    ) -> None:
        """トークン無効化のテスト。

        Args:
            mock_get_db_connection: モックされたDB接続関数
            mock_connection: モックされたDB接続

        Returns:
            なし
        """
        # Arrange
        mock_get_db_connection.return_value.__enter__.return_value = mock_connection

        repo = PushTokenRepository()

        # Act
        repo.disable_token("token1")

        # Assert
        mock_connection.execute.assert_called_once()
