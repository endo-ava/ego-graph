/**
 * スレッド一覧項目コンポーネント
 * 個別のスレッドを表示し、クリック時の履歴復元を担当します
 */

import { useCallback, memo } from 'react';
import { useShallow } from 'zustand/shallow';
import { useChatStore } from '@/lib/store';
import { useThreadSelection } from '@/hooks/useThreadSelection';
import type { Thread } from '@/types/chat';

interface ThreadItemProps {
  thread: Thread;
}

function ThreadItemComponent({ thread }: ThreadItemProps) {
  const currentThreadId = useChatStore(
    useShallow((state) => state.currentThreadId),
  );
  const { selectThread } = useThreadSelection();

  const isActive = currentThreadId === thread.thread_id;

  const handleClick = useCallback(() => {
    selectThread(thread.thread_id);
  }, [selectThread, thread.thread_id]);

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

export const ThreadItem = memo(ThreadItemComponent);
