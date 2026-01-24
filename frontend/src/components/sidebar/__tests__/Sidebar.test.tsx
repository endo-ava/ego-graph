/**
 * Sidebar コンポーネントのテスト
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Sidebar } from '../Sidebar';
import { useChatStore } from '@/lib/store';

// ThreadListをモック
vi.mock('../ThreadList', () => ({
  ThreadList: () => <div data-testid="thread-list">Thread List Mock</div>,
}));

const DEFAULT_INNER_WIDTH = 1024;

const mockMatchMedia = (matches: boolean) => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => true,
    }),
  });
};

describe('Sidebar', () => {
  let queryClient: QueryClient;
  let originalInnerWidth: number;

  beforeEach(() => {
    originalInnerWidth = window.innerWidth;
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: DEFAULT_INNER_WIDTH,
    });
    mockMatchMedia(false);

    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    // ストアの初期化
    useChatStore.setState({ sidebarOpen: false, activeView: 'chat' });
  });

  afterEach(() => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: originalInnerWidth,
    });
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

    expect(screen.getByRole('complementary')).toBeInTheDocument();
    expect(screen.getByText('History')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'New chat' })).toBeInTheDocument();
    expect(screen.getByTestId('thread-list')).toBeInTheDocument();
  });

  it('New chatでチャット画面へ戻る', async () => {
    useChatStore.setState({ activeView: 'system_prompt' });
    const user = userEvent.setup();

    renderSidebar();

    await user.click(screen.getByRole('button', { name: 'New chat' }));

    expect(useChatStore.getState().activeView).toBe('chat');
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

  it('System Promptボタンで画面を切り替える', async () => {
    const user = userEvent.setup();

    renderSidebar();

    await user.click(screen.getByRole('button', { name: 'System Prompt' }));

    expect(useChatStore.getState().activeView).toBe('system_prompt');
  });

  it('System Promptボタンでモバイル時はサイドバーを閉じる', async () => {
    const user = userEvent.setup();

    mockMatchMedia(true);

    useChatStore.setState({ sidebarOpen: true });

    renderSidebar();

    await user.click(screen.getByRole('button', { name: 'System Prompt' }));

    expect(useChatStore.getState().sidebarOpen).toBe(false);
  });

});
