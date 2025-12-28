"""Backend テスト用の共有 pytest フィクスチャ。"""

from io import BytesIO
from unittest.mock import MagicMock, patch

import duckdb
import pandas as pd
import pytest
from fastapi.testclient import TestClient
from pydantic import SecretStr

from backend.api import deps
from backend.config import BackendConfig, LLMConfig
from backend.main import create_app
from shared.config import R2Config

# ========================================
# 環境変数クリア（テスト用）
# ========================================


@pytest.fixture(autouse=True)
def clear_env_vars(monkeypatch):
    """テスト用に環境変数をクリア。

    Pydantic Settingsが環境変数を読み取らないようにする。
    """
    # R2関連
    monkeypatch.delenv("R2_ENDPOINT_URL", raising=False)
    monkeypatch.delenv("R2_ACCESS_KEY_ID", raising=False)
    monkeypatch.delenv("R2_SECRET_ACCESS_KEY", raising=False)
    monkeypatch.delenv("R2_BUCKET_NAME", raising=False)
    monkeypatch.delenv("R2_RAW_PATH", raising=False)
    monkeypatch.delenv("R2_EVENTS_PATH", raising=False)

    # LLM関連
    monkeypatch.delenv("LLM_PROVIDER", raising=False)
    monkeypatch.delenv("LLM_API_KEY", raising=False)
    monkeypatch.delenv("LLM_MODEL_NAME", raising=False)
    monkeypatch.delenv("LLM_TEMPERATURE", raising=False)
    monkeypatch.delenv("LLM_MAX_TOKENS", raising=False)

    # Backend関連
    monkeypatch.delenv("BACKEND_HOST", raising=False)
    monkeypatch.delenv("BACKEND_PORT", raising=False)
    monkeypatch.delenv("BACKEND_RELOAD", raising=False)
    monkeypatch.delenv("BACKEND_API_KEY", raising=False)
    monkeypatch.delenv("LOG_LEVEL", raising=False)


# ========================================
# 設定フィクスチャ
# ========================================


@pytest.fixture
def mock_r2_config():
    """モックR2設定。"""

    # model_construct()を使って検証をスキップして直接構築
    return R2Config.model_construct(
        endpoint_url="https://test.r2.cloudflarestorage.com",
        access_key_id="test_key",
        secret_access_key=SecretStr("test_secret"),  # SecretStrでラップ
        bucket_name="test-bucket",
        raw_path="raw/",
        events_path="events/",
    )


@pytest.fixture
def mock_llm_config():
    """モックLLM設定。"""

    # model_construct()を使って検証をスキップして直接構築
    return LLMConfig.model_construct(
        provider="openai",
        api_key=SecretStr("test-api-key"),  # SecretStrでラップ
        model_name="gpt-4o-mini",
        temperature=0.7,
        max_tokens=2048,
    )


@pytest.fixture
def mock_backend_config(mock_r2_config, mock_llm_config):
    """モックBackend設定。"""

    # model_construct()を使って検証をスキップして直接構築
    config = BackendConfig.model_construct(
        host="127.0.0.1",
        port=8000,
        reload=False,
        api_key=SecretStr("test-backend-key"),  # SecretStrでラップ
        cors_origins="http://localhost:3000",  # ワイルドカードを避ける
        log_level="DEBUG",
    )
    config.r2 = mock_r2_config
    config.llm = mock_llm_config
    return config


# ========================================
# DuckDB フィクスチャ（実DuckDB使用）
# ========================================


@pytest.fixture
def duckdb_conn():
    """実DuckDB（:memory:）接続。"""
    conn = duckdb.connect(":memory:")
    yield conn
    conn.close()


class DuckDBConnectionWrapper:
    """DuckDB接続のラッパー（テスト用の属性を保持）。"""

    def __init__(self, conn, parquet_path):
        self._conn = conn
        self.test_parquet_path = parquet_path

    def __getattr__(self, name):
        """属性アクセスを内部の接続オブジェクトに委譲。"""
        return getattr(self._conn, name)


