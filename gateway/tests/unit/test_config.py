"""設定管理モジュールの単体テスト。"""

from unittest.mock import patch

import pytest
from pydantic import ValidationError

from gateway.config import GatewayConfig

# ============================================================================
# GatewayConfigテスト
# ============================================================================


class TestGatewayConfig:
    """GatewayConfigクラスのテスト。"""

    def test_config_loads_default_values(self):
        """デフォルト値が正しくロードされることを確認する。"""
        # Arrange
        env_vars = {
            "GATEWAY_API_KEY": "test_api_key_32_bytes_or_more",
            "GATEWAY_WEBHOOK_SECRET": "test_webhook_secret_32_bytes_or_more",
        }

        # Act
        with patch.dict("os.environ", env_vars, clear=True):
            config = GatewayConfig()

        # Assert
        assert config.host == "0.0.0.0"
        assert config.port == 8001
        assert config.api_key.get_secret_value() == "test_api_key_32_bytes_or_more"
        assert (
            config.webhook_secret.get_secret_value()
            == "test_webhook_secret_32_bytes_or_more"
        )
        assert config.cors_origins == "*"
        assert config.log_level == "INFO"
        assert config.fcm_project_id is None
        assert config.default_user_id == "default_user"
        assert config.session_pattern == r"^agent-[0-9]{4}$"

    def test_config_loads_from_environment_variables(self):
        """環境変数から設定が正しくロードされることを確認する。"""
        # Arrange
        env_vars = {
            "GATEWAY_HOST": "127.0.0.1",
            "GATEWAY_PORT": "9001",
            "GATEWAY_API_KEY": "env_api_key_32_bytes_or_more",
            "GATEWAY_WEBHOOK_SECRET": "env_webhook_secret_32_bytes_or_more",
            "CORS_ORIGINS": "https://example.com",
            "LOG_LEVEL": "DEBUG",
            "GCM_PROJECT_ID": "test-project",
            "DEFAULT_USER_ID": "test_user",
            "SESSION_PATTERN": r"^test-[0-9]+$",
        }

        # Act
        with patch.dict("os.environ", env_vars, clear=True):
            config = GatewayConfig()

        # Assert
        assert config.host == "127.0.0.1"
        assert config.port == 9001
        assert config.api_key.get_secret_value() == "env_api_key_32_bytes_or_more"
        assert (
            config.webhook_secret.get_secret_value()
            == "env_webhook_secret_32_bytes_or_more"
        )
        assert config.cors_origins == "https://example.com"
        assert config.log_level == "DEBUG"
        assert config.fcm_project_id == "test-project"
        assert config.default_user_id == "test_user"
        assert config.session_pattern == r"^test-[0-9]+$"

    def test_config_requires_api_key(self):
        """必須のAPIキーが未設定の場合はバリデーションエラーになることを確認する。"""
        # Arrange
        env_vars = {
            "USE_ENV_FILE": "false",
            "GATEWAY_WEBHOOK_SECRET": "test_webhook_secret_32_bytes_or_more",
        }

        # Act & Assert
        with patch.dict("os.environ", env_vars, clear=True):
            import importlib

            import gateway.config as config_module

            importlib.reload(config_module)
            with pytest.raises(ValidationError) as exc_info:
                config_module.GatewayConfig()

        assert "GATEWAY_API_KEY" in str(exc_info.value)

    def test_config_requires_webhook_secret(self):
        """必須のWebhookシークレットが未設定の場合はバリデーションエラーになることを確認する。"""
        # Arrange
        env_vars = {
            "USE_ENV_FILE": "false",
            "GATEWAY_API_KEY": "test_api_key_32_bytes_or_more",
        }

        # Act & Assert
        with patch.dict("os.environ", env_vars, clear=True):
            import importlib

            import gateway.config as config_module

            importlib.reload(config_module)
            with pytest.raises(ValidationError) as exc_info:
                config_module.GatewayConfig()

        assert "GATEWAY_WEBHOOK_SECRET" in str(exc_info.value)

    def test_config_secret_str_hides_values(self):
        """SecretStrが値を隠蔽することを確認する。"""
        # Arrange
        env_vars = {
            "GATEWAY_API_KEY": "secret_key_32_bytes_or_more",
            "GATEWAY_WEBHOOK_SECRET": "secret_webhook_32_bytes_or_more",
        }

        # Act
        with patch.dict("os.environ", env_vars, clear=True):
            config = GatewayConfig()

        # Assert
        # 文字列表現では値が隠蔽される
        assert "secret_key_32_bytes_or_more" not in str(config.api_key)
        assert "secret_webhook_32_bytes_or_more" not in str(config.webhook_secret)
        # get_secret_value()で実際の値を取得できる
        assert config.api_key.get_secret_value() == "secret_key_32_bytes_or_more"
        assert (
            config.webhook_secret.get_secret_value()
            == "secret_webhook_32_bytes_or_more"
        )

    def test_from_env_loads_config_and_sets_up_logging(self):
        """from_envで設定をロードしロギングが設定されることを確認する。"""
        # Arrange
        env_vars = {
            "GATEWAY_API_KEY": "test_api_key_32_bytes_or_more",
            "GATEWAY_WEBHOOK_SECRET": "test_webhook_secret_32_bytes_or_more",
            "LOG_LEVEL": "DEBUG",
        }

        # Act
        with patch.dict("os.environ", env_vars, clear=True):
            config = GatewayConfig.from_env()

        # Assert
        assert config.log_level == "DEBUG"
        assert config.api_key.get_secret_value() == "test_api_key_32_bytes_or_more"


# ============================================================================
# モジュール定数テスト
# ============================================================================


class TestModuleConstants:
    """モジュール定数のテスト。"""

    def test_use_env_file_default(self):
        """USE_ENV_FILEのデフォルト値を確認する。"""
        # Arrange & Act & Assert
        with patch.dict("os.environ", {}, clear=True):
            import importlib

            import gateway.config as config_module

            importlib.reload(config_module)
            assert config_module.USE_ENV_FILE is True

    def test_use_env_file_can_be_disabled(self):
        """USE_ENV_FILEが環境変数で無効化できることを確認する。"""
        # Arrange & Act & Assert
        with patch.dict("os.environ", {"USE_ENV_FILE": "false"}, clear=True):
            import importlib

            import gateway.config as config_module

            importlib.reload(config_module)
            assert config_module.USE_ENV_FILE is False

    def test_gateway_env_files_when_enabled(self):
        """USE_ENV_FILEが有効な場合にGATEWAY_ENV_FILESが設定されることを確認する。"""
        # Arrange & Act & Assert
        with patch.dict("os.environ", {}, clear=True):
            import importlib

            import gateway.config as config_module

            importlib.reload(config_module)
            assert config_module.GATEWAY_ENV_FILES == ["gateway/.env"]

    def test_gateway_env_files_when_disabled(self):
        """USE_ENV_FILEが無効な場合にGATEWAY_ENV_FILESが空になることを確認する。"""
        # Arrange & Act & Assert
        with patch.dict("os.environ", {"USE_ENV_FILE": "false"}, clear=True):
            import importlib

            import gateway.config as config_module

            importlib.reload(config_module)
            assert config_module.GATEWAY_ENV_FILES == []
