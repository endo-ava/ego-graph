"""チャット履歴用のデータモデル。

スレッドとメッセージの構造を定義します。
"""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel

# 定数
THREAD_TITLE_MAX_LENGTH = 50
THREAD_PREVIEW_MAX_LENGTH = 50


class Thread(BaseModel):
    """チャットスレッド。

    Attributes:
        thread_id: スレッドのUUID
        user_id: ユーザーID（MVP: 固定値 "default_user"）
        title: スレッドのタイトル（初回メッセージの先頭THREAD_TITLE_MAX_LENGTH文字）
        preview: 最新メッセージのプレビュー（先頭THREAD_PREVIEW_MAX_LENGTH文字）
        message_count: スレッド内メッセージ数
        created_at: スレッド作成日時（UTC）
        last_message_at: 最終メッセージ日時（UTC）
    """

    thread_id: str
    user_id: str
    title: str
    preview: str | None
    message_count: int
    created_at: datetime
    last_message_at: datetime


class ThreadMessage(BaseModel):
    """スレッド内のメッセージ。

    Attributes:
        message_id: メッセージのUUID
        thread_id: 所属するスレッドのUUID
        user_id: ユーザーID
        role: メッセージの送信者（'user' | 'assistant'）
        content: メッセージ本文（チャット履歴では常に存在）
        created_at: メッセージ作成日時（UTC）
        model_name: 使用したLLMモデル名（assistantメッセージのみ、optinal）
    """

    message_id: str
    thread_id: str
    user_id: str
    role: Literal["user", "assistant"]
    content: str
    created_at: datetime
    model_name: str | None = None


class ThreadListResponse(BaseModel):
    """スレッド一覧のレスポンス。

    Attributes:
        threads: スレッドのリスト（last_message_at降順）
        total: 総スレッド数
        limit: 1ページあたりの件数
        offset: オフセット
    """

    threads: list[Thread]
    total: int
    limit: int
    offset: int


class ThreadMessagesResponse(BaseModel):
    """スレッドメッセージのレスポンス。

    Attributes:
        thread_id: スレッドのUUID
        messages: メッセージのリスト（created_at昇順）
    """

    thread_id: str
    messages: list[ThreadMessage]
