"""YouTube API/Data統合テスト。"""

from unittest.mock import patch

from backend.infrastructure.repositories.youtube_repository import YouTubeRepository


class TestWatchHistoryEndpoint:
    """Watch Historyエンドポイントのテスト。"""

    def test_get_watch_history_success(self, test_client, mock_db_and_parquet):
        """視聴履歴を取得できる。"""
        mock_result = [
            {
                "watch_id": "watch_1",
                "watched_at_utc": "2024-01-01 10:00:00",
                "video_id": "video_1",
                "video_title": "Video A",
                "channel_id": "channel_1",
                "channel_name": "Channel X",
                "duration_seconds": 600,
                "video_url": "https://youtube.com/watch?v=video_1",
            }
        ]

        with patch.object(
            YouTubeRepository, "get_watch_history", return_value=mock_result
        ):
            response = test_client.get(
                "/v1/data/youtube/history?start_date=2024-01-01&end_date=2024-01-03&limit=5",
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert isinstance(data, list)
            assert len(data) > 0
            # 最初の履歴の構造を確認
            assert "watch_id" in data[0]
            assert "watched_at_utc" in data[0]
            assert "video_id" in data[0]
            assert "video_title" in data[0]
            assert "channel_id" in data[0]
            assert "channel_name" in data[0]
            assert "duration_seconds" in data[0]
            assert "video_url" in data[0]

    def test_get_watch_history_requires_api_key(self, test_client):
        """API Keyが必要。"""
        response = test_client.get(
            "/v1/data/youtube/history?start_date=2024-01-01&end_date=2024-01-03&limit=5"
        )

        assert response.status_code == 401

    def test_get_watch_history_validates_limit(self, test_client):
        """limitの範囲バリデーション。"""
        # limit > 100
        response = test_client.get(
            "/v1/data/youtube/history?start_date=2024-01-01&end_date=2024-01-03&limit=101",
            headers={"X-API-Key": "test-backend-key"},
        )
        assert response.status_code == 422

        # limit < 1
        response = test_client.get(
            "/v1/data/youtube/history?start_date=2024-01-01&end_date=2024-01-03&limit=0",
            headers={"X-API-Key": "test-backend-key"},
        )
        assert response.status_code == 422

    def test_get_watch_history_requires_dates(self, test_client):
        """start_date/end_dateが必須。"""
        response = test_client.get(
            "/v1/data/youtube/history?limit=5",
            headers={"X-API-Key": "test-backend-key"},
        )

        assert response.status_code == 422


class TestWatchingStatsEndpoint:
    """Watching Statsエンドポイントのテスト。"""

    def test_get_watching_stats_success(self, test_client, mock_db_and_parquet):
        """視聴統計を取得できる。"""
        mock_result = [
            {
                "period": "2024-01-01",
                "total_seconds": 3600,
                "video_count": 20,
                "unique_videos": 15,
            }
        ]

        with patch.object(
            YouTubeRepository, "get_watching_stats", return_value=mock_result
        ):
            response = test_client.get(
                "/v1/data/youtube/stats/watching?start_date=2024-01-01&end_date=2024-01-03&granularity=day",
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert isinstance(data, list)
            assert len(data) > 0
            # 最初の統計の構造を確認
            assert "period" in data[0]
            assert "total_seconds" in data[0]
            assert "video_count" in data[0]
            assert "unique_videos" in data[0]

    def test_get_watching_stats_requires_api_key(self, test_client):
        """API Keyが必要。"""
        response = test_client.get(
            "/v1/data/youtube/stats/watching?start_date=2024-01-01&end_date=2024-01-03&granularity=day"
        )

        assert response.status_code == 401

    def test_get_watching_stats_validates_granularity(self, test_client):
        """granularityのバリデーション。"""
        response = test_client.get(
            "/v1/data/youtube/stats/watching?start_date=2024-01-01&end_date=2024-01-03&granularity=invalid",
            headers={"X-API-Key": "test-backend-key"},
        )

        assert response.status_code == 422

    def test_get_watching_stats_default_granularity(
        self, test_client, mock_db_and_parquet
    ):
        """granularityのデフォルト値は"day"。"""
        mock_result = [
            {
                "period": "2024-01-01",
                "total_seconds": 1000,
                "video_count": 5,
                "unique_videos": 3,
            }
        ]

        with patch.object(
            YouTubeRepository, "get_watching_stats", return_value=mock_result
        ):
            # granularity省略
            response = test_client.get(
                "/v1/data/youtube/stats/watching?start_date=2024-01-01&end_date=2024-01-03",
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200


class TestTopChannelsEndpoint:
    """Top Channelsエンドポイントのテスト。"""

    def test_get_top_channels_success(self, test_client, mock_db_and_parquet):
        """トップチャンネルを取得できる。"""
        mock_result = [
            {
                "channel_id": "channel_1",
                "channel_name": "Channel X",
                "video_count": 50,
                "total_seconds": 18000,
            }
        ]

        with patch.object(
            YouTubeRepository, "get_top_channels", return_value=mock_result
        ):
            response = test_client.get(
                "/v1/data/youtube/stats/top-channels?start_date=2024-01-01&end_date=2024-01-03&limit=5",
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert isinstance(data, list)
            assert len(data) > 0
            # 最初のチャンネルの構造を確認
            assert "channel_id" in data[0]
            assert "channel_name" in data[0]
            assert "video_count" in data[0]
            assert "total_seconds" in data[0]

    def test_get_top_channels_requires_api_key(self, test_client):
        """API Keyが必要。"""
        response = test_client.get(
            "/v1/data/youtube/stats/top-channels?start_date=2024-01-01&end_date=2024-01-03&limit=5"
        )

        assert response.status_code == 401

    def test_get_top_channels_validates_limit(self, test_client):
        """limitの範囲バリデーション。"""
        # limit > 100
        response = test_client.get(
            "/v1/data/youtube/stats/top-channels?start_date=2024-01-01&end_date=2024-01-03&limit=101",
            headers={"X-API-Key": "test-backend-key"},
        )
        assert response.status_code == 422

        # limit < 1
        response = test_client.get(
            "/v1/data/youtube/stats/top-channels?start_date=2024-01-01&end_date=2024-01-03&limit=0",
            headers={"X-API-Key": "test-backend-key"},
        )
        assert response.status_code == 422

    def test_get_top_channels_requires_dates(self, test_client):
        """start_date/end_dateが必須。"""
        response = test_client.get(
            "/v1/data/youtube/stats/top-channels?limit=5",
            headers={"X-API-Key": "test-backend-key"},
        )

        assert response.status_code == 422
