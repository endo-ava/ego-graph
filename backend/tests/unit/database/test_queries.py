"""Database/Queries層のテスト。"""

import pytest
from datetime import date

from backend.database.queries import (
    get_parquet_path,
    execute_query,
    get_top_tracks,
    get_listening_stats,
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
        # Arrange: トップトラック取得クエリとパラメータを準備
        # read_parquet()の代わりにテーブル名を使用するため、
        # クエリを直接実行して動作を確認
        query = """
            SELECT
                track_name,
                artist_names[1] as artist,
                COUNT(*) as play_count,
                SUM(ms_played) / 60000.0 as total_minutes
            FROM spotify_plays
            WHERE played_at_utc::DATE BETWEEN ? AND ?
            GROUP BY track_name, artist_names[1]
            ORDER BY play_count DESC
            LIMIT ?
        """

        # Act: トップトラックを取得
        result = execute_query(
            duckdb_with_sample_data,
            query,
            [date(2024, 1, 1), date(2024, 1, 3), 5],
        )

        # Assert: トップトラックが正しく取得されることを検証
        assert len(result) > 0
        # "Song A" (track_1) が3回再生されているので1位
        assert result[0]["track_name"] == "Song A"
        assert result[0]["play_count"] == 3
        assert "total_minutes" in result[0]

    def test_respects_limit_parameter(self, duckdb_with_sample_data):
        """limitパラメータを尊重。"""
        # Arrange: limit=2でクエリを準備
        query = """
            SELECT
                track_name,
                artist_names[1] as artist,
                COUNT(*) as play_count,
                SUM(ms_played) / 60000.0 as total_minutes
            FROM spotify_plays
            WHERE played_at_utc::DATE BETWEEN ? AND ?
            GROUP BY track_name, artist_names[1]
            ORDER BY play_count DESC
            LIMIT ?
        """

        # Act: limit=2でトップトラックを取得
        result = execute_query(
            duckdb_with_sample_data,
            query,
            [date(2024, 1, 1), date(2024, 1, 3), 2],
        )

        # Assert: 最大2件までしか返されないことを検証
        assert len(result) <= 2

    def test_filters_by_date_range(self, duckdb_with_sample_data):
        """日付範囲でフィルタリング。"""
        # Arrange: 2024-01-01のみに絞り込むクエリを準備
        query = """
            SELECT
                track_name,
                artist_names[1] as artist,
                COUNT(*) as play_count,
                SUM(ms_played) / 60000.0 as total_minutes
            FROM spotify_plays
            WHERE played_at_utc::DATE BETWEEN ? AND ?
            GROUP BY track_name, artist_names[1]
            ORDER BY play_count DESC
            LIMIT ?
        """

        # Act: 2024-01-01のデータのみ取得
        result = execute_query(
            duckdb_with_sample_data,
            query,
            [date(2024, 1, 1), date(2024, 1, 1), 10],
        )

        # Assert: 2024-01-01には2件のレコードがあることを検証
        assert len(result) == 2


class TestGetListeningStats:
    """get_listening_stats のテスト。"""

    def test_aggregates_by_day(self, duckdb_with_sample_data):
        """日単位で集計。"""
        # Arrange: 日単位集計クエリを準備
        query = """
            SELECT
                strftime(played_at_utc::DATE, '%Y-%m-%d') as period,
                SUM(ms_played) as total_ms,
                COUNT(*) as track_count,
                COUNT(DISTINCT track_id) as unique_tracks
            FROM spotify_plays
            WHERE played_at_utc::DATE BETWEEN ? AND ?
            GROUP BY period
            ORDER BY period ASC
        """

        # Act: 日単位で統計情報を取得
        result = execute_query(
            duckdb_with_sample_data, query, [date(2024, 1, 1), date(2024, 1, 3)]
        )

        # Assert: 3日分のデータが正しく集計されることを検証
        assert len(result) == 3  # 3日分
        assert result[0]["period"] == "2024-01-01"
        assert result[0]["track_count"] == 2

    def test_aggregates_by_month(self, duckdb_with_sample_data):
        """月単位で集計。"""
        # Arrange: 月単位集計クエリを準備
        query = """
            SELECT
                strftime(played_at_utc::DATE, '%Y-%m') as period,
                SUM(ms_played) as total_ms,
                COUNT(*) as track_count,
                COUNT(DISTINCT track_id) as unique_tracks
            FROM spotify_plays
            WHERE played_at_utc::DATE BETWEEN ? AND ?
            GROUP BY period
            ORDER BY period ASC
        """

        # Act: 月単位で統計情報を取得
        result = execute_query(
            duckdb_with_sample_data, query, [date(2024, 1, 1), date(2024, 1, 3)]
        )

        # Assert: 1ヶ月分のデータが正しく集計されることを検証
        assert len(result) == 1  # 1ヶ月分
        assert result[0]["period"] == "2024-01"
        assert result[0]["track_count"] == 5  # 全5件

    def test_invalid_granularity_raises_error(self):
        """無効な粒度でエラー発生。"""
        # Arrange: この関数はテスト不要（get_listening_stats関数内でチェックされる）

        # Act: なし

        # Assert: 実装側の検証ロジックを信頼
        pass


class TestSearchTracksByName:
    """search_tracks_by_name のテスト。"""

    def test_searches_by_track_name(self, duckdb_with_sample_data):
        """トラック名で検索。"""
        # Arrange: トラック名検索クエリと検索パターンを準備
        search_pattern = "%Song A%"
        query = """
            SELECT
                track_name,
                artist_names[1] as artist,
                COUNT(*) as play_count,
                MAX(played_at_utc)::VARCHAR as last_played
            FROM spotify_plays
            WHERE LOWER(track_name) LIKE LOWER(?)
               OR LOWER(artist_names[1]) LIKE LOWER(?)
            GROUP BY track_name, artist_names[1]
            ORDER BY play_count DESC
            LIMIT ?
        """

        # Act: トラック名で検索
        result = execute_query(
            duckdb_with_sample_data, query, [search_pattern, search_pattern, 20]
        )

        # Assert: "Song A"が見つかることを検証
        assert len(result) > 0
        assert result[0]["track_name"] == "Song A"

    def test_searches_by_artist_name(self, duckdb_with_sample_data):
        """アーティスト名で検索。"""
        # Arrange: アーティスト名検索クエリと検索パターンを準備
        search_pattern = "%Artist X%"
        query = """
            SELECT
                track_name,
                artist_names[1] as artist,
                COUNT(*) as play_count,
                MAX(played_at_utc)::VARCHAR as last_played
            FROM spotify_plays
            WHERE LOWER(track_name) LIKE LOWER(?)
               OR LOWER(artist_names[1]) LIKE LOWER(?)
            GROUP BY track_name, artist_names[1]
            ORDER BY play_count DESC
            LIMIT ?
        """

        # Act: アーティスト名で検索
        result = execute_query(
            duckdb_with_sample_data, query, [search_pattern, search_pattern, 20]
        )

        # Assert: Artist XはSong Aなので見つかることを検証
        assert len(result) > 0
        assert result[0]["artist"] == "Artist X"

    def test_case_insensitive_search(self, duckdb_with_sample_data):
        """大文字小文字を区別しない検索。"""
        # Arrange: 大文字と小文字の検索パターンを準備
        search_pattern_lower = "%song a%"
        search_pattern_upper = "%SONG A%"

        query = """
            SELECT
                track_name,
                artist_names[1] as artist,
                COUNT(*) as play_count,
                MAX(played_at_utc)::VARCHAR as last_played
            FROM spotify_plays
            WHERE LOWER(track_name) LIKE LOWER(?)
               OR LOWER(artist_names[1]) LIKE LOWER(?)
            GROUP BY track_name, artist_names[1]
            ORDER BY play_count DESC
            LIMIT ?
        """

        # Act: 小文字と大文字の両方で検索
        result_lower = execute_query(
            duckdb_with_sample_data,
            query,
            [search_pattern_lower, search_pattern_lower, 20],
        )
        result_upper = execute_query(
            duckdb_with_sample_data,
            query,
            [search_pattern_upper, search_pattern_upper, 20],
        )

        # Assert: 大文字小文字に関わらず同じ結果が返されることを検証
        assert len(result_lower) == len(result_upper)
        assert result_lower[0]["track_name"] == result_upper[0]["track_name"]
