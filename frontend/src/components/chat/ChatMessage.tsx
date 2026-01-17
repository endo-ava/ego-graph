/**
 * ChatMessageコンポーネント
 * Markdownレンダリングとコードハイライトに対応
 */

import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { format } from 'date-fns';
import { cn } from '@/lib/utils';
import type { ChatMessage as ChatMessageType } from '@/types/chat';

interface ChatMessageProps {
  message: ChatMessageType;
  isStreaming?: boolean;
}

export function ChatMessage({ message, isStreaming = false }: ChatMessageProps) {
  const isUser = message.role === 'user';
  const isError = message.isError ?? false;
  const isLoading = message.isLoading ?? false;
  const showCursor = !isUser && !isError && isStreaming && !isLoading;

  return (
    <div
      className={cn(
        'flex w-full gap-3 px-4 py-6',
        isUser ? 'bg-background' : 'bg-muted/30',
      )}
    >
      {/* アバター */}
      <div
        className={cn(
          'flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold',
          isUser
            ? 'bg-primary text-primary-foreground'
            : 'bg-secondary text-secondary-foreground',
        )}
      >
        {isUser ? 'U' : 'A'}
      </div>

      {/* メッセージコンテンツ */}
      <div className="flex-1 space-y-2 overflow-hidden">
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold">
            {isUser ? 'You' : 'Assistant'}
            {!isUser && message.model_name && (
              <span className="ml-2 text-xs text-muted-foreground">
                ({message.model_name})
              </span>
            )}
          </span>
          <span className="text-xs text-muted-foreground">
            {format(message.timestamp, 'HH:mm')}
          </span>
        </div>

        {isLoading ? (
          <LoadingDots />
        ) : (
          <div
            className={cn(
              'prose prose-sm max-w-none dark:prose-invert',
              isError && 'text-destructive',
            )}
          >
            {message.content ? (
              <>
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    code(props) {
                      const { children, className, ...rest } = props;
                      const match = /language-(\w+)/.exec(className || '');
                      const language = match?.[1];
                      const isInline = !className && !match;

                      return !isInline && language ? (
                        <SyntaxHighlighter
                          style={vscDarkPlus}
                          language={language}
                          PreTag="div"
                        >
                          {String(children).replace(/\n$/, '')}
                        </SyntaxHighlighter>
                      ) : (
                        <code className={className} {...rest}>
                          {children}
                        </code>
                      );
                    },
                  }}
                >
                  {message.content}
                </ReactMarkdown>
                {showCursor && <span className="ml-1 animate-pulse">▋</span>}
              </>
            ) : (
              <p className="text-muted-foreground italic">No content</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * ローディングアニメーション（3つのドット）
 */
function LoadingDots() {
  return (
    <div className="flex items-center gap-1" aria-hidden="true">
      <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.3s]" />
      <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.15s]" />
      <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground" />
    </div>
  );
}
