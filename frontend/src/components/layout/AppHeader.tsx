/**
 * 共通ヘッダーコンポーネント
 * Chat/System Prompt ビューで使用される
 */

import { ReactNode } from 'react';
import { Button } from '@/components/ui/button';

interface AppHeaderProps {
  /** 中央に表示するタイトル */
  title: string;
  /** ハンバーガーメニューボタンのクリックハンドラ */
  onMenuClick: () => void;
  /** 右側に表示するアクション要素（オプション） */
  rightAction?: ReactNode;
}

export function AppHeader({ title, onMenuClick, rightAction }: AppHeaderProps) {
  return (
    <header className="relative flex shrink-0 items-center justify-between border-b bg-background px-4 pb-3 pt-[calc(0.75rem+env(safe-area-inset-top))]">
      {/* ハンバーガーメニューボタン */}
      <Button
        variant="ghost"
        size="icon"
        onClick={onMenuClick}
        className="md:hidden"
        aria-label="Open menu"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <line x1="3" y1="6" x2="21" y2="6" />
          <line x1="3" y1="12" x2="21" y2="12" />
          <line x1="3" y1="18" x2="21" y2="18" />
        </svg>
      </Button>

      {/* タイトル（中央配置） */}
      <h1 className="absolute left-1/2 -translate-x-1/2 max-w-[calc(100%-8rem)] truncate text-lg font-semibold tracking-tight">
        {title}
      </h1>

      {/* 右側アクションまたはスペーサー */}
      {rightAction ?? <div className="w-[52px]" />}
    </header>
  );
}
