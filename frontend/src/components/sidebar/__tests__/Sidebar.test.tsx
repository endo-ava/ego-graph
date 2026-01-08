/**
 * Sidebar コンポーネントのテスト
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Sidebar } from '../Sidebar';
import { useChatStore } from '@/lib/store';

// ThreadListをモック
vi.mock('../ThreadList', () => ({
  ThreadList: () => <div data-testid="thread-list">Thread List Mock</div>,
}));

describe('Sidebar', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    // ストアの初期化
    useChatStore.setState({ sidebarOpen: false });
  });

  const renderSidebar = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <Sidebar />
      </QueryClientProvider>,
    );
  };

  it('サイドバーが正しくレンダリングされる', () => {
    renderSidebar();

    expect(screen.getByRole('complementary', { name: 'スレッド一覧' })).toBeInTheDocument();
    expect(screen.getByText('会話履歴')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '新規チャット' })).toBeInTheDocument();
    expect(screen.getByTestId('thread-list')).toBeInTheDocument();
  });

  it('デフォルトで閉じている状態（モバイル）', () => {
    renderSidebar();

    const sidebar = screen.getByRole('complementary');
    expect(sidebar).toHaveClass('-translate-x-full');
  });

  it('sidebarOpenがtrueのとき開いている', () => {
    useChatStore.setState({ sidebarOpen: true });
    renderSidebar();

    const sidebar = screen.getByRole('complementary');
    expect(sidebar).toHaveClass('translate-x-0');
  });

  it('sidebarOpenがtrueのときオーバーレイが表示される', () => {
    useChatStore.setState({ sidebarOpen: true });
    renderSidebar();

    // オーバーレイの存在確認（data-testidがないのでclassで確認）
    const overlays = document.querySelectorAll('.fixed.inset-0.bg-black\\/50');
    expect(overlays.length).toBeGreaterThan(0);
  });

  it('閉じるボタンが存在する', () => {
    useChatStore.setState({ sidebarOpen: true });
    renderSidebar();

    const closeButton = screen.getByRole('button', { name: 'サイドバーを閉じる' });
    expect(closeButton).toBeInTheDocument();
  });
});
