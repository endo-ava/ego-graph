"""Spotify パイプラインの E2E 結合テスト。

MemoryS3 + responses モックを使用し、Collector → Transform → Storage → Compaction
の全データフローを検証する。
"""

from unittest.mock import patch

import responses
from pydantic import SecretStr

from pipelines.sources.common.config import (
    Config,
    DuckDBConfig,
    R2Config,
    SpotifyConfig,
)
from pipelines.sources.spotify.pipeline import (
    run_spotify_compact,
    run_spotify_ingest,
)
from pipelines.tests.e2e.test_browser_history_ingest import (
    _MemoryS3Server,
)
from pipelines.tests.fixtures.spotify_responses import get_mock_recently_played


def _build_config(memory_s3) -> Config:
    """Spotify pipeline 用の設定を構築する。"""
    r2 = R2Config(
        endpoint_url=memory_s3.endpoint_url,
        access_key_id="test-access-key",
        secret_access_key=SecretStr("test-secret-key"),
        bucket_name="test-bucket",
    )
    return Config(
        spotify=SpotifyConfig(
            client_id="test-client-id",
            client_secret=SecretStr("test-client-secret"),
            refresh_token=SecretStr("test-refresh-token"),
        ),
        duckdb=DuckDBConfig(r2=r2),
    )


def _mock_spotify_api():
    """Spotify API の必要エンドポイントをモックする。"""
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={
            "access_token": "mock-access-token",
            "expires_in": 3600,
            "token_type": "Bearer",
        },
        status=200,
    )
    # 現在月の played_at で返す (compaction が対象月を拾えるよう)
    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json={
            "items": [
                {
                    "track": {
                        "id": "3n3Ppam7vgaVa1iaRUc9Lp",
                        "name": "Mr. Brightside",
                        "artists": [
                            {"id": "0C0XlULifJtAgn6ZNCW2eu", "name": "The Killers"}
                        ],
                        "album": {"id": "4OHNH3sDzIxnmUADXzv2kT", "name": "Hot Fuss"},
                        "duration_ms": 222973,
                        "popularity": 85,
                    },
                    "played_at": "2026-04-01T02:30:00.000Z",
                    "context": {
                        "type": "playlist",
                        "uri": "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",
                    },
                },
                {
                    "track": {
                        "id": "0VjIjW4GlUZAMYd2vXMi3b",
                        "name": "Blinding Lights",
                        "artists": [
                            {"id": "1Xyo4u8uXC1ZmMpatF05PJ", "name": "The Weeknd"}
                        ],
                        "album": {
                            "id": "4yP0hdKOZPNshxUOjY0cZj",
                            "name": "After Hours",
                        },
                        "duration_ms": 200040,
                        "popularity": 92,
                    },
                    "played_at": "2026-04-01T02:26:00.000Z",
                    "context": {
                        "type": "album",
                        "uri": "spotify:album:4yP0hdKOZPNshxUOjY0cZj",
                    },
                },
            ]
        },
        status=200,
    )


@responses.activate
def test_spotify_ingest_to_compact_end_to_end():
    """Spotify 収集から compaction までの全フローが MemoryS3 上で完結する。"""
    with _MemoryS3Server() as memory_s3:
        config = _build_config(memory_s3)
        _mock_spotify_api()

        # enrich_master_data は実HTTP通信(Spotify API)を伴うためモック
        # (enrichment は integration テストで別途検証済み)
        with patch(
            "pipelines.sources.spotify.ingest_pipeline.enrich_master_data",
        ):
            # Act 1: ingest 実行 (Collector → Transform → Storage)
            ingest_result = run_spotify_ingest(config=config)

            # Assert 1: ingest 結果を検証
            assert ingest_result["provider"] == "spotify"
            assert ingest_result["operation"] == "ingest"
            assert ingest_result["status"] == "succeeded"

        # Act 2: compaction 実行 (S3読込 → 重複排除 → 書込)
        compact_result = run_spotify_compact(config=config)

        # Assert 2: compaction 結果を検証
        assert compact_result["provider"] == "spotify"
        assert compact_result["operation"] == "compact"
        assert len(compact_result["compacted_keys"]) > 0

        # Assert 3: MemoryS3 に期待されるオブジェクトが保存されている
        object_keys = {key for _, key in memory_s3.objects}

        # raw データ
        assert any(k.startswith("raw/spotify/recently_played/") for k in object_keys), (
            "raw Spotify data not found"
        )

        # events (plays)
        assert any(k.startswith("events/spotify/plays/year=") for k in object_keys), (
            "events parquet not found"
        )

        # compacted data
        assert any(
            k.startswith("compacted/events/spotify/plays/year=") for k in object_keys
        ), "compacted plays not found"

        # state
        assert any("state/spotify_ingest_state.json" in k for k in object_keys), (
            "ingest state not found"
        )
