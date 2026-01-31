/**
 * MessageListコンポーネント
 * react-virtuosoで仮想スクロールを実装
 */

import { lazy, Suspense, useRef } from 'react';
import { Virtuoso, VirtuosoHandle } from 'react-virtuoso';
import type { ChatMessage as ChatMessageType } from '@/types/chat';
import { MessageSkeleton } from '@/components/message/MessageSkeleton';

import { WelcomeScreen } from '@/components/chat/WelcomeScreen';

const ChatMessage = lazy(() => import('@/components/message/ChatMessage'));

interface MessageListProps {
  messages: ChatMessageType[];
}

export function MessageList({ messages }: MessageListProps) {
  const virtuosoRef = useRef<VirtuosoHandle>(null);

  if (messages.length === 0) {
    return <WelcomeScreen />;
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
