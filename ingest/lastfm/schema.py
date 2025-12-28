"""Last.fm データ用の DuckDB スキーマ定義。"""

import logging

import duckdb

logger = logging.getLogger(__name__)


class LastFmSchema:
    """Last.fm データ用の DuckDB ビュー（Mart層）を管理する。"""

    @staticmethod
    def initialize_mart(
        conn: duckdb.DuckDBPyConnection, tracks_glob: str, artists_glob: str
    ):
        """Mart スキーマに Last.fm のビューを作成します。

        Args:
            conn: DuckDB コネクション
            tracks_glob: トラック Parquet ファイルの S3 グロブパターン
            artists_glob: アーティスト Parquet ファイルの S3 グロブパターン
        """
        logger.info("Initializing Last.fm Mart views...")

        # スキーマの存在を確認
        conn.execute("CREATE SCHEMA IF NOT EXISTS mart")

        # トラックビューの作成
        try:
            conn.from_parquet(tracks_glob, hive_partitioning=True).create_view(
                "mart.lastfm_tracks", replace=True
            )
            logger.info(f"Created view mart.lastfm_tracks using {tracks_glob}")
        except Exception as e:
            logger.warning(f"Could not create mart.lastfm_tracks: {e}")

        # アーティストビューの作成
        try:
            conn.from_parquet(artists_glob, hive_partitioning=True).create_view(
                "mart.lastfm_artists", replace=True
            )
            logger.info(f"Created view mart.lastfm_artists using {artists_glob}")
        except Exception as e:
            logger.warning(f"Could not create mart.lastfm_artists: {e}")

        logger.info("Last.fm Mart views initialized")
