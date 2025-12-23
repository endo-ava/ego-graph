"""Config層のテスト。"""

import pytest
from pydantic import ValidationError, SecretStr
from backend.config import LLMConfig, BackendConfig


class TestLLMConfig:
    """LLMConfigのテスト。"""

    def test_default_values(self):
        """デフォルト値の検証。"""
        # Arrange & Act: model_construct()を使ってデフォルト値で構築
        config = LLMConfig.model_construct(api_key="test-key")

        # Assert: デフォルト値を検証
        assert config.provider == "openai"
        assert config.model_name == "gpt-4o-mini"
        assert config.temperature == 0.7
        assert config.max_tokens == 2048

    def test_custom_values(self):
        """カスタム値の設定。"""
        # Arrange & Act: カスタム値でLLMConfigを構築
        config = LLMConfig.model_construct(
            provider="anthropic",
            api_key="test-key",
            model_name="claude-3-5-sonnet-20241022",
            temperature=0.5,
            max_tokens=4096,
        )

        # Assert: カスタム値が正しく設定されることを検証
        assert config.provider == "anthropic"
        assert config.model_name == "claude-3-5-sonnet-20241022"
        assert config.temperature == 0.5
        assert config.max_tokens == 4096

    def test_missing_api_key_raises_error(self):
        """API Keyが必須であることを確認（環境変数経由）。"""
        # Arrange: 環境変数がクリアされた状態を想定

        # Act & Assert: API Keyなしでの作成時にValidationErrorが発生することを検証
        with pytest.raises(ValidationError):
            LLMConfig()

    def test_api_key_is_secret(self):
        """API Keyが SecretStr として扱われる。"""
        # Arrange & Act: SecretStrでラップしたAPI KeyでLLMConfigを構築
        config = LLMConfig.model_construct(api_key=SecretStr("test-key"))

        # Assert: API KeyがSecretStrとして扱われることを検証
        assert isinstance(config.api_key, SecretStr)
        assert config.api_key.get_secret_value() == "test-key"


class TestBackendConfig:
    """BackendConfigのテスト。"""

    def test_default_values(self):
        """デフォルト値の検証。"""
        # Arrange & Act: デフォルト値でBackendConfigを構築
        config = BackendConfig.model_construct()

        # Assert: デフォルト値を検証
        assert config.host == "127.0.0.1"
        assert config.port == 8000
        assert config.reload is True
        assert config.log_level == "INFO"
        assert config.api_key is None
        assert config.llm is None
        assert config.r2 is None

    def test_custom_values(self):
        """カスタム値の設定。"""
        # Arrange & Act: カスタム値でBackendConfigを構築
        config = BackendConfig.model_construct(
            host="0.0.0.0",
            port=9000,
            reload=False,
            api_key=SecretStr("custom-key"),  # SecretStrでラップして渡す
            log_level="DEBUG",
        )

        # Assert: カスタム値が正しく設定されることを検証
        assert config.host == "0.0.0.0"
        assert config.port == 9000
        assert config.reload is False
        assert config.api_key.get_secret_value() == "custom-key"
        assert config.log_level == "DEBUG"

    def test_from_env_missing_r2_raises_error(self, monkeypatch):
        """R2設定が不足している場合のエラー。"""
        # Arrange: R2Config()の初期化をモックしてValidationErrorを発生させる
        from unittest.mock import patch

        with patch("backend.config.R2Config") as mock_r2_config:
            mock_r2_config.side_effect = ValidationError.from_exception_data(
                "R2Config", [{"type": "missing", "loc": ("R2_ENDPOINT_URL",), "msg": "Field required", "input": {}}]
            )

            # Act & Assert: R2設定不足時にValueErrorが発生することを検証
            with pytest.raises(ValueError, match="R2 configuration is missing"):
                BackendConfig.from_env()

    def test_from_env_with_r2_only(self, monkeypatch):
        """R2設定のみでロード可能（LLMは任意）。"""
        # Arrange: R2環境変数を設定
        monkeypatch.setenv("R2_ENDPOINT_URL", "https://test.r2.cloudflarestorage.com")
        monkeypatch.setenv("R2_ACCESS_KEY_ID", "test_key")
        monkeypatch.setenv("R2_SECRET_ACCESS_KEY", "test_secret")
        monkeypatch.setenv("R2_BUCKET_NAME", "test-bucket")

        # LLM環境変数はクリア
        monkeypatch.delenv("LLM_API_KEY", raising=False)

        # Act: 環境変数からConfigをロード
        config = BackendConfig.from_env()

        # Assert: R2のみが設定され、LLMはNoneであることを検証
        assert config.r2 is not None
        assert config.r2.bucket_name == "test-bucket"
        assert config.llm is None  # LLMは任意なのでNone

    def test_from_env_with_llm_and_r2(self, monkeypatch):
        """LLMとR2の両方が設定されている場合。"""
        # Arrange: R2とLLMの環境変数を設定
        # R2環境変数
        monkeypatch.setenv("R2_ENDPOINT_URL", "https://test.r2.cloudflarestorage.com")
        monkeypatch.setenv("R2_ACCESS_KEY_ID", "test_key")
        monkeypatch.setenv("R2_SECRET_ACCESS_KEY", "test_secret")
        monkeypatch.setenv("R2_BUCKET_NAME", "test-bucket")

        # LLM環境変数
        monkeypatch.setenv("LLM_API_KEY", "test-llm-key")
        monkeypatch.setenv("LLM_PROVIDER", "anthropic")

        # Act: 環境変数からConfigをロード
        config = BackendConfig.from_env()

        # Assert: R2とLLM両方が設定されることを検証
        assert config.r2 is not None
        assert config.llm is not None
        assert config.llm.provider == "anthropic"

    def test_validate_for_production_with_api_key_and_llm(self, mock_backend_config):
        """API KeyとLLMがあれば本番環境検証成功。"""
        # Arrange: mock_backend_configにはすでにapi_keyとllmが設定されている

        # Act: 本番環境検証を実行
        mock_backend_config.validate_for_production()

        # Assert: エラーが発生しないことを検証（実行が完了すればOK）

    def test_validate_for_production_missing_api_key(self, mock_backend_config):
        """API Keyがなければ本番環境検証失敗。"""
        # Arrange: API Keyを削除
        mock_backend_config.api_key = None

        # Act & Assert: API Key不足でValueErrorが発生することを検証
        with pytest.raises(ValueError, match="BACKEND_API_KEY is required"):
            mock_backend_config.validate_for_production()

    def test_validate_for_production_missing_llm(self, mock_backend_config):
        """LLMがなければ本番環境検証失敗。"""
        # Arrange: LLM設定を削除
        mock_backend_config.llm = None

        # Act & Assert: LLM設定不足でValueErrorが発生することを検証
        with pytest.raises(ValueError, match="LLM configuration is required"):
            mock_backend_config.validate_for_production()
