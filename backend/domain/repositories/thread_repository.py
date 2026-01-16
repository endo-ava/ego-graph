"""Thread Repository Interface.

スレッド管理のリポジトリインターフェースを定義します。
"""

from abc import ABC, abstractmethod

from backend.domain.models.thread import Thread, ThreadMessage


class IThreadRepository(ABC):
    """スレッドリポジトリのインターフェース。

    チャット履歴のスレッドとメッセージに対するCRUD操作の抽象化を提供します。
    実装クラスはこのインターフェースを継承し、具体的なデータストア操作を実装します。
    """

    @abstractmethod
    def create_thread(self, user_id: str, first_message_content: str) -> Thread:
        """新規スレッドを作成する。

        Args:
            user_id: ユーザーID
            first_message_content: 初回メッセージの内容（タイトル生成に使用）

        Returns:
            Thread: 作成されたスレッドオブジェクト
        """
        pass

    @abstractmethod
    def add_message(
        self,
        thread_id: str,
        user_id: str,
        role: str,
        content: str,
        model_name: str | None = None,
    ) -> ThreadMessage:
        """スレッドにメッセージを追加する。

        Args:
            thread_id: スレッドのUUID
            user_id: ユーザーID
            role: メッセージの送信者（'user' | 'assistant'）
            content: メッセージ本文
            model_name: 使用したLLMモデル名（assistantメッセージのみ）

        Returns:
            ThreadMessage: 追加されたメッセージオブジェクト
        """
        pass

    @abstractmethod
    def get_thread(self, thread_id: str) -> Thread | None:
        """スレッドを取得する。

        Args:
            thread_id: スレッドのUUID

        Returns:
            Thread | None: スレッドオブジェクト（存在しない場合はNone）
        """
        pass

    @abstractmethod
    def get_threads(
        self, user_id: str, limit: int = 50, offset: int = 0
    ) -> tuple[list[Thread], int]:
        """ユーザーのスレッド一覧を取得する。

        最終メッセージ日時の降順で取得します。

        Args:
            user_id: ユーザーID
            limit: 1ページあたりの件数（デフォルト: 50）
            offset: オフセット（デフォルト: 0）

        Returns:
            tuple[list[Thread], int]: (スレッドのリスト, 総件数) のタプル
        """
        pass

    @abstractmethod
    def get_messages(self, thread_id: str) -> list[ThreadMessage]:
        """スレッドのメッセージ一覧を取得する。

        作成日時の昇順で取得します。

        Args:
            thread_id: スレッドのUUID

        Returns:
            list[ThreadMessage]: メッセージのリスト（時系列順）
        """
        pass
