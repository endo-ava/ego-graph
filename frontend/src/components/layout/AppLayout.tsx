/**
 * アプリケーション全体のレイアウトコンポーネント
 * サイドバーとメインコンテンツエリアを管理します
 */

import { ReactNode, useEffect } from 'react';
import { useShallow } from 'zustand/shallow';
import { useChatStore } from '@/lib/store';
import { useSwipe } from '@/hooks/ui/useSwipe';
import { Sidebar } from '@/components/sidebar/Sidebar';

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { sidebarOpen, setSidebarOpen, onSidebarClose } = useChatStore(
    useShallow((state) => ({
      sidebarOpen: state.sidebarOpen,
      setSidebarOpen: state.setSidebarOpen,
      onSidebarClose: state.onSidebarClose,
    })),
  );

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

  useSwipe({
    onSwipeRight: () => {
      if (window.innerWidth < 768) {
        setSidebarOpen(true);
      }
    },
    onSwipeLeft: () => {
      if (window.innerWidth < 768 && sidebarOpen) {
        try {
          onSidebarClose?.();
        } finally {
          setSidebarOpen(false);
        }
      }
    },
  });

  return (
    <div className="flex h-[100dvh] overflow-hidden">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">{children}</div>
    </div>
  );
}
