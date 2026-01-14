/**
 * スレッド一覧項目コンポーネント
 * 個別のスレッドを表示し、クリック時の履歴復元を担当します
 */

import { useRef } from 'react';
import { useChatStore } from '@/lib/store';
import { getThreadMessages } from '@/lib/api';
import type { Thread, ChatMessage } from '@/types/chat';

interface ThreadItemProps {
  thread: Thread;
}

export function ThreadItem({ thread }: ThreadItemProps) {
  const {
    setCurrentThreadId,
    setMessages,
    setSidebarOpen,
    currentThreadId,
    setSelectedModel,
  } = useChatStore();
  const abortControllerRef = useRef<AbortController | null>(null);

  const isActive = currentThreadId === thread.thread_id;

  const handleClick = async () => {
    const clickedThreadId = thread.thread_id;

    // 前のリクエストがあればキャンセル
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    // 新しいAbortControllerを作成
    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      // 1. スレッドメッセージを取得
      const response = await getThreadMessages(clickedThreadId, { signal: controller.signal });

      // 2. 競合を防ぐため、レスポンス受信後にスレッドIDを設定
      setCurrentThreadId(clickedThreadId);

      // 3. メッセージを履歴に復元（ChatMessage形式に変換）
      const messages: ChatMessage[] = response.messages.map((msg) => ({
        id: msg.message_id,
        role: msg.role,
        content: msg.content,
        timestamp: new Date(msg.created_at),
        model_name: msg.model_name || undefined,
      }));
      setMessages(messages);

      // 4. 最後のアシスタントメッセージのモデル名を取得して選択モデルを設定
      const lastAssistantMessage = [...response.messages]
        .reverse()
        .find((msg) => msg.role === 'assistant');
      if (lastAssistantMessage?.model_name) {
        setSelectedModel(lastAssistantMessage.model_name);
      }
      // 古いスレッドで model_name が null の場合、選択モデルは変更しない（デフォルトモデルは ModelSelector で設定）

      // 5. モバイルの場合はサイドバーを閉じる
      if (window.innerWidth < 768) {
        setSidebarOpen(false);
      }
    } catch (error) {
      // AbortErrorは無視（意図的なキャンセル）
      if (error instanceof Error && error.name === 'AbortError') {
        return;
      }
      console.error('Failed to load thread messages:', error);
      alert('スレッドの読み込みに失敗しました。もう一度試してください。');
    }
  };

  return (
    <button
      onClick={handleClick}
      className={`w-full text-left px-4 py-3 hover:bg-accent transition-colors ${
        isActive ? 'bg-accent' : ''
      }`}
      aria-label={`スレッド: ${thread.title}`}
      aria-current={isActive ? 'page' : undefined}
    >
      <div className="font-bold text-sm line-clamp-2 mb-1">{thread.title}</div>
    </button>
  );
}