@pytest.fixture
def duckdb_with_sample_data(duckdb_conn, tmp_path):
    """サンプルParquetデータを持つDuckDB。"""
    # サンプルデータ作成
    sample_data = pd.DataFrame(
        {
            "played_at_utc": pd.to_datetime(
                [
                    "2024-01-01 10:00:00",
                    "2024-01-01 11:00:00",
                    "2024-01-02 10:00:00",
                    "2024-01-02 11:00:00",
                    "2024-01-03 10:00:00",
                ]
            ),
            "track_id": ["track_1", "track_2", "track_1", "track_3", "track_1"],
            "track_name": ["Song A", "Song B", "Song A", "Song C", "Song A"],
            "artist_names": [
                ["Artist X"],
                ["Artist Y"],
                ["Artist X"],
                ["Artist Z"],
                ["Artist X"],
            ],
            "album_name": ["Album 1", "Album 2", "Album 1", "Album 3", "Album 1"],
            "ms_played": [180000, 200000, 180000, 150000, 180000],
        }
    )

    # Parquetファイルとして保存
    parquet_path = tmp_path / "test_data.parquet"
    sample_data.to_parquet(parquet_path)

    # DuckDBにDataFrameを直接登録（下位互換性のため）
    duckdb_conn.register("sample_data_df", sample_data)
    duckdb_conn.execute("CREATE TABLE spotify_plays AS SELECT * FROM sample_data_df")
    duckdb_conn.unregister("sample_data_df")

    # ラッパーオブジェクトを作成
    wrapper = DuckDBConnectionWrapper(duckdb_conn, str(parquet_path))

    yield wrapper


# ========================================
# R2（S3）モックフィクスチャ
# ========================================


@pytest.fixture
def mock_boto3_client():
    """モックboto3 S3クライアント。"""
    with patch("boto3.client") as mock_client:
        s3 = MagicMock()
        mock_client.return_value = s3

        # デフォルト動作設定
        s3.put_object.return_value = {"ResponseMetadata": {"HTTPStatusCode": 200}}
        s3.get_object.return_value = {
            "Body": BytesIO(b'{"cursor": 123456789}'),
            "ContentType": "application/json",
        }

        yield s3


# ========================================
# LLM API モックフィクスチャ
# ========================================


@pytest.fixture
def mock_httpx_client():
    """モックhttpxクライアント（LLM API用）。"""
    with patch("httpx.AsyncClient") as mock_client:
        client_instance = MagicMock()
        mock_client.return_value.__aenter__.return_value = client_instance

        # デフォルトレスポンス設定
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "id": "chatcmpl-test-123",
            "choices": [
                {
                    "message": {
                        "role": "assistant",
                        "content": "This is a test response.",
                    }
                }
            ],
            "usage": {"prompt_tokens": 10, "completion_tokens": 20},
        }
        client_instance.post.return_value = mock_response

        yield client_instance


# ========================================
# FastAPI テストクライアント
# ========================================


@pytest.fixture
def test_client(mock_backend_config):
    """FastAPI TestClient。"""

    # テスト用の設定でアプリを作成
    app = create_app(config=mock_backend_config)

    # 依存性オーバーライド用
    app.dependency_overrides[deps.get_config] = lambda: mock_backend_config

    with TestClient(app) as client:
        yield client

    app.dependency_overrides.clear()


# ========================================
# API Data テスト用の共通モック
# ========================================


@pytest.fixture
def mock_db_and_parquet():
    """データAPIテスト用のDB接続とParquetパスのモック。"""
    with patch("backend.api.data.get_db_connection") as mock_get_db, patch(
        "backend.api.data.get_parquet_path",
        return_value="s3://test-bucket/events/spotify/plays/**/*.parquet",
    ):
        mock_conn = MagicMock()
        mock_get_db.return_value.__enter__.return_value = mock_conn
        mock_get_db.return_value.__exit__.return_value = False

        yield {"mock_get_db": mock_get_db, "mock_conn": mock_conn}
