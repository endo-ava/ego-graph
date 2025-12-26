"""R2上のLast.fmエンリッチメントデータを検証・確認するスクリプト。

Last.fmのトラック情報とアーティスト情報の総レコード数と最新データを表示します。
DuckDBの httpfs 拡張を使用して、R2上のファイルを直接クエリします。

Usage:
    uv run python backend/scripts/verify_lastfm_parquet.py
"""

import logging
import os
import sys

import duckdb
from tabulate import tabulate

# プロジェクトルートをパスに追加
sys.path.append(os.getcwd())

from shared.config import Config

# ロギング設定
logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


def verify_r2_lastfm_data():
    """R2上のLast.fm Parquetデータを検証し、最新のメタデータを表示する。"""
    logger.info("🦆 Verifying EgoGraph Last.fm Data in R2...")

    try:
        config = Config.from_env()
    except Exception:
        logger.exception("Failed to load config")
        return

    if not config.duckdb or not config.duckdb.r2:
        logger.error("R2 configuration is missing.")
        return

    r2_conf = config.duckdb.r2
    conn = duckdb.connect(":memory:")

    try:
        # S3(R2) 設定の適用
        conn.execute("INSTALL httpfs; LOAD httpfs;")
        conn.execute(
            """
            CREATE SECRET (
                TYPE S3,
                KEY_ID ?,
                SECRET ?,
                REGION 'auto',
                ENDPOINT ?,
                URL_STYLE 'path'
            );
            """,
            [
                r2_conf.access_key_id,
                r2_conf.secret_access_key.get_secret_value(),
                r2_conf.endpoint_url.replace("https://", ""),
            ],
        )

        # Parquetファイルのパスパターン
        tracks_url = f"s3://{r2_conf.bucket_name}/{r2_conf.master_path}lastfm/tracks/**/*.parquet"
        artists_url = f"s3://{r2_conf.bucket_name}/{r2_conf.master_path}lastfm/artists/**/*.parquet"

        # === トラック情報の確認 ===
        logger.info("\n" + "=" * 60)
        logger.info("🎵 Last.fm Track Information")
        logger.info("=" * 60)

        try:
            track_count = conn.execute(
                "SELECT COUNT(*) FROM read_parquet(?)", [tracks_url]
            ).fetchone()[0]
            logger.info(f"✅ Total track records in R2: {track_count}")

            if track_count > 0:
                # 最新20件のトラック情報を表示
                logger.info("\n📊 Latest 20 Tracks:")
                query_tracks = """
                    SELECT track_name, artist_name, listeners, playcount
                    FROM read_parquet(?)
                    ORDER BY playcount DESC
                    LIMIT 20
                """
                df_tracks = conn.execute(query_tracks, [tracks_url]).df()
                df_tracks.index = df_tracks.index + 1
                print(
                    tabulate(
                        df_tracks,
                        headers=["#", "Track Name", "Artist", "Listeners", "Playcount"],
                        tablefmt="simple",
                    )
                )

                # 詳細表示 (最新5件)
                logger.info("\n🔍 Detailed View (Top 5 by Playcount):")
                query_detail = """
                    SELECT track_name, artist_name, listeners, playcount, tags
                    FROM read_parquet(?)
                    ORDER BY playcount DESC
                    LIMIT 5
                """
                df_detail = conn.execute(query_detail, [tracks_url]).df()
                print(tabulate(df_detail, headers="keys", tablefmt="simple_grid"))
            else:
                logger.info("ℹ️ No track data found. Run Last.fm enrichment first.")

        except duckdb.IOException as e:
            if "No files found" in str(e):
                logger.warning("⚠️ No track Parquet files found in R2.")
            else:
                logger.error(f"❌ DuckDB IO Error (tracks): {e}")

        # === アーティスト情報の確認 ===
        logger.info("\n" + "=" * 60)
        logger.info("🎤 Last.fm Artist Information")
        logger.info("=" * 60)

        try:
            artist_count = conn.execute(
                "SELECT COUNT(*) FROM read_parquet(?)", [artists_url]
            ).fetchone()[0]
            logger.info(f"✅ Total artist records in R2: {artist_count}")

            if artist_count > 0:
                # 最新20件のアーティスト情報を表示
                logger.info("\n📊 Top 20 Artists by Listeners:")
                query_artists = """
                    SELECT artist_name, listeners, playcount
                    FROM read_parquet(?)
                    ORDER BY listeners DESC
                    LIMIT 20
                """
                df_artists = conn.execute(query_artists, [artists_url]).df()
                df_artists.index = df_artists.index + 1
                print(
                    tabulate(
                        df_artists,
                        headers=["#", "Artist Name", "Listeners", "Playcount"],
                        tablefmt="simple",
                    )
                )

                # 詳細表示 (最新5件)
                logger.info("\n🔍 Detailed View (Top 5 by Listeners):")
                query_detail = """
                    SELECT artist_name, listeners, playcount, tags, bio_summary
                    FROM read_parquet(?)
                    ORDER BY listeners DESC
                    LIMIT 5
                """
                df_detail = conn.execute(query_detail, [artists_url]).df()
                print(tabulate(df_detail, headers="keys", tablefmt="simple_grid"))
            else:
                logger.info("ℹ️ No artist data found. Run Last.fm enrichment first.")

        except duckdb.IOException as e:
            if "No files found" in str(e):
                logger.warning("⚠️ No artist Parquet files found in R2.")
            else:
                logger.error(f"❌ DuckDB IO Error (artists): {e}")

    except Exception as e:
        logger.error(f"❌ Unexpected Error: {e}")
    finally:
        conn.close()


if __name__ == "__main__":
    verify_r2_lastfm_data()
