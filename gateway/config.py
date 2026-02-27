"""Gateway 設定管理。

認証トークン、サーバー設定、Webhook シークレットなどを管理します。
"""

import logging
import os

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict

# 環境変数で .env ファイルの使用を制御（デフォルトは使用）
USE_ENV_FILE = os.getenv("USE_ENV_FILE", "true").lower() in ("true", "1", "yes")
GATEWAY_ENV_FILES = ["gateway/.env"] if USE_ENV_FILE else []


class GatewayConfig(BaseSettings):
    """Gateway サーバー設定。

    サーバー設定、認証トークン、Webhook シークレットを管理します。
    """

    model_config = SettingsConfigDict(
        env_file=GATEWAY_ENV_FILES,
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # サーバー設定
    host: str = Field("0.0.0.0", alias="GATEWAY_HOST")
    port: int = Field(8001, alias="GATEWAY_PORT")

    # 認証キー（必須、32バイト以上推奨）
    api_key: SecretStr = Field(..., alias="GATEWAY_API_KEY")

    # Webhook シークレット（必須、32バイト以上推奨）
    webhook_secret: SecretStr = Field(..., alias="GATEWAY_WEBHOOK_SECRET")

    # CORS設定
    cors_origins: str = Field("*", alias="CORS_ORIGINS")

    # ロギング
    log_level: str = Field("INFO", alias="LOG_LEVEL")

    # 開発用リロード（本番では false 推奨）
    reload: bool = Field(False, alias="GATEWAY_RELOAD")

    # FCM プロジェクト ID（オプション）
    fcm_project_id: str | None = Field(None, alias="FCM_PROJECT_ID")

    # FCM サービスアカウント JSON パス（省略時は固定パスを使用）
    fcm_credentials_path: str = Field(
        "gateway/firebase-service-account.json",
        alias="FCM_CREDENTIALS_PATH",
    )

    # デフォルトユーザー ID（MVP では固定）
    default_user_id: str = Field("default_user", alias="DEFAULT_USER_ID")

    # tmux セッション名の正規表現パターン
    session_pattern: str = Field(r"^agent-[0-9]{4}$", alias="SESSION_PATTERN")

    @classmethod
    def from_env(cls) -> "GatewayConfig":
        """環境変数から設定をロードします。

        Returns:
            設定済みのGatewayConfigインスタンス

        Raises:
            ValidationError: 必須の環境変数が不足している場合
        """
        config = cls()

        # ロギング設定
        logging.basicConfig(
            level=getattr(logging, config.log_level.upper()),
            format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        )

        return config
