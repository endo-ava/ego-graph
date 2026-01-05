"""Tools/Spotify/Stats層のテスト。"""

from unittest.mock import MagicMock, patch

import pytest

from backend.tools.spotify.stats import GetListeningStatsTool, GetTopTracksTool
from shared.config import R2Config


class TestGetTopTracksTool:
    """GetTopTracksToolのテスト。"""

    def test_name_property(self):
        """nameプロパティが正しい。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetTopTracksTool(r2_config)

        # Assert: nameプロパティを検証
        assert tool.name == "get_top_tracks"

    def test_description_property(self):
        """descriptionプロパティが正しい。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetTopTracksTool(r2_config)

        # Assert: descriptionプロパティを検証
        assert isinstance(tool.description, str)
        assert len(tool.description) > 0

    def test_input_schema_structure(self):
        """input_schemaが正しい構造を持つ。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetTopTracksTool(r2_config)

        # Act: input_schemaを取得
        schema = tool.input_schema

        # Assert: スキーマ構造を検証
        assert schema["type"] == "object"
        assert "start_date" in schema["properties"]
        assert "end_date" in schema["properties"]
        assert "limit" in schema["properties"]
        assert "start_date" in schema["required"]
        assert "end_date" in schema["required"]

    def test_to_schema_generates_tool(self):
        """to_schema()がToolスキーマを生成。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetTopTracksTool(r2_config)

        # Act: to_schema()でスキーマを生成
        schema = tool.to_schema()

        # Assert: 生成されたスキーマを検証
        assert schema.name == "get_top_tracks"
        assert isinstance(schema.description, str)
        assert isinstance(schema.inputSchema, dict)

    def test_execute_with_valid_dates(self):
        """正しい日付でexecute()を実行。"""
        # Arrange: モックDBと get_top_tracks関数のモックを準備
        mock_db = MagicMock()
        mock_conn = MagicMock()
        mock_db.__enter__.return_value = mock_conn

        # get_top_tracksのモック
        with patch(
            "backend.tools.spotify.stats.fetch_top_tracks",
            return_value=[
                {
                    "track_name": "Song A",
                    "artist": "Artist X",
                    "play_count": 10,
                    "total_minutes": 30.0,
                }
            ],
        ) as mock_get_top_tracks:
            r2_config = R2Config.model_construct(
                bucket_name="bucket", events_path="events/"
            )
            tool = GetTopTracksTool(r2_config, db_connection_factory=lambda: mock_db)

            # Act: ツールを実行
            result = tool.execute(
                start_date="2024-01-01", end_date="2024-01-31", limit=10
            )

            # Assert: 実行結果と関数呼び出しを検証
            assert len(result) == 1
            assert result[0]["track_name"] == "Song A"

            # get_top_tracksが正しい引数で呼ばれたことを確認
            mock_get_top_tracks.assert_called_once()

    def test_execute_with_invalid_date_format_raises_error(self):
        """不正な日付形式でエラー。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetTopTracksTool(r2_config)

        # Act & Assert: 不正な日付形式でValueErrorが発生することを検証
        with pytest.raises(ValueError, match="invalid_start_date"):
            tool.execute(start_date="invalid-date", end_date="2024-01-31")

    def test_execute_with_default_limit(self):
        """limitのデフォルト値で実行。"""
        # Arrange: モックDBとget_top_tracks関数のモックを準備
        mock_db = MagicMock()
        mock_conn = MagicMock()
        mock_db.__enter__.return_value = mock_conn

        with patch(
            "backend.tools.spotify.stats.fetch_top_tracks", return_value=[]
        ) as mock_get_top_tracks:
            r2_config = R2Config.model_construct(
                bucket_name="bucket", events_path="events/"
            )
            tool = GetTopTracksTool(r2_config, db_connection_factory=lambda: mock_db)

            # Act: limitパラメータを省略して実行
            tool.execute(start_date="2024-01-01", end_date="2024-01-31")

            # Assert: デフォルトのlimit=10で呼ばれることを検証
            call_args = mock_get_top_tracks.call_args
            assert call_args[0][4] == 10  # 5番目の引数がlimit


class TestGetListeningStatsTool:
    """GetListeningStatsToolのテスト。"""

    def test_name_property(self):
        """nameプロパティが正しい。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetListeningStatsTool(r2_config)

        # Assert: nameプロパティを検証
        assert tool.name == "get_listening_stats"

    def test_description_property(self):
        """descriptionプロパティが正しい。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetListeningStatsTool(r2_config)

        # Assert: descriptionプロパティを検証
        assert isinstance(tool.description, str)
        assert len(tool.description) > 0

    def test_input_schema_structure(self):
        """input_schemaが正しい構造を持つ。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetListeningStatsTool(r2_config)

        # Act: input_schemaを取得
        schema = tool.input_schema

        # Assert: スキーマ構造を検証
        assert schema["type"] == "object"
        assert "start_date" in schema["properties"]
        assert "end_date" in schema["properties"]
        assert "granularity" in schema["properties"]
        assert schema["properties"]["granularity"]["enum"] == ["day", "week", "month"]

    def test_to_schema_generates_tool(self):
        """to_schema()がToolスキーマを生成。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetListeningStatsTool(r2_config)

        # Act: to_schema()でスキーマを生成
        schema = tool.to_schema()

        # Assert: 生成されたスキーマを検証
        assert schema.name == "get_listening_stats"
        assert isinstance(schema.description, str)
        assert isinstance(schema.inputSchema, dict)

    def test_execute_with_valid_parameters(self):
        """正しいパラメータでexecute()を実行。"""
        # Arrange: モックDBとget_listening_stats関数のモックを準備
        mock_db = MagicMock()
        mock_conn = MagicMock()
        mock_db.__enter__.return_value = mock_conn

        with patch(
            "backend.tools.spotify.stats.fetch_listening_stats",
            return_value=[
                {
                    "period": "2024-01-01",
                    "total_ms": 3600000,
                    "track_count": 20,
                    "unique_tracks": 15,
                }
            ],
        ) as mock_get_listening_stats:
            r2_config = R2Config.model_construct(
                bucket_name="bucket", events_path="events/"
            )
            tool = GetListeningStatsTool(
                r2_config, db_connection_factory=lambda: mock_db
            )

            # Act: ツールを実行
            result = tool.execute(
                start_date="2024-01-01", end_date="2024-01-31", granularity="day"
            )

            # Assert: 実行結果と関数呼び出しを検証
            assert len(result) == 1
            assert result[0]["period"] == "2024-01-01"

            # get_listening_statsが正しい引数で呼ばれたことを確認
            mock_get_listening_stats.assert_called_once()

    def test_execute_with_invalid_date_format_raises_error(self):
        """不正な日付形式でエラー。"""
        # Arrange: ツールを準備
        r2_config = R2Config.model_construct(
            bucket_name="bucket", events_path="events/"
        )
        tool = GetListeningStatsTool(r2_config)

        # Act & Assert: 不正な日付形式でValueErrorが発生することを検証
        with pytest.raises(ValueError, match="invalid_start_date"):
            tool.execute(
                start_date="invalid-date", end_date="2024-01-31", granularity="day"
            )

    def test_execute_with_default_granularity(self):
        """granularityのデフォルト値で実行。"""
        # Arrange: モックDBとget_listening_stats関数のモックを準備
        mock_db = MagicMock()
        mock_conn = MagicMock()
        mock_db.__enter__.return_value = mock_conn

        with patch(
            "backend.tools.spotify.stats.fetch_listening_stats", return_value=[]
        ) as mock_get_listening_stats:
            r2_config = R2Config.model_construct(
                bucket_name="bucket", events_path="events/"
            )
            tool = GetListeningStatsTool(
                r2_config, db_connection_factory=lambda: mock_db
            )

            # Act: granularityパラメータを省略して実行
            tool.execute(start_date="2024-01-01", end_date="2024-01-31")

            # Assert: デフォルトのgranularity="day"で呼ばれることを検証
            call_args = mock_get_listening_stats.call_args
            assert call_args[0][4] == "day"  # 5番目の引数がgranularity
