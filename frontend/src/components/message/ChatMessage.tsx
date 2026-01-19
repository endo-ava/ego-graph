import { memo } from 'react';
import { cn } from '@/lib/utils';
import type { ChatMessage as ChatMessageType } from '@/types/chat';
import { MessageAvatar } from '@/components/message/MessageAvatar';
import { MessageContent } from '@/components/message/MessageContent';

interface ChatMessageProps {
  message: ChatMessageType;
  isStreaming?: boolean;
}

function ChatMessage({ message, isStreaming = false }: ChatMessageProps) {
  const isUser = message.role === 'user';
  const isError = message.isError ?? false;
  const isLoading = message.isLoading ?? false;
  const showCursor = !isUser && !isError && isStreaming && !isLoading;

  return (
    <article
      className={cn(
        'flex w-full gap-3 px-4 py-6',
        isUser ? 'bg-background' : 'bg-muted/30',
      )}
      aria-label={`Message from ${isUser ? 'you' : 'assistant'}`}
    >
      <MessageAvatar isUser={isUser} />
      <MessageContent
        isUser={isUser}
        modelName={message.model_name ?? undefined}
        timestamp={message.timestamp}
        isError={isError}
        isLoading={isLoading}
        content={message.content ?? undefined}
        showCursor={showCursor}
      />
    </article>
  );
}

export default memo(ChatMessage, (prevProps, nextProps) => {
  return (
    prevProps.message.id === nextProps.message.id &&
    prevProps.message.content === nextProps.message.content &&
    prevProps.message.isError === nextProps.message.isError &&
    prevProps.message.isLoading === nextProps.message.isLoading &&
    prevProps.message.model_name === nextProps.message.model_name &&
    prevProps.message.timestamp === nextProps.message.timestamp &&
    prevProps.message.role === nextProps.message.role &&
    prevProps.isStreaming === nextProps.isStreaming
  );
});
