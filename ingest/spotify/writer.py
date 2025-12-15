"""Spotify データ用の DuckDB ライター。"""

import logging
import duckdb
from datetime import timezone
from typing import Any
from uuid import uuid4

logger = logging.getLogger(__name__)


class SpotifyDuckDBWriter:
    """べき等な upsert で Spotify データを DuckDB に書き込む。"""

    def __init__(self, conn: duckdb.DuckDBPyConnection):
        """DuckDB コネクション付きでライターを初期化する。

        Args:
            conn: DuckDB コネクション
        """
        self.conn = conn

    def upsert_plays(self, items: list[dict[str, Any]]) -> int:
        """重複除去しながら再生履歴を挿入する。

        Args:
            items: Spotify API の生レスポンス項目 (recently_played)

        Returns:
            upsert したレコード数
        """
        if not items:
            return 0

        logger.info(f"Upserting {len(items)} play records")

        # API レスポンスを行データに変換
        rows = []
        for item in items:
            track = item.get("track", {})

            # played_at + track_id から決定的な play_id を生成
            played_at_str = item.get("played_at", "")
            track_id = track.get("id", "")
            play_id = f"{played_at_str}_{track_id}" if track_id else str(uuid4())

            row = {
                "play_id": play_id,
                "played_at_utc": played_at_str,
                "track_id": track_id,
                "track_name": track.get("name", "Unknown"),
                "artist_ids": [a.get("id") for a in track.get("artists", [])],
                "artist_names": [a.get("name") for a in track.get("artists", [])],
                "album_id": track.get("album", {}).get("id"),
                "album_name": track.get("album", {}).get("name"),
                "ms_played": track.get("duration_ms"),
                "context_type": item.get("context", {}).get("type") if item.get("context") else None,
                "device_name": None,  # recently_played API には含まれない
            }
            rows.append(row)

        # べき等性のため INSERT OR REPLACE を使用
        self.conn.executemany("""
            INSERT OR REPLACE INTO raw.spotify_plays
            (play_id, played_at_utc, track_id, track_name,
             artist_ids, artist_names, album_id, album_name,
             ms_played, context_type, device_name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, [
            (r["play_id"], r["played_at_utc"], r["track_id"], r["track_name"],
             r["artist_ids"], r["artist_names"], r["album_id"], r["album_name"],
             r["ms_played"], r["context_type"], r["device_name"])
            for r in rows
        ])

        logger.info(f"Successfully upserted {len(rows)} plays")
        return len(rows)

    def upsert_tracks(self, items: list[dict[str, Any]]) -> int:
        """楽曲マスタデータを挿入する。

        Args:
            items: Spotify API の生レスポンス項目 (recently_played)

        Returns:
            upsert したユニーク楽曲数
        """
        if not items:
            return 0

        logger.info(f"Upserting track master data")

        # ユニークな楽曲を抽出
        tracks_dict = {}
        for item in items:
            track = item.get("track", {})
            track_id = track.get("id")
            if track_id and track_id not in tracks_dict:
                tracks_dict[track_id] = {
                    "track_id": track_id,
                    "name": track.get("name", "Unknown"),
                    "artist_ids": [a.get("id") for a in track.get("artists", [])],
                    "artist_names": [a.get("name") for a in track.get("artists", [])],
                    "album_id": track.get("album", {}).get("id"),
                    "album_name": track.get("album", {}).get("name"),
                    "duration_ms": track.get("duration_ms"),
                    "popularity": track.get("popularity"),
                }

        rows = list(tracks_dict.values())

        if not rows:
            return 0

        # 楽曲を upsert
        self.conn.executemany("""
            INSERT OR REPLACE INTO mart.spotify_tracks
            (track_id, name, artist_ids, artist_names,
             album_id, album_name, duration_ms, popularity)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, [
            (r["track_id"], r["name"], r["artist_ids"], r["artist_names"],
             r["album_id"], r["album_name"], r["duration_ms"], r["popularity"])
            for r in rows
        ])

        logger.info(f"Successfully upserted {len(rows)} tracks")
        return len(rows)

    def get_stats(self) -> dict[str, Any]:
        """データベース統計情報を取得する。

        Returns:
            total_plays, total_tracks, latest_play を含む辞書
        """
        stats = {}

        # 再生回数をカウント
        result = self.conn.execute("SELECT COUNT(*) FROM raw.spotify_plays").fetchone()
        stats["total_plays"] = result[0] if result else 0

        # 楽曲数をカウント
        result = self.conn.execute("SELECT COUNT(*) FROM mart.spotify_tracks").fetchone()
        stats["total_tracks"] = result[0] if result else 0

        # 最新の再生日時を取得
        result = self.conn.execute("""
            SELECT MAX(played_at_utc) FROM raw.spotify_plays
        """).fetchone()
        latest_play = result[0] if result else None
        # DuckDBから返される datetime は naive なので、UTC timezone を付与
        if latest_play and latest_play.tzinfo is None:
            latest_play = latest_play.replace(tzinfo=timezone.utc)
        stats["latest_play"] = latest_play

        return stats
