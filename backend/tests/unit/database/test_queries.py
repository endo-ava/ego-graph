"""Database/Queries層のテスト。"""

from datetime import date

import pytest

from backend.database.queries import (
    execute_query,
    get_listening_stats,
    get_parquet_path,
    get_top_tracks,
    search_tracks_by_name,
)


class TestGetParquetPath:
    """get_parquet_path のテスト。"""

    def test_generates_correct_path(self):
        """正しいS3パスパターンを生成。"""
        # Arrange: バケット名とプレフィックスを準備
        bucket = "my-bucket"
        prefix = "events/"

        # Act: S3パスを生成
        path = get_parquet_path(bucket, prefix)

        # Assert: 正しいパスパターンが生成されることを検証
        assert path == "s3://my-bucket/events/spotify/plays/**/*.parquet"

    def test_handles_different_bucket(self):
        """異なるバケット名で正しく生成。"""
        # Arrange: 異なるバケット名とプレフィックスを準備
        bucket = "test-bucket"
        prefix = "data/"

        # Act: S3パスを生成
        path = get_parquet_path(bucket, prefix)

        # Assert: 正しいパスパターンが生成されることを検証
        assert path == "s3://test-bucket/data/spotify/plays/**/*.parquet"


class TestExecuteQuery:
    """execute_query のテスト。"""

    def test_executes_simple_query(self, duckdb_conn):
        """シンプルなクエリを実行。"""
        # Arrange: DuckDB接続を準備（fixtureから提供）

        # Act: シンプルなSELECTクエリを実行
        result = execute_query(duckdb_conn, "SELECT 1 as value")

        # Assert: 結果が正しいことを検証
        assert len(result) == 1
        assert result[0]["value"] == 1

    def test_executes_query_with_params(self, duckdb_conn):
        """パラメータ付きクエリを実行。"""
        # Arrange: DuckDB接続を準備（fixtureから提供）

        # Act: パラメータを使用してクエリを実行
        result = execute_query(duckdb_conn, "SELECT ? as num", [42])

        # Assert: パラメータが正しく適用されることを検証
        assert result[0]["num"] == 42

    def test_returns_empty_list_for_no_results(self, duckdb_conn):
        """結果がない場合は空リストを返す。"""
        # Arrange: 空のテーブルを作成
        duckdb_conn.execute("CREATE TABLE empty_table (id INT)")

        # Act: 空のテーブルからSELECT
        result = execute_query(duckdb_conn, "SELECT * FROM empty_table")

        # Assert: 空リストが返されることを検証
        assert result == []

    def test_returns_list_of_dicts(self, duckdb_conn):
        """結果を辞書のリストで返す。"""
        # Arrange: テストテーブルを作成してデータを挿入
        duckdb_conn.execute("CREATE TABLE test_table (id INT, name VARCHAR)")
        duckdb_conn.execute("INSERT INTO test_table VALUES (1, 'Alice'), (2, 'Bob')")

        # Act: テーブルからデータを取得
        result = execute_query(duckdb_conn, "SELECT * FROM test_table ORDER BY id")

        # Assert: 辞書のリストとして正しく返されることを検証
        assert len(result) == 2
        assert result[0] == {"id": 1, "name": "Alice"}
        assert result[1] == {"id": 2, "name": "Bob"}


class TestGetTopTracks:
    """get_top_tracks のテスト。"""

    def test_returns_top_tracks(self, duckdb_with_sample_data):
        """トップトラックを取得。"""
        # Arrange: get_top_tracksを使用してトップトラックを取得
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: get_top_tracks関数を直接呼び出す
        result = get_top_tracks(
            duckdb_with_sample_data,
            parquet_path,
            start_date=date(2024, 1, 1),
            end_date=date(2024, 1, 3),
            limit=5,
        )

        # Assert: トップトラックが正しく取得されることを検証
        assert len(result) > 0
        # "Song A" (track_1) が3回再生されているので1位
        assert result[0]["track_name"] == "Song A"
        assert result[0]["play_count"] == 3
        assert "total_minutes" in result[0]

    def test_respects_limit_parameter(self, duckdb_with_sample_data):
        """limitパラメータを尊重。"""
        # Arrange: get_top_tracksを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: limit=2でトップトラックを取得
        result = get_top_tracks(
            duckdb_with_sample_data,
            parquet_path,
            start_date=date(2024, 1, 1),
            end_date=date(2024, 1, 3),
            limit=2,
        )

        # Assert: 最大2件までしか返されないことを検証
        assert len(result) <= 2

    def test_filters_by_date_range(self, duckdb_with_sample_data):
        """日付範囲でフィルタリング。"""
        # Arrange: get_top_tracksを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: 2024-01-01のデータのみ取得
        result = get_top_tracks(
            duckdb_with_sample_data,
            parquet_path,
            start_date=date(2024, 1, 1),
            end_date=date(2024, 1, 1),
            limit=10,
        )

        # Assert: 2024-01-01には2件のレコードがあることを検証
        assert len(result) == 2


