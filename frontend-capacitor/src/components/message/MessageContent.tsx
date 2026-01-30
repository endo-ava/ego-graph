import { cn } from '@/lib/utils';
import { format, isValid } from 'date-fns';
import { MarkdownContent } from '@/components/content/MarkdownContent';

export function MessageContent({
  isUser,
  modelName,
  timestamp,
  isError,
  isLoading,
  content,
  showCursor,
}: MessageContentProps) {
  const timeLabel = isValid(timestamp) ? format(timestamp, 'HH:mm') : '--';
  return (
    <div className="flex-1 space-y-2 overflow-hidden">
      <div className="flex items-center gap-2">
        {!isUser && modelName && (
          <span className="font-mono text-xs font-medium text-muted-foreground">
            {modelName}
          </span>
        )}
        <span className="font-mono text-xs text-muted-foreground">
          {timeLabel}
        </span>
      </div>
      <div
        className={cn(
          'prose prose-sm max-w-none dark:prose-invert',
          isError && 'text-destructive',
        )}
      >
        {isLoading ? (
          <>
            <span className="sr-only">Loading...</span>
            <LoadingDots />
          </>
        ) : content ? (
          <>
            <MarkdownContent content={content} />
            {showCursor && <span className="ml-1 animate-pulse">▋</span>}
          </>
        ) : (
          <p className="text-muted-foreground italic">No content</p>
        )}
      </div>
    </div>
  );
}
function LoadingDots() {
  return (
    <div className="flex items-center gap-1" aria-hidden="true">
      <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.3s]" />
      <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.15s]" />
      <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground" />
    </div>
  );
}
interface MessageContentProps {
  isUser: boolean;
  modelName?: string;
  timestamp: Date;
  isError: boolean;
  isLoading: boolean;
  content?: string | null;
  showCursor?: boolean;
}
