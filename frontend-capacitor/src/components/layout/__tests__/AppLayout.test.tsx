/**
 * AppLayout コンポーネントのテスト
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppLayout } from '../AppLayout';
import { useChatStore } from '@/lib/store';

// Sidebarをモック
vi.mock('../../sidebar/Sidebar', () => ({
  Sidebar: () => <div data-testid="sidebar">Sidebar Mock</div>,
}));

describe('AppLayout', () => {
  let queryClient: QueryClient;
  const originalInnerWidth = window.innerWidth;

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

  afterEach(() => {
    // window.innerWidthを元に戻す
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: originalInnerWidth,
    });
  });

  const renderAppLayout = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <AppLayout>
          <div data-testid="main-content">Main Content</div>
        </AppLayout>
      </QueryClientProvider>,
    );
  };

  /**
   * タッチイベントをシミュレート
   */
  const simulateSwipe = (
    startX: number,
    endX: number,
    startY = 100,
    endY = 100,
  ) => {
    act(() => {
      const touchStart = new TouchEvent('touchstart', {
        touches: [{ clientX: startX, clientY: startY } as Touch],
      });
      document.dispatchEvent(touchStart);
    });

    act(() => {
      const touchMove = new TouchEvent('touchmove', {
        touches: [{ clientX: endX, clientY: endY } as Touch],
      });
      document.dispatchEvent(touchMove);
    });

    act(() => {
      const touchEnd = new TouchEvent('touchend', {
        touches: [],
      });
      document.dispatchEvent(touchEnd);
    });
  };

  it('レイアウトが正しくレンダリングされる', () => {
    renderAppLayout();

    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('main-content')).toBeInTheDocument();
  });

  describe('スワイプジェスチャー（モバイル）', () => {
    beforeEach(() => {
      // モバイル画面サイズに設定
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 375, // iPhone SE サイズ
      });
    });

    it('右スワイプでサイドバーが開く', () => {
      renderAppLayout();

      expect(useChatStore.getState().sidebarOpen).toBe(false);

      // 左端から右にスワイプ（50px以上移動）
      simulateSwipe(10, 80, 100, 100);

      expect(useChatStore.getState().sidebarOpen).toBe(true);
    });

    it('左スワイプでサイドバーが閉じる', () => {
      renderAppLayout();

      act(() => {
        useChatStore.setState({ sidebarOpen: true });
      });

      expect(useChatStore.getState().sidebarOpen).toBe(true);

      // 右から左にスワイプ（50px以上移動）
      simulateSwipe(200, 100, 100, 100);

      expect(useChatStore.getState().sidebarOpen).toBe(false);
    });

    it('サイドバーが閉じている状態で左スワイプしても何も起こらない', () => {
      renderAppLayout();

      expect(useChatStore.getState().sidebarOpen).toBe(false);

      // 右から左にスワイプ
      simulateSwipe(200, 100, 100, 100);

      // 閉じたままである
      expect(useChatStore.getState().sidebarOpen).toBe(false);
    });

    it('短い距離のスワイプでは反応しない', () => {
      renderAppLayout();

      expect(useChatStore.getState().sidebarOpen).toBe(false);

      // 短い距離（50px未満）のスワイプ
      simulateSwipe(10, 40, 100, 100);

      expect(useChatStore.getState().sidebarOpen).toBe(false);
    });

    it('垂直方向の移動が大きいとスワイプとして認識されない', () => {
      renderAppLayout();

      expect(useChatStore.getState().sidebarOpen).toBe(false);

      // 水平方向に十分な距離があるが、垂直方向の移動が大きい（100px超）
      simulateSwipe(10, 80, 50, 200);

      // サイドバーは開かない
      expect(useChatStore.getState().sidebarOpen).toBe(false);
    });
  });

  describe('スワイプジェスチャー（デスクトップ）', () => {
    beforeEach(() => {
      // デスクトップ画面サイズに設定
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 1024,
      });
    });

    it('右スワイプしてもサイドバー状態は変わらない', () => {
      renderAppLayout();

      expect(useChatStore.getState().sidebarOpen).toBe(true);

      // 右スワイプ
      simulateSwipe(10, 80, 100, 100);

      // デスクトップではスワイプで開かない
      expect(useChatStore.getState().sidebarOpen).toBe(true);
    });

    it('左スワイプしてもサイドバー状態は変わらない', () => {
      useChatStore.setState({ sidebarOpen: true });
      renderAppLayout();

      expect(useChatStore.getState().sidebarOpen).toBe(true);

      // 左スワイプ
      simulateSwipe(200, 100, 100, 100);

      // デスクトップではスワイプで閉じない
      expect(useChatStore.getState().sidebarOpen).toBe(true);
    });
  });
});
