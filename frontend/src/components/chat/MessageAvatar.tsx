import { cn } from '@/lib/utils';

interface MessageAvatarProps {
  isUser: boolean;
}

export function MessageAvatar({ isUser }: MessageAvatarProps) {
  return (
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
  );
}
