"""プッシュ通知トークンのリポジトリ実装。

データベースへのアクセスを抽象化し、トークンのCRUD操作を提供します。
"""

import logging
from dataclasses import dataclass
from datetime import datetime
from sqlite3 import Connection
from typing import Any

from gateway.infrastructure.database import get_db_connection

logger = logging.getLogger(__name__)


@dataclass
class PushDevice:
    """プッシュ通知デバイス情報。

    Attributes:
        id: デバイスID
        user_id: ユーザーID
        device_token: FCMデバイストークン
        platform: プラットフォーム（android, ios）
        device_name: デバイス名
        enabled: 有効フラグ
        last_seen_at: 最終確認日時
        created_at: 作成日時
    """

    id: int
    user_id: str
    device_token: str
    platform: str
    device_name: str | None
    enabled: bool
    last_seen_at: datetime
    created_at: datetime


class PushTokenRepository:
    """プッシュ通知トークンのリポジトリクラス。"""

    @staticmethod
    def _parse_datetime(value: Any) -> datetime:
        """SQLite から取得した日時を datetime に正規化します。"""
        if isinstance(value, datetime):
            return value
        if isinstance(value, str):
            return datetime.fromisoformat(value)
        raise ValueError(f"Unsupported datetime value: {value!r}")

    def _row_to_push_device(self, row: tuple[Any, ...]) -> PushDevice:
        """DB 行を PushDevice に変換します。"""
        return PushDevice(
            id=int(row[0]),
            user_id=str(row[1]),
            device_token=str(row[2]),
            platform=str(row[3]),
            device_name=row[4],
            enabled=bool(row[5]),
            last_seen_at=self._parse_datetime(row[6]),
            created_at=self._parse_datetime(row[7]),
        )

    def _fetch_device_by_token(
        self, conn: Connection, device_token: str
    ) -> PushDevice:
        """デバイストークンで端末情報を取得します。"""
        row = conn.execute(
            """
            SELECT id, user_id, device_token, platform, device_name,
                   enabled, last_seen_at, created_at
            FROM push_devices
            WHERE device_token = ?
        """,
            [device_token],
        ).fetchone()
        if row is None:
            raise ValueError(f"push_device not found for token: {device_token}")
        return self._row_to_push_device(row)

    def save_token(
        self,
        user_id: str,
        device_token: str,
        platform: str,
        device_name: str | None = None,
    ) -> PushDevice:
        """FCMトークンを保存または更新します。

        Args:
            user_id: ユーザーID
            device_token: FCMデバイストークン
            platform: プラットフォーム（android, ios）
            device_name: デバイス名（オプション）

        Returns:
            保存または更新されたPushDeviceインスタンス

        Note:
            device_tokenが既存の場合は、last_seen_atを更新します。
        """
        with get_db_connection() as conn:
            # 既存トークンの確認
            existing = conn.execute(
                "SELECT id FROM push_devices WHERE device_token = ?",
                [device_token],
            ).fetchone()

            if existing:
                # 既存の場合は更新
                conn.execute(
                    """
                    UPDATE push_devices
                    SET user_id = ?,
                        platform = ?,
                        device_name = ?,
                        enabled = 1,
                        last_seen_at = CURRENT_TIMESTAMP
                    WHERE device_token = ?
                """,
                    [user_id, platform, device_name, device_token],
                )

                logger.info("Updated existing push token: user_id=%s", user_id)
            else:
                # 新規登録
                conn.execute(
                    """
                    INSERT INTO push_devices
                    (user_id, device_token, platform, device_name)
                    VALUES (?, ?, ?, ?)
                """,
                    [user_id, device_token, platform, device_name],
                )

                logger.info("Registered new push token: user_id=%s", user_id)

            return self._fetch_device_by_token(conn, device_token)

    def get_tokens(self, user_id: str) -> list[PushDevice]:
        """ユーザーに紐づく全てのトークンを取得します。

        Args:
            user_id: ユーザーID

        Returns:
            PushDeviceインスタンスのリスト（有効なもののみ）
        """
        with get_db_connection() as conn:
            rows = conn.execute(
                """
                SELECT id, user_id, device_token, platform, device_name,
                       enabled, last_seen_at, created_at
                FROM push_devices
                WHERE user_id = ? AND enabled = 1
                ORDER BY last_seen_at DESC
            """,
                [user_id],
            ).fetchall()

            return [self._row_to_push_device(row) for row in rows]

    def update_last_seen(self, device_token: str) -> None:
        """デバイストークンの最終確認日時を更新します。

        Args:
            device_token: FCMデバイストークン
        """
        with get_db_connection() as conn:
            conn.execute(
                """
                UPDATE push_devices
                SET last_seen_at = CURRENT_TIMESTAMP
                WHERE device_token = ?
            """,
                [device_token],
            )

            logger.debug("Updated last_seen for token: %s", device_token[:10] + "...")

    def disable_token(self, device_token: str) -> None:
        """デバイストークンを無効化します。

        Args:
            device_token: 無効化するFCMデバイストークン

        Note:
            トークンが無効になった場合に使用します。
        """
        with get_db_connection() as conn:
            conn.execute(
                """
                UPDATE push_devices
                SET enabled = 0
                WHERE device_token = ?
            """,
                [device_token],
            )

            logger.info("Disabled push token: %s", device_token[:10] + "...")


# ============================================================================
# Session Repository
# ============================================================================


@dataclass
class Session:
    """端末セッション情報。

    Attributes:
        session_id: tmuxセッションID (例: agent-0001)
        activity: 最終アクティブ時刻 (tmuxフォーマット)
        created: セッション作成時刻 (tmuxフォーマット)
    """

    session_id: str
    activity: str | None
    created: str | None


class SessionRepository:
    """端末セッションのリポジトリクラス。

    tmuxセッション情報のキャッシュと取得を提供します。
    """

    # クラスレベルのキャッシュ（簡易実装）
    _cache: dict[str, Session] = {}

    def save_session(self, session: Session) -> None:
        """セッション情報を保存します。

        Args:
            session: 保存するセッション情報
        """
        self._cache[session.session_id] = session

    def get_session(self, session_id: str) -> Session | None:
        """指定されたセッションIDのセッション情報を取得します。

        Args:
            session_id: セッションID

        Returns:
            セッション情報が見つかった場合はSessionインスタンス、
            見つからない場合はNone
        """
        return self._cache.get(session_id)

    def list_sessions(self) -> list[Session]:
        """全てのセッション情報を取得します。

        Returns:
            Sessionインスタンスのリスト
        """
        return list(self._cache.values())

    def delete_session(self, session_id: str) -> None:
        """指定されたセッションIDのセッション情報を削除します。

        Args:
            session_id: 削除するセッションID

        Note:
            セッションが存在しない場合は何もしません。
        """
        self._cache.pop(session_id, None)

    def clear_cache(self) -> None:
        """キャッシュをクリアします。

        Note:
            主にテスト用のメソッドです。
        """
        self._cache.clear()
