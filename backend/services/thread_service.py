"""スレッド管理サービス。

チャット履歴のスレッドとメッセージに対するCRUD操作を提供します。
"""

import logging
from datetime import datetime, timezone
from uuid import uuid4

import duckdb

from backend.models.thread import (
    THREAD_PREVIEW_MAX_LENGTH,
    THREAD_TITLE_MAX_LENGTH,
    Thread,
    ThreadMessage,
)

logger = logging.getLogger(__name__)


class ThreadService:
    """スレッド管理サービス。

    チャット履歴のスレッドとメッセージの作成・取得を担当します。
    すべてのメソッドはDuckDB接続を受け取り、トランザクション管理は
    呼び出し側に委譲します。

    Attributes:
        conn: DuckDBコネクション
    """

    def __init__(self, conn: duckdb.DuckDBPyConnection):
        """ThreadServiceを初期化します。

        Args:
            conn: DuckDBコネクション
        """
        self.conn = conn

    def create_thread(self, user_id: str, first_message_content: str) -> Thread:
        """新規スレッドを作成します。

        初回メッセージの先頭50文字をタイトルとして使用します。

        Args:
            user_id: ユーザーID
            first_message_content: 初回メッセージの内容

        Returns:
            Thread: 作成されたスレッドオブジェクト

        Raises:
            duckdb.Error: データベース操作に失敗した場合
        """
        thread_id = str(uuid4())
        now = datetime.now(timezone.utc)

        # タイトルは初回メッセージの先頭N文字
        title = first_message_content[:THREAD_TITLE_MAX_LENGTH]

        try:
            self.conn.execute(
                """
                INSERT INTO threads (
                    thread_id, user_id, title, created_at, last_message_at
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                (thread_id, user_id, title, now, now),
            )
            self.conn.commit()
        except Exception:
            self.conn.rollback()
            raise

        logger.info("Created thread thread_id=%s, user_id=%s", thread_id, user_id)

        return Thread(
            thread_id=thread_id,
            user_id=user_id,
            title=title,
            preview=title,
            message_count=0,
            created_at=now,
            last_message_at=now,
        )

    def add_message(
        self, thread_id: str, user_id: str, role: str, content: str
    ) -> ThreadMessage:
        """スレッドにメッセージを追加します。

        メッセージを追加し、スレッドのlast_message_atを更新します。

        Args:
            thread_id: スレッドのUUID
            user_id: ユーザーID
            role: メッセージの送信者（'user' | 'assistant'）
            content: メッセージ本文

        Returns:
            ThreadMessage: 追加されたメッセージオブジェクト

        Raises:
            duckdb.Error: データベース操作に失敗した場合
        """
        message_id = str(uuid4())
        now = datetime.now(timezone.utc)

        try:
            # メッセージを追加
            self.conn.execute(
                """
                INSERT INTO messages (
                    message_id, thread_id, user_id, role, content, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (message_id, thread_id, user_id, role, content, now),
            )

            # スレッドのlast_message_atを更新
            self.conn.execute(
                """
                UPDATE threads
                SET last_message_at = ?
                WHERE thread_id = ?
                """,
                (now, thread_id),
            )

            self.conn.commit()
        except Exception:
            self.conn.rollback()
            raise

        logger.info(
            "Added message message_id=%s to thread_id=%s, role=%s",
            message_id,
            thread_id,
            role,
        )

        return ThreadMessage(
            message_id=message_id,
            thread_id=thread_id,
            user_id=user_id,
            role=role,
            content=content,
            created_at=now,
        )

    def get_threads(
        self, user_id: str, limit: int = 50, offset: int = 0
    ) -> tuple[list[Thread], int]:
        """ユーザーのスレッド一覧を取得します。

        最終メッセージ日時の降順で取得します。

        Args:
            user_id: ユーザーID
            limit: 1ページあたりの件数（デフォルト: 50）
            offset: オフセット（デフォルト: 0）

        Returns:
            tuple[list[Thread], int]: (スレッドのリスト, 総件数) のタプル

        Raises:
            duckdb.Error: データベース操作に失敗した場合

        Note:
            パフォーマンス最適化の検討:
            現在、message_countは毎回COUNT(messages.message_id)で集計しています。
            スレッド数が多く、各スレッドに大量のメッセージがある場合、
            threadsテーブルにmessage_countカラムを追加し非正規化することで
            クエリパフォーマンスを改善できる可能性があります。
            ただし、その場合はメッセージ追加時にthreadsテーブルも更新する
            必要があり、実装の複雑さとトレードオフになります。
            現時点（MVP段階）では、この実装で十分な性能が得られています。
        """
        # 総件数を取得
        result = self.conn.execute(
            """
            SELECT COUNT(*) FROM threads WHERE user_id = ?
            """,
            (user_id,),
        )
        total = result.fetchone()[0]

        # スレッド一覧を取得（last_message_at降順）
        result = self.conn.execute(
            """
            SELECT
                threads.thread_id,
                threads.user_id,
                threads.title,
                arg_max(messages.content, messages.created_at) AS preview,
                COUNT(messages.message_id) AS message_count,
                threads.created_at,
                threads.last_message_at
            FROM threads
            LEFT JOIN messages ON messages.thread_id = threads.thread_id
            WHERE threads.user_id = ?
            GROUP BY
                threads.thread_id,
                threads.user_id,
                threads.title,
                threads.created_at,
                threads.last_message_at
            ORDER BY threads.last_message_at DESC
            LIMIT ? OFFSET ?
            """,
            (user_id, limit, offset),
        )

        threads = [
            Thread(
                thread_id=row[0],
                user_id=row[1],
                title=row[2],
                preview=row[3][:THREAD_PREVIEW_MAX_LENGTH] if row[3] else None,
                message_count=row[4],
                created_at=row[5].replace(tzinfo=timezone.utc),
                last_message_at=row[6].replace(tzinfo=timezone.utc),
            )
            for row in result.fetchall()
        ]

        logger.debug(
            "Retrieved threads for user_id=%s, count=%s, total=%s",
            user_id,
            len(threads),
            total,
        )

        return threads, total

    def get_thread(self, thread_id: str) -> Thread | None:
        """スレッドを取得します。

        Args:
            thread_id: スレッドのUUID

        Returns:
            Thread | None: スレッドオブジェクト(存在しない場合はNone)

        Raises:
            duckdb.Error: データベース操作に失敗した場合
        """
        result = self.conn.execute(
            """
            SELECT
                threads.thread_id,
                threads.user_id,
                threads.title,
                arg_max(messages.content, messages.created_at) AS preview,
                COUNT(messages.message_id) AS message_count,
                threads.created_at,
                threads.last_message_at
            FROM threads
            LEFT JOIN messages ON messages.thread_id = threads.thread_id
            WHERE threads.thread_id = ?
            GROUP BY
                threads.thread_id,
                threads.user_id,
                threads.title,
                threads.created_at,
                threads.last_message_at
            """,
            (thread_id,),
        )

        row = result.fetchone()
        if row is None:
            logger.debug("Thread not found: thread_id=%s", thread_id)
            return None

        logger.debug("Retrieved thread: thread_id=%s", thread_id)
        return Thread(
            thread_id=row[0],
            user_id=row[1],
            title=row[2],
            preview=row[3][:THREAD_PREVIEW_MAX_LENGTH] if row[3] else None,
            message_count=row[4],
            created_at=row[5].replace(tzinfo=timezone.utc),
            last_message_at=row[6].replace(tzinfo=timezone.utc),
        )

    def get_messages(self, thread_id: str) -> list[ThreadMessage]:
        """スレッドのメッセージ一覧を取得します。

        作成日時の昇順で取得します。

        Args:
            thread_id: スレッドのUUID

        Returns:
            list[ThreadMessage]: メッセージのリスト（時系列順）

        Raises:
            duckdb.Error: データベース操作に失敗した場合
        """
        result = self.conn.execute(
            """
            SELECT message_id, thread_id, user_id, role, content, created_at
            FROM messages
            WHERE thread_id = ?
            ORDER BY created_at ASC
            """,
            (thread_id,),
        )

        messages = [
            ThreadMessage(
                message_id=row[0],
                thread_id=row[1],
                user_id=row[2],
                role=row[3],
                content=row[4],
                created_at=row[5].replace(tzinfo=timezone.utc),
            )
            for row in result.fetchall()
        ]

        logger.debug(
            "Retrieved messages for thread_id=%s, count=%s", thread_id, len(messages)
        )

        return messages
