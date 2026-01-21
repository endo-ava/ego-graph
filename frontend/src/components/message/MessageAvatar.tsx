interface MessageAvatarProps {
  isUser: boolean;
}

export function MessageAvatar({ isUser }: MessageAvatarProps) {
  if (isUser) {
    // ユーザーアバター: 正方形
    return (
      <div
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-sm bg-secondary"
        aria-label="User"
        role="img"
      >
        <svg
          viewBox="0 0 24 24"
          xmlns="http://www.w3.org/2000/svg"
          className="h-4 w-4 text-secondary-foreground"
        >
          <rect x="4" y="4" width="16" height="16" fill="currentColor" />
        </svg>
      </div>
    );
  }

  // AIアバター: 六角形（サイバーシアン）
  return (
    <div
      className="relative flex h-8 w-8 shrink-0 items-center justify-center"
      aria-label="Assistant"
      role="img"
    >
      <svg
        viewBox="0 0 100 100"
        xmlns="http://www.w3.org/2000/svg"
        className="h-full w-full"
      >
        <path
          d="M50 5 L90 27.5 L90 72.5 L50 95 L10 72.5 L10 27.5 Z"
          fill="currentColor"
          className="text-accent [filter:drop-shadow(0_0_12px_oklch(from_var(--color-accent)_l_c_h_/_0.5))_drop-shadow(0_0_24px_oklch(from_var(--color-accent)_l_c_h_/_0.3))] dark:[filter:drop-shadow(0_0_16px_oklch(from_var(--color-accent)_l_c_h_/_0.6))_drop-shadow(0_0_32px_oklch(from_var(--color-accent)_l_c_h_/_0.4))]"
        />
      </svg>
    </div>
  );
}
