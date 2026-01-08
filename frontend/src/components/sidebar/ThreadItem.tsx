/**
 * スレッド一覧項目コンポーネント
 * 個別のスレッドを表示し、クリック時の履歴復元を担当します
 */

import { useChatStore } from '@/lib/store';
import { getThreadMessages } from '@/lib/api';
import type { Thread, ChatMessage } from '@/types/chat';

interface ThreadItemProps {
  thread: Thread;
}

export function ThreadItem({ thread }: ThreadItemProps) {
  const { setCurrentThreadId, setMessages, setSidebarOpen, currentThreadId } = useChatStore();

  const isActive = currentThreadId === thread.thread_id;

  const handleClick = async () => {
    try {
      // 1. スレッドIDを設定
      setCurrentThreadId(thread.thread_id);

      // 2. スレッドメッセージを取得
      const response = await getThreadMessages(thread.thread_id);

      // 3. メッセージを履歴に復元（ChatMessage形式に変換）
      const messages: ChatMessage[] = response.messages.map((msg) => ({
        id: msg.message_id,
        role: msg.role,
        content: msg.content,
        timestamp: new Date(msg.created_at),
      }));
      setMessages(messages);

      // 4. モバイルの場合はサイドバーを閉じる
      if (window.innerWidth < 768) {
        setSidebarOpen(false);
      }
    } catch (error) {
      console.error('Failed to load thread messages:', error);
      // エラーハンドリングは必要に応じて追加
    }
  };

  return (
    <button
      onClick={handleClick}
      className={`w-full text-left px-4 py-3 hover:bg-accent transition-colors ${
        isActive ? 'bg-accent' : ''
      }`}
      aria-label={`スレッド: ${thread.title}`}
      aria-current={isActive ? 'true' : undefined}
    >
      <div className="font-bold text-sm line-clamp-2 mb-1">{thread.title}</div>
    </button>
  );
}
