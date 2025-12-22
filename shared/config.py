"""EgoGraphの設定管理。

環境変数をロードして検証し、全てのモジュールに対して
一元化された設定オブジェクトを提供します。
"""

import logging
from typing import Optional

from pydantic import Field, SecretStr, ValidationError, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class LastFmConfig(BaseSettings):
    """Last.fm API設定。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    api_key: str = Field(..., alias="LASTFM_API_KEY")
    api_secret: SecretStr = Field(..., alias="LASTFM_API_SECRET")


class SpotifyConfig(BaseSettings):
    """Spotify API設定。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    client_id: str = Field(..., alias="SPOTIFY_CLIENT_ID")
    client_secret: SecretStr = Field(..., alias="SPOTIFY_CLIENT_SECRET")
    refresh_token: SecretStr = Field(..., alias="SPOTIFY_REFRESH_TOKEN")
    redirect_uri: str = Field(
        "http://127.0.0.1:8888/callback", alias="SPOTIFY_REDIRECT_URI"
    )
    scope: str = Field(
        "user-read-recently-played playlist-read-private playlist-read-collaborative",
        alias="SPOTIFY_SCOPE",
    )


class EmbeddingConfig(BaseSettings):
    """埋め込みモデル設定(ローカル実行)。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    model_name: str = Field("cl-nagoya/ruri-v3-310m", alias="EMBEDDING_MODEL_NAME")
    batch_size: int = Field(32, alias="EMBEDDING_BATCH_SIZE")
    device: Optional[str] = Field(None, alias="EMBEDDING_DEVICE")
    expected_dimension: int = Field(768, alias="EMBEDDING_DIMENSION")


class QdrantConfig(BaseSettings):
    """Qdrant Cloud設定。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    url: str = Field(..., alias="QDRANT_URL")
    api_key: SecretStr = Field(..., alias="QDRANT_API_KEY")
    collection_name: str = Field(
        "egograph_spotify_ruri", alias="QDRANT_COLLECTION_NAME"
    )
    vector_size: int = Field(768, alias="QDRANT_VECTOR_SIZE")
    batch_size: int = Field(1000, alias="QDRANT_BATCH_SIZE")

    @field_validator("url")
    @classmethod
    def validate_url(cls, v: str) -> str:
        """URLがスラッシュで終わっていないことを確認します。"""
        return v.rstrip("/")


class R2Config(BaseSettings):
    """Cloudflare R2設定 (S3互換)。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    endpoint_url: str = Field(..., alias="R2_ENDPOINT_URL")
    access_key_id: str = Field(..., alias="R2_ACCESS_KEY_ID")
    secret_access_key: SecretStr = Field(..., alias="R2_SECRET_ACCESS_KEY")
    bucket_name: str = Field("egograph", alias="R2_BUCKET_NAME")
    raw_path: str = Field("raw/", alias="R2_RAW_PATH")
    events_path: str = Field("events/", alias="R2_EVENTS_PATH")


class DuckDBConfig(BaseSettings):
    """DuckDB設定。"""

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    db_path: str = Field("data/analytics.duckdb", alias="DUCKDB_PATH")
    r2: Optional[R2Config] = None


class Config(BaseSettings):
    """メイン設定オブジェクト。

    全てのサブ設定を集約し、グローバル設定を提供します。
    """

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    # ロギング
    log_level: str = Field("INFO", alias="LOG_LEVEL")

    # サブ設定
    spotify: Optional[SpotifyConfig] = None
    lastfm: Optional[LastFmConfig] = None
    embedding: Optional[EmbeddingConfig] = None
    qdrant: Optional[QdrantConfig] = None
    duckdb: Optional[DuckDBConfig] = None

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
        except (ValidationError, ValueError):
            logging.exception("Failed to load Spotify config")

        try:
            config.embedding = EmbeddingConfig()
        except (ValidationError, ValueError):
            logging.exception("Failed to load Embedding config")

        try:
            config.lastfm = LastFmConfig()
        except (ValidationError, ValueError):
            logging.info("Last.fm config not available")

        try:
            config.qdrant = QdrantConfig()
        except (ValidationError, ValueError):
            logging.info(
                "Qdrant config not available, vector search features will be disabled"
            )

        try:
            r2_config = None
            try:
                r2_config = R2Config()
            except (ValidationError, ValueError):
                logging.info(
                    "R2 config not available, DuckDB will run in local-only mode"
                )
            config.duckdb = DuckDBConfig(r2=r2_config)
        except (ValidationError, ValueError):
            logging.exception("Failed to load DuckDB config")

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
