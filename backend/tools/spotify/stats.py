"""Spotify再生統計ツール。"""

import logging
from datetime import date
from typing import Any

from backend.database.connection import DuckDBConnection
from backend.database.queries import get_listening_stats, get_top_tracks
from backend.tools.base import ToolBase

logger = logging.getLogger(__name__)


class GetTopTracksTool(ToolBase):
    """指定期間で最も再生された曲を取得するツール。"""

    def __init__(self, db_connection: DuckDBConnection, parquet_path: str):
        """GetTopTracksToolを初期化します。

        Args:
            db_connection: DuckDB接続ファクトリ
            parquet_path: ParquetファイルのS3パス
        """
        self.db_connection = db_connection
        self.parquet_path = parquet_path

    @property
    def name(self) -> str:
        return "get_top_tracks"

    @property
    def description(self) -> str:
        return (
            "指定した期間（start_dateからend_date）で最も再生された曲を取得します。"
            "再生回数の多い順にソートされます。"
        )

    @property
    def input_schema(self) -> dict[str, Any]:
        return {
            "type": "object",
            "properties": {
                "start_date": {
                    "type": "string",
                    "description": "開始日（ISO形式: YYYY-MM-DD）",
                },
                "end_date": {
                    "type": "string",
                    "description": "終了日（ISO形式: YYYY-MM-DD）",
                },
                "limit": {
                    "type": "integer",
                    "description": "取得する曲数",
                    "default": 10,
                },
            },
            "required": ["start_date", "end_date"],
        }

    def execute(
        self, start_date: str, end_date: str, limit: int = 10
    ) -> list[dict[str, Any]]:
        """トップトラックを取得します。

        Args:
            start_date: 開始日（ISO形式: YYYY-MM-DD）
            end_date: 終了日（ISO形式: YYYY-MM-DD）
            limit: 取得する曲数

        Returns:
            トップトラックのリスト

        Raises:
            ValueError: 日付形式が不正な場合
        """
        try:
            start = date.fromisoformat(start_date)
            end = date.fromisoformat(end_date)
        except ValueError as e:
            raise ValueError(f"Invalid date format: {e}") from e

        logger.info(f"Executing get_top_tracks: {start} to {end}, limit={limit}")

        with self.db_connection as conn:
            return get_top_tracks(conn, self.parquet_path, start, end, limit)


class GetListeningStatsTool(ToolBase):
    """期間別の視聴統計を取得するツール。"""

    def __init__(self, db_connection: DuckDBConnection, parquet_path: str):
        """GetListeningStatsToolを初期化します。

        Args:
            db_connection: DuckDB接続ファクトリ
            parquet_path: ParquetファイルのS3パス
        """
        self.db_connection = db_connection
        self.parquet_path = parquet_path

    @property
    def name(self) -> str:
        return "get_listening_stats"

    @property
    def description(self) -> str:
        return (
            "指定した期間の視聴統計を取得します。"
            "日別、週別、月別で集計できます。"
            "総再生時間、再生トラック数、ユニーク曲数を返します。"
        )

    @property
    def input_schema(self) -> dict[str, Any]:
        return {
            "type": "object",
            "properties": {
                "start_date": {
                    "type": "string",
                    "description": "開始日（ISO形式: YYYY-MM-DD）",
                },
                "end_date": {
                    "type": "string",
                    "description": "終了日（ISO形式: YYYY-MM-DD）",
                },
                "granularity": {
                    "type": "string",
                    "description": "集計単位",
                    "enum": ["day", "week", "month"],
                    "default": "day",
                },
            },
            "required": ["start_date", "end_date"],
        }

    def execute(
        self, start_date: str, end_date: str, granularity: str = "day"
    ) -> list[dict[str, Any]]:
        """視聴統計を取得します。

        Args:
            start_date: 開始日（ISO形式: YYYY-MM-DD）
            end_date: 終了日（ISO形式: YYYY-MM-DD）
            granularity: 集計単位（"day", "week", "month"）

        Returns:
            期間別統計のリスト

        Raises:
            ValueError: 日付形式またはgranularityが不正な場合
        """
        try:
            start = date.fromisoformat(start_date)
            end = date.fromisoformat(end_date)
        except ValueError as e:
            raise ValueError(f"Invalid date format: {e}") from e

        logger.info(
            f"Executing get_listening_stats: {start} to {end}, granularity={granularity}"
        )

        with self.db_connection as conn:
            return get_listening_stats(conn, self.parquet_path, start, end, granularity)
