"""チャット履歴用DuckDB接続管理。

ローカルファイルベースのDuckDB接続で、スレッドとメッセージの永続化を担当します。
R2用の:memory:接続（connection.py）とは独立しています。
"""

import logging
from pathlib import Path

import duckdb

logger = logging.getLogger(__name__)

# チャット履歴DBのパス
DB_PATH = Path(__file__).parent.parent / "data" / "chat.duckdb"


class ChatDuckDBConnection:
    """チャット履歴用のDuckDB接続マネージャー。

    コンテキストマネージャーとして使用し、ローカルファイルベースの
    DuckDB接続を作成します。

    Example:
        >>> with ChatDuckDBConnection() as conn:
        ...     result = conn.execute(
        ...         "SELECT * FROM threads WHERE user_id = ?",
        ...         ("default_user",)
        ...     )
        ...     threads = result.fetchall()
    """

    def __init__(self):
        """ChatDuckDBConnectionを初期化します。"""
        self.conn: duckdb.DuckDBPyConnection | None = None

    def __enter__(self) -> duckdb.DuckDBPyConnection:
        """コンテキストマネージャーのエントリー。

        データディレクトリを作成し、ローカルファイルベースの
        DuckDB接続を開きます。

        Returns:
            duckdb.DuckDBPyConnection: 開かれたDuckDBコネクション

        Raises:
            duckdb.Error: DuckDB接続に失敗した場合
        """
        # データディレクトリを作成（存在しない場合）
        DB_PATH.parent.mkdir(parents=True, exist_ok=True)
        logger.debug("Opening chat database at path: %s", DB_PATH)

        # ローカルファイルベースの接続を作成
        self.conn = duckdb.connect(str(DB_PATH))
        return self.conn

    def __exit__(self, exc_type, exc_val, exc_tb):
        """コンテキストマネージャーの終了。

        接続をクローズします。
        """
        if self.conn:
            self.conn.close()
            self.conn = None
            logger.debug("Closed chat database connection")


def create_chat_tables(conn: duckdb.DuckDBPyConnection):
    """チャット履歴用のテーブルを作成します。

    threads テーブルとmessages テーブルを作成し、
    必要なインデックスを設定します。べき等な操作です。

    Args:
        conn: DuckDBコネクション

    Raises:
        duckdb.Error: テーブル作成に失敗した場合
    """
    logger.info("Creating chat tables if they do not exist")

    # threads テーブル
    conn.execute("""
        CREATE TABLE IF NOT EXISTS threads (
            thread_id VARCHAR PRIMARY KEY,
            user_id VARCHAR NOT NULL,
            title VARCHAR NOT NULL,
            created_at TIMESTAMPTZ NOT NULL,
            last_message_at TIMESTAMPTZ NOT NULL
        )
    """)

    # threads テーブルのインデックス
    # ユーザーIDと最終メッセージ日時でソートするため
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_user_last_message
        ON threads(user_id, last_message_at DESC)
    """)

    # messages テーブル
    # NOTE: DuckDB 1.1.0以降は外部キー制約をサポートしていますが、
    # 現時点では設定していません。将来的に外部キー制約を追加する場合は、
    # 既存データのマイグレーションを考慮する必要があります。
    # 現在は参照整合性をアプリケーションロジックで保証しています。
    conn.execute("""
        CREATE TABLE IF NOT EXISTS messages (
            message_id VARCHAR PRIMARY KEY,
            thread_id VARCHAR NOT NULL,
            user_id VARCHAR NOT NULL,
            role VARCHAR NOT NULL,
            content TEXT NOT NULL,
            created_at TIMESTAMPTZ NOT NULL
        )
    """)

    # messages テーブルのインデックス
    # スレッド内のメッセージを作成日時順で取得するため
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_thread_created
        ON messages(thread_id, created_at)
    """)

    conn.commit()
    logger.info("Chat tables created successfully")
