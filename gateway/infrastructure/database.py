"""SQLite データベース接続とスキーマ管理。

プッシュ通知トークンを管理するテーブルを提供します。
"""

import logging
import sqlite3
from contextlib import contextmanager
from pathlib import Path

logger = logging.getLogger(__name__)

# データベースファイルパス
DB_PATH = Path(__file__).parent.parent / "gateway.db"


@contextmanager
def get_db_connection():
    """SQLite データベース接続を提供するコンテキストマネージャー。

    Yields:
        sqlite3.Connection: SQLite 接続オブジェクト

    Example:
        >>> with get_db_connection() as conn:
        ...     result = conn.execute("SELECT 1").fetchone()
    """
    conn = sqlite3.connect(str(DB_PATH))
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def create_push_tables(conn: sqlite3.Connection) -> None:
    """プッシュ通知関連のテーブルを作成します。

    Args:
        conn: SQLite 接続オブジェクト

    Note:
        テーブルが既存の場合はスキップします。
    """
    conn.execute("""
        CREATE TABLE IF NOT EXISTS push_devices (
            id INTEGER PRIMARY KEY,
            user_id TEXT NOT NULL DEFAULT 'default_user',
            device_token TEXT NOT NULL UNIQUE,
            platform TEXT NOT NULL,
            device_name TEXT,
            enabled INTEGER NOT NULL DEFAULT 1,
            last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
    """)
    logger.info("Push devices table created successfully")


def init_database() -> None:
    """データベースを初期化します。

    テーブル作成とインデックス作成を行います。
    """
    with get_db_connection() as conn:
        create_push_tables(conn)
        # ユーザーIDと有効フラグのインデックスを作成
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_push_devices_user_enabled
            ON push_devices(user_id, enabled)
        """)
        logger.info("Database initialized successfully")
