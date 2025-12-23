"""API/Data統合テスト。"""

import pytest
from unittest.mock import patch, MagicMock


class TestTopTracksEndpoint:
    """Top Tracksエンドポイントのテスト。"""

    def test_get_top_tracks_success(self, test_client):
        """トップトラックを取得できる。"""
        mock_result = [
            {
                "track_name": "Song A",
                "artist": "Artist X",
                "play_count": 10,
                "total_minutes": 30.5,
            }
        ]

        with patch("backend.api.data.get_db_connection") as mock_get_db, patch(
            "backend.api.data.get_parquet_path",
            return_value="s3://test-bucket/events/spotify/plays/**/*.parquet",
        ), patch(
            "backend.api.data.get_top_tracks", return_value=mock_result
        ):
            mock_conn = MagicMock()
            mock_get_db.return_value.__enter__.return_value = mock_conn
            mock_get_db.return_value.__exit__.return_value = False

            response = test_client.get(
                "/v1/data/spotify/stats/top-tracks?start_date=2024-01-01&end_date=2024-01-03&limit=5",
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert isinstance(data, list)
            assert len(data) > 0
            # 最初のトラックの構造を確認
            assert "track_name" in data[0]
            assert "artist" in data[0]
            assert "play_count" in data[0]
            assert "total_minutes" in data[0]

    def test_get_top_tracks_requires_api_key(self, test_client):
        """API Keyが必要。"""
        response = test_client.get(
            "/v1/data/spotify/stats/top-tracks?start_date=2024-01-01&end_date=2024-01-03&limit=5"
        )

        assert response.status_code == 401

    def test_get_top_tracks_validates_limit(self, test_client):
        """limitの範囲バリデーション。"""
        # limit > 100
        response = test_client.get(
            "/v1/data/spotify/stats/top-tracks?start_date=2024-01-01&end_date=2024-01-03&limit=101",
            headers={"X-API-Key": "test-backend-key"},
        )
        assert response.status_code == 422

        # limit < 1
        response = test_client.get(
            "/v1/data/spotify/stats/top-tracks?start_date=2024-01-01&end_date=2024-01-03&limit=0",
            headers={"X-API-Key": "test-backend-key"},
        )
        assert response.status_code == 422

    def test_get_top_tracks_requires_dates(self, test_client):
        """start_date/end_dateが必須。"""
        response = test_client.get(
            "/v1/data/spotify/stats/top-tracks?limit=5",
            headers={"X-API-Key": "test-backend-key"},
        )

        assert response.status_code == 422


class TestListeningStatsEndpoint:
    """Listening Statsエンドポイントのテスト。"""

    def test_get_listening_stats_success(self, test_client):
        """視聴統計を取得できる。"""
        mock_result = [
            {
                "period": "2024-01-01",
                "total_ms": 3600000,
                "track_count": 20,
                "unique_tracks": 15,
            }
        ]

        with patch("backend.api.data.get_db_connection") as mock_get_db, patch(
            "backend.api.data.get_parquet_path",
            return_value="s3://test-bucket/events/spotify/plays/**/*.parquet",
        ), patch(
            "backend.api.data.get_listening_stats", return_value=mock_result
        ):
            mock_conn = MagicMock()
            mock_get_db.return_value.__enter__.return_value = mock_conn
            mock_get_db.return_value.__exit__.return_value = False

            response = test_client.get(
                "/v1/data/spotify/stats/listening?start_date=2024-01-01&end_date=2024-01-03&granularity=day",
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert isinstance(data, list)
            assert len(data) > 0
            # 最初の統計の構造を確認
            assert "period" in data[0]
            assert "total_ms" in data[0]
            assert "track_count" in data[0]
            assert "unique_tracks" in data[0]

    def test_get_listening_stats_requires_api_key(self, test_client):
        """API Keyが必要。"""
        response = test_client.get(
            "/v1/data/spotify/stats/listening?start_date=2024-01-01&end_date=2024-01-03&granularity=day"
        )

        assert response.status_code == 401

    def test_get_listening_stats_validates_granularity(self, test_client):
        """granularityのバリデーション。"""
        response = test_client.get(
            "/v1/data/spotify/stats/listening?start_date=2024-01-01&end_date=2024-01-03&granularity=invalid",
            headers={"X-API-Key": "test-backend-key"},
        )

        assert response.status_code == 422

    def test_get_listening_stats_default_granularity(self, test_client):
        """granularityのデフォルト値は"day"。"""
        mock_result = [{"period": "2024-01-01", "total_ms": 1000, "track_count": 5, "unique_tracks": 3}]

        with patch("backend.api.data.get_db_connection") as mock_get_db, patch(
            "backend.api.data.get_parquet_path",
            return_value="s3://test-bucket/events/spotify/plays/**/*.parquet",
        ), patch(
            "backend.api.data.get_listening_stats", return_value=mock_result
        ):
            mock_conn = MagicMock()
            mock_get_db.return_value.__enter__.return_value = mock_conn
            mock_get_db.return_value.__exit__.return_value = False

            # granularity省略
            response = test_client.get(
                "/v1/data/spotify/stats/listening?start_date=2024-01-01&end_date=2024-01-03",
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
