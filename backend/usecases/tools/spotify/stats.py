"""Spotify再生統計ツール。"""

import logging
from collections.abc import Callable
from typing import Any

from backend.infrastructure.database import DuckDBConnection
from backend.usecases.spotify_stats import (
    fetch_listening_stats,
    fetch_top_tracks,
)
from backend.usecases.tools.base import ToolBase
from backend.validators import (
    validate_date_range,
    validate_granularity,
    validate_limit,
)
from shared.config import R2Config

logger = logging.getLogger(__name__)


class GetTopTracksTool(ToolBase):
    """指定期間で最も再生された曲を取得するツール。"""

    def __init__(
        self,
        r2_config: R2Config,
        db_connection_factory: Callable[[], DuckDBConnection] | None = None,
    ):
        """GetTopTracksToolを初期化します。

        Args:
            r2_config: R2設定
            db_connection_factory: DuckDB接続ファクトリ（未指定なら都度生成）
        """
        self.r2_config = r2_config
        self._db_connection_factory = db_connection_factory

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
        start, end = validate_date_range(start_date, end_date)
        validated_limit = validate_limit(limit, max_value=100)

        logger.info("Executing get_top_tracks: %s to %s, limit=%s", start, end, limit)

        # テスト用のファクトリがある場合はそれを使用（モック注入用）
        if self._db_connection_factory:
            db_connection = self._db_connection_factory()
            # ファクトリから返されたオブジェクトをコンテキストマネージャーとして使用
            with db_connection as conn:
                return fetch_top_tracks(
                    conn, self.r2_config, start, end, validated_limit
                )

        # 通常の場合はDuckDBConnectionをコンテキストマネージャーとして使用
        with DuckDBConnection(self.r2_config) as conn:
            return fetch_top_tracks(conn, self.r2_config, start, end, validated_limit)


class GetListeningStatsTool(ToolBase):
    """期間別の視聴統計を取得するツール。"""

    def __init__(
        self,
        r2_config: R2Config,
        db_connection_factory: Callable[[], DuckDBConnection] | None = None,
    ):
        """GetListeningStatsToolを初期化します。

        Args:
            r2_config: R2設定
            db_connection_factory: DuckDB接続ファクトリ（未指定なら都度生成）
        """
        self.r2_config = r2_config
        self._db_connection_factory = db_connection_factory

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
        start, end = validate_date_range(start_date, end_date)
        validated_granularity = validate_granularity(granularity)

        logger.info(
            "Executing get_listening_stats: %s to %s, granularity=%s",
            start,
            end,
            granularity,
        )

        # テスト用のファクトリがある場合はそれを使用（モック注入用）
        if self._db_connection_factory:
            db_connection = self._db_connection_factory()
            # ファクトリから返されたオブジェクトをコンテキストマネージャーとして使用
            with db_connection as conn:
                return fetch_listening_stats(
                    conn, self.r2_config, start, end, validated_granularity
                )

        # 通常の場合はDuckDBConnectionをコンテキストマネージャーとして使用
        with DuckDBConnection(self.r2_config) as conn:
            return fetch_listening_stats(
                conn, self.r2_config, start, end, validated_granularity
            )
