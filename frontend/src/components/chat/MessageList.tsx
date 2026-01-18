/**
 * MessageListコンポーネント
 * react-virtuosoで仮想スクロールを実装
 */

import { lazy, Suspense, useRef } from 'react';
import { Virtuoso, VirtuosoHandle } from 'react-virtuoso';
import type { ChatMessage as ChatMessageType } from '@/types/chat';
import { MessageSkeleton } from '@/components/message/MessageSkeleton';

const ChatMessage = lazy(() => import('@/components/message/ChatMessage'));

interface MessageListProps {
  messages: ChatMessageType[];
}

export function MessageList({ messages }: MessageListProps) {
  const virtuosoRef = useRef<VirtuosoHandle>(null);
  const buildTime = import.meta.env.VITE_BUILD_TIME;

  if (messages.length === 0) {
    return (
      <section className="flex h-full items-center justify-center" aria-live="polite">
        <div className="text-center text-muted-foreground">
          <h2 className="text-lg font-semibold">Start a conversation</h2>
          <p className="text-sm">Send a message to get started</p>
          {buildTime ? (
            <p className="mt-2 text-xs">Last updated: {buildTime}</p>
          ) : null}
        </div>
      </section>
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
          <Suspense fallback={<MessageSkeleton />}>
            <ChatMessage key={message.id} message={message} />
          </Suspense>
        )}
        followOutput="smooth"
      />
    </div>
  );
}
