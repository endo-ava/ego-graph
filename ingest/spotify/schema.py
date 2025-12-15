"""Spotify データ用の DuckDB スキーマ定義。"""

import logging
import duckdb
from pathlib import Path

logger = logging.getLogger(__name__)


class SpotifySchema:
    """Spotify データ用の DuckDB スキーマを管理する。"""

    # スキーマ定義
    RAW_PLAYS_TABLE = """
        CREATE TABLE IF NOT EXISTS raw.spotify_plays (
            play_id VARCHAR PRIMARY KEY,
            played_at_utc TIMESTAMP NOT NULL,
            track_id VARCHAR NOT NULL,
            track_name VARCHAR NOT NULL,
            artist_ids VARCHAR[],
            artist_names VARCHAR[],
            album_id VARCHAR,
            album_name VARCHAR,
            ms_played INTEGER,
            context_type VARCHAR,
            device_name VARCHAR,
            inserted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    MART_TRACKS_TABLE = """
        CREATE TABLE IF NOT EXISTS mart.spotify_tracks (
            track_id VARCHAR PRIMARY KEY,
            name VARCHAR NOT NULL,
            artist_ids VARCHAR[],
            artist_names VARCHAR[],
            album_id VARCHAR,
            album_name VARCHAR,
            duration_ms INTEGER,
            popularity INTEGER,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    @staticmethod
    def initialize_db(db_path: str) -> duckdb.DuckDBPyConnection:
        """スキーマ付きで DuckDB を初期化する。

        Args:
            db_path: DuckDB データベースファイルへのパス

        Returns:
            DuckDB コネクション
        """
        logger.info(f"Initializing DuckDB at {db_path}")

        # ディレクトリが存在することを確認
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)

        conn = duckdb.connect(db_path)

        # スキーマを作成
        conn.execute("CREATE SCHEMA IF NOT EXISTS raw")
        conn.execute("CREATE SCHEMA IF NOT EXISTS mart")

        # テーブルを作成
        conn.execute(SpotifySchema.RAW_PLAYS_TABLE)
        conn.execute(SpotifySchema.MART_TRACKS_TABLE)

        logger.info("Schema initialized successfully")
        return conn

    @staticmethod
    def create_indexes(conn: duckdb.DuckDBPyConnection):
        """パフォーマンス向上用のインデックスを作成する。

        Args:
            conn: DuckDB コネクション
        """
        logger.info("Creating indexes...")

        # 時系列クエリ用の played_at インデックス
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_plays_time
            ON raw.spotify_plays(played_at_utc DESC)
        """)

        # JOIN用の track_id インデックス
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_plays_track
            ON raw.spotify_plays(track_id)
        """)

        logger.info("Indexes created")