class TestGetListeningStats:
    """get_listening_stats のテスト。"""

    def test_aggregates_by_day(self, duckdb_with_sample_data):
        """日単位で集計。"""
        # Arrange: get_listening_statsを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: 日単位で統計情報を取得
        result = get_listening_stats(
            duckdb_with_sample_data,
            parquet_path,
            start_date=date(2024, 1, 1),
            end_date=date(2024, 1, 3),
            granularity="day",
        )

        # Assert: 3日分のデータが正しく集計されることを検証
        assert len(result) == 3  # 3日分
        assert result[0]["period"] == "2024-01-01"
        assert result[0]["track_count"] == 2

    def test_aggregates_by_month(self, duckdb_with_sample_data):
        """月単位で集計。"""
        # Arrange: get_listening_statsを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: 月単位で統計情報を取得
        result = get_listening_stats(
            duckdb_with_sample_data,
            parquet_path,
            start_date=date(2024, 1, 1),
            end_date=date(2024, 1, 3),
            granularity="month",
        )

        # Assert: 1ヶ月分のデータが正しく集計されることを検証
        assert len(result) == 1  # 1ヶ月分
        assert result[0]["period"] == "2024-01"
        assert result[0]["track_count"] == 5  # 全5件

    def test_invalid_granularity_raises_error(self, duckdb_with_sample_data):
        """無効な粒度でエラー発生。"""
        # Arrange: get_listening_statsを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act & Assert: 無効なgranularityでValueErrorが発生することを検証
        with pytest.raises(ValueError, match="Invalid granularity"):
            get_listening_stats(
                duckdb_with_sample_data,
                parquet_path,
                start_date=date(2024, 1, 1),
                end_date=date(2024, 1, 3),
                granularity="invalid",
            )


class TestSearchTracksByName:
    """search_tracks_by_name のテスト。"""

    def test_searches_by_track_name(self, duckdb_with_sample_data):
        """トラック名で検索。"""
        # Arrange: search_tracks_by_nameを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: トラック名で検索
        result = search_tracks_by_name(
            duckdb_with_sample_data, parquet_path, query="Song A", limit=20
        )

        # Assert: "Song A"が見つかることを検証
        assert len(result) > 0
        assert result[0]["track_name"] == "Song A"

    def test_searches_by_artist_name(self, duckdb_with_sample_data):
        """アーティスト名で検索。"""
        # Arrange: search_tracks_by_nameを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: アーティスト名で検索
        result = search_tracks_by_name(
            duckdb_with_sample_data, parquet_path, query="Artist X", limit=20
        )

        # Assert: Artist XはSong Aなので見つかることを検証
        assert len(result) > 0
        assert result[0]["artist"] == "Artist X"

    def test_case_insensitive_search(self, duckdb_with_sample_data):
        """大文字小文字を区別しない検索。"""
        # Arrange: search_tracks_by_nameを使用
        parquet_path = duckdb_with_sample_data.test_parquet_path

        # Act: 小文字と大文字の両方で検索
        result_lower = search_tracks_by_name(
            duckdb_with_sample_data, parquet_path, query="song a", limit=20
        )
        result_upper = search_tracks_by_name(
            duckdb_with_sample_data, parquet_path, query="SONG A", limit=20
        )

        # Assert: 大文字小文字に関わらず同じ結果が返されることを検証
        assert len(result_lower) == len(result_upper)
        assert result_lower[0]["track_name"] == result_upper[0]["track_name"]
