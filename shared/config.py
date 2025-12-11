"""EgoGraphの設定管理。

環境変数をロードして検証し、全てのモジュールに対して
一元化された設定オブジェクトを提供します。
"""

import logging
import os
from typing import Optional

from pydantic import Field, field_validator, ValidationError
from pydantic_settings import BaseSettings, SettingsConfigDict


class SpotifyConfig(BaseSettings):
    """Spotify API設定。"""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    client_id: str = Field(..., alias="SPOTIFY_CLIENT_ID")
    client_secret: str = Field(..., alias="SPOTIFY_CLIENT_SECRET")
    refresh_token: str = Field(..., alias="SPOTIFY_REFRESH_TOKEN")
    redirect_uri: str = Field(
        "http://localhost:8888/callback",
        alias="SPOTIFY_REDIRECT_URI"
    )
    scope: str = Field(
        "user-read-recently-played playlist-read-private playlist-read-collaborative",
        alias="SPOTIFY_SCOPE"
    )


class EmbeddingConfig(BaseSettings):
    """埋め込みモデル設定(ローカル実行)。"""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    model_name: str = Field(
        "cl-nagoya/ruri-v3-310m",
        alias="EMBEDDING_MODEL_NAME"
    )
    batch_size: int = Field(32, alias="EMBEDDING_BATCH_SIZE")
    device: Optional[str] = Field(None, alias="EMBEDDING_DEVICE")
    expected_dimension: int = Field(1024, alias="EMBEDDING_DIMENSION")


class QdrantConfig(BaseSettings):
    """Qdrant Cloud設定。"""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    url: str = Field(..., alias="QDRANT_URL")
    api_key: str = Field(..., alias="QDRANT_API_KEY")
    collection_name: str = Field(
        "egograph_spotify_ruri",
        alias="QDRANT_COLLECTION_NAME"
    )
    vector_size: int = Field(1024, alias="QDRANT_VECTOR_SIZE")
    batch_size: int = Field(1000, alias="QDRANT_BATCH_SIZE")

    @field_validator("url")
    @classmethod
    def validate_url(cls, v: str) -> str:
        """URLがスラッシュで終わっていないことを確認します。"""
        return v.rstrip("/")


class Config(BaseSettings):
    """メイン設定オブジェクト。

    全てのサブ設定を集約し、グローバル設定を提供します。
    """

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    # ロギング
    log_level: str = Field("INFO", alias="LOG_LEVEL")

    # サブ設定
    spotify: Optional[SpotifyConfig] = None
    embedding: Optional[EmbeddingConfig] = None
    qdrant: Optional[QdrantConfig] = None

    @classmethod
    def from_env(cls) -> "Config":
        """環境変数から設定をロードします。

        Returns:
            設定済みのConfigインスタンス

        Raises:
            ValueError: 必須の環境変数が不足している場合
        """
        config = cls()

        # サブ設定のロード
        try:
            config.spotify = SpotifyConfig()
        except (ValidationError, ValueError) as e:
            logging.warning(f"Failed to load Spotify config: {e}")

        try:
            config.embedding = EmbeddingConfig()
        except (ValidationError, ValueError) as e:
            logging.warning(f"Failed to load Embedding config: {e}")

        try:
            config.qdrant = QdrantConfig()
        except (ValidationError, ValueError) as e:
            logging.warning(f"Failed to load Qdrant config: {e}")

        # ロギングの設定
        logging.basicConfig(
            level=getattr(logging, config.log_level.upper()),
            format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        )

        return config

    def validate_all(self) -> None:
        """全ての必須設定が存在することを検証します。

        Raises:
            ValueError: 必須設定が不足している場合
        """
        if not self.spotify:
            raise ValueError("Spotify configuration is required")
        if not self.embedding:
            raise ValueError("Embedding configuration is required")
        if not self.qdrant:
            raise ValueError("Qdrant configuration is required")
