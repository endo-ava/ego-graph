/**
 * MessageListコンポーネント
 * react-virtuosoで仮想スクロールを実装
 */

import { useEffect, useRef } from 'react';
import { Virtuoso, VirtuosoHandle } from 'react-virtuoso';
import { ChatMessage } from './ChatMessage';
import type { ChatMessage as ChatMessageType } from '@/types/chat';

interface MessageListProps {
  messages: ChatMessageType[];
}

export function MessageList({ messages }: MessageListProps) {
  const virtuosoRef = useRef<VirtuosoHandle>(null);

  // 新規メッセージ追加時に自動スクロール
  useEffect(() => {
    if (messages.length > 0) {
      virtuosoRef.current?.scrollToIndex({
        index: messages.length - 1,
        behavior: 'smooth',
      });
    }
  }, [messages.length]);

  // メッセージがない場合は空状態を表示
  if (messages.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-center text-muted-foreground">
          <p className="text-lg font-semibold">Start a conversation</p>
          <p className="text-sm">Send a message to get started</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full">
      <Virtuoso
        ref={virtuosoRef}
        className="h-full"
        style={{ height: '100%' }}
        data={messages}
        itemContent={(_index, message) => (
          <ChatMessage key={message.id} message={message} />
        )}
        followOutput="smooth"
      />
    </div>
  );
}
