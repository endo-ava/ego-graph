/**
 * アプリケーション全体のレイアウトコンポーネント
 * サイドバーとメインコンテンツエリアを管理します
 */

import { ReactNode, useEffect } from 'react';
import { useChatStore } from '@/lib/store';
import { useSwipe } from '@/hooks/useSwipe';
import { Sidebar } from '@/components/sidebar/Sidebar';

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { sidebarOpen, setSidebarOpen } = useChatStore();

  useEffect(() => {
    const handleResize = () => {
      setSidebarOpen(window.innerWidth >= 768);
    };

    handleResize();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [setSidebarOpen]);

  // スワイプジェスチャーでサイドバー開閉
  useSwipe({
    onSwipeRight: () => {
      // モバイルでのみ右スワイプでサイドバーを開く
      if (window.innerWidth < 768) {
        setSidebarOpen(true);
      }
    },
    onSwipeLeft: () => {
      // モバイルでサイドバーが開いている場合、左スワイプで閉じる
      if (window.innerWidth < 768 && sidebarOpen) {
        setSidebarOpen(false);
      }
    },
  });

  return (
    <div className="flex h-[100dvh] overflow-hidden">
      {/* サイドバー */}
      <Sidebar />

      {/* メインコンテンツエリア */}
      <div className="flex-1 flex flex-col min-w-0">{children}</div>
    </div>
  );
}
