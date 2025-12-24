"""EgoGraph Backend設定管理。

shared.configを拡張し、LLM APIとバックエンドサーバー固有の設定を追加します。
"""

import logging
from typing import Optional

from pydantic import Field, SecretStr, ValidationError
from pydantic_settings import BaseSettings, SettingsConfigDict

from shared.config import R2Config


class LLMConfig(BaseSettings):
    """LLM API設定。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    provider: str = Field("openai", alias="LLM_PROVIDER")
    api_key: SecretStr = Field(..., alias="LLM_API_KEY")
    model_name: str = Field("gpt-4o-mini", alias="LLM_MODEL_NAME")
    temperature: float = Field(0.7, alias="LLM_TEMPERATURE")
    max_tokens: int = Field(2048, alias="LLM_MAX_TOKENS")


class BackendConfig(BaseSettings):
    """Backend APIサーバー設定。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    # サーバー設定
    host: str = Field("127.0.0.1", alias="BACKEND_HOST")
    port: int = Field(8000, alias="BACKEND_PORT")
    reload: bool = Field(True, alias="BACKEND_RELOAD")

    # オプショナル認証
    api_key: Optional[SecretStr] = Field(None, alias="BACKEND_API_KEY")

    # CORS設定
    cors_origins: str = Field("*", alias="CORS_ORIGINS")  # カンマ区切り

    # ロギング
    log_level: str = Field("INFO", alias="LOG_LEVEL")

    # サブ設定
    llm: Optional[LLMConfig] = None
    r2: Optional[R2Config] = None

    @classmethod
    def from_env(cls) -> "BackendConfig":
        """環境変数から設定をロードします。

        Returns:
            設定済みのBackendConfigインスタンス

        Raises:
            ValueError: 必須の環境変数が不足している場合
        """
        config = cls()

        # LLM設定のロード
        try:
            config.llm = LLMConfig()
        except (ValidationError, ValueError):
            logging.warning(
                "LLM config not available. Chat endpoints will be disabled."
            )

        # R2設定のロード
        try:
            config.r2 = R2Config()
        except (ValidationError, ValueError):
            logging.error("R2 config is required for backend operation")
            raise ValueError("R2 configuration is missing. Please set R2_* env vars.")

        # ロギング設定
        logging.basicConfig(
            level=getattr(logging, config.log_level.upper()),
            format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        )

        return config

    def validate_for_production(self) -> None:
        """本番環境用の設定を検証します。

        Raises:
            ValueError: 本番環境で必須の設定が不足している場合
        """
        if not self.api_key:
            raise ValueError("BACKEND_API_KEY is required for production")
        if not self.llm:
            raise ValueError("LLM configuration is required for production")
