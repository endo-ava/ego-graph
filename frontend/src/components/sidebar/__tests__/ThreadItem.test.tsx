/**
 * ThreadItemコンポーネントのテスト
 * スレッド復元時のモデル選択を検証
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThreadItem } from '../ThreadItem';
import { useChatStore } from '@/lib/store';
import * as api from '@/lib/api';
import type { Thread, ThreadMessagesResponse } from '@/types/chat';

// localStorageのモック
const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    clear: () => {
      store = {};
    },
  };
})();

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

describe('ThreadItem', () => {
  const mockThread: Thread = {
    thread_id: 'thread-1',
    user_id: 'default_user',
    title: 'Test Thread',
    preview: 'Test preview',
    message_count: 4,
    created_at: '2024-01-01T00:00:00Z',
    last_message_at: '2024-01-01T01:00:00Z',
  };

  beforeEach(() => {
    // localStorageのモック
    Object.defineProperty(window, 'localStorage', {
      value: localStorageMock,
      writable: true,
    });
    localStorageMock.clear();

    // ストアをクリア
    useChatStore.setState({
      messages: [],
      currentThreadId: null,
      sidebarOpen: false,
      activeView: 'chat',
      selectedModel: 'tngtech/deepseek-r1t2-chimera:free',
    });

    // モックをリセット
    vi.restoreAllMocks();

    // matchMediaをデスクトップモードに設定
    mockMatchMedia(false);

    // window.innerWidthをデスクトップサイズに設定
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });
  });

  it('スレッドタイトルを表示する', () => {
    render(<ThreadItem thread={mockThread} />);

    expect(screen.getByText('Test Thread')).toBeInTheDocument();
  });

  it('クリック時にスレッドメッセージを取得する', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [
        {
          message_id: 'msg-1',
          thread_id: 'thread-1',
          role: 'user',
          content: 'Hello',
          created_at: '2024-01-01T00:00:00Z',
        },
        {
          message_id: 'msg-2',
          thread_id: 'thread-1',
          role: 'assistant',
          content: 'Hi there!',
          created_at: '2024-01-01T00:01:00Z',
          model_name: 'test-model',
        },
      ],
    };

    const getThreadMessagesSpy = vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);
    const user = userEvent.setup();

    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    // APIが呼ばれることを確認
    await waitFor(() => {
      expect(getThreadMessagesSpy).toHaveBeenCalledTimes(1);
      expect(getThreadMessagesSpy).toHaveBeenCalledWith('thread-1', expect.objectContaining({
        signal: expect.any(AbortSignal),
      }));
    });
  });

  it('スレッド選択時にチャット画面へ戻る', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);

    useChatStore.setState({
      activeView: 'system_prompt',
    });

    const user = userEvent.setup();
    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    await waitFor(() => {
      expect(useChatStore.getState().activeView).toBe('chat');
    });
  });

  it('スレッド復元時にメッセージ履歴が設定される', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [
        {
          message_id: 'msg-1',
          thread_id: 'thread-1',
          role: 'user',
          content: 'Hello',
          created_at: '2024-01-01T00:00:00Z',
        },
        {
          message_id: 'msg-2',
          thread_id: 'thread-1',
          role: 'assistant',
          content: 'Hi there!',
          created_at: '2024-01-01T00:01:00Z',
          model_name: 'test-model',
        },
      ],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);
    const user = userEvent.setup();

    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    // ストアのメッセージが更新される
    await waitFor(() => {
      const messages = useChatStore.getState().messages;
      expect(messages).toHaveLength(2);
    });

    const messages = useChatStore.getState().messages;

    // 1つ目のメッセージ
    expect(messages[0]).toMatchObject({
      id: 'msg-1',
      role: 'user',
      content: 'Hello',
    });

    // 2つ目のメッセージ
    expect(messages[1]).toMatchObject({
      id: 'msg-2',
      role: 'assistant',
      content: 'Hi there!',
      model_name: 'test-model',
    });
  });

  it('スレッド復元時に最後のアシスタントメッセージのmodel_nameが選択される', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [
        {
          message_id: 'msg-1',
          thread_id: 'thread-1',
          role: 'user',
          content: 'Hello',
          created_at: '2024-01-01T00:00:00Z',
        },
        {
          message_id: 'msg-2',
          thread_id: 'thread-1',
          role: 'assistant',
          content: 'Hi there!',
          created_at: '2024-01-01T00:01:00Z',
          model_name: 'model-1',
        },
        {
          message_id: 'msg-3',
          thread_id: 'thread-1',
          role: 'user',
          content: 'How are you?',
          created_at: '2024-01-01T00:02:00Z',
        },
        {
          message_id: 'msg-4',
          thread_id: 'thread-1',
          role: 'assistant',
          content: "I'm fine, thanks!",
          created_at: '2024-01-01T00:03:00Z',
          model_name: 'model-2',
        },
      ],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);
    const user = userEvent.setup();

    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    // 最後のアシスタントメッセージのmodel_nameが選択される
    await waitFor(() => {
      expect(useChatStore.getState().selectedModel).toBe('model-2');
    });

    // localStorageにも保存される
    expect(localStorageMock.getItem('selected_model')).toBe('model-2');
  });

  it('model_nameがnullの場合は選択モデルを変更しない', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [
        {
          message_id: 'msg-1',
          thread_id: 'thread-1',
          role: 'user',
          content: 'Hello',
          created_at: '2024-01-01T00:00:00Z',
        },
        {
          message_id: 'msg-2',
          thread_id: 'thread-1',
          role: 'assistant',
          content: 'Hi there!',
          created_at: '2024-01-01T00:01:00Z',
          model_name: null,
        },
      ],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);

    // 初期のモデルを設定
    useChatStore.setState({
      selectedModel: 'initial-model',
    });

    const user = userEvent.setup();
    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    await waitFor(() => {
      const messages = useChatStore.getState().messages;
      expect(messages).toHaveLength(2);
    });

    // 選択モデルが変更されない
    expect(useChatStore.getState().selectedModel).toBe('initial-model');
  });

  it('アシスタントメッセージがない場合は選択モデルを変更しない', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [
        {
          message_id: 'msg-1',
          thread_id: 'thread-1',
          role: 'user',
          content: 'Hello',
          created_at: '2024-01-01T00:00:00Z',
        },
      ],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);

    // 初期のモデルを設定
    useChatStore.setState({
      selectedModel: 'initial-model',
    });

    const user = userEvent.setup();
    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    await waitFor(() => {
      const messages = useChatStore.getState().messages;
      expect(messages).toHaveLength(1);
    });

    // 選択モデルが変更されない
    expect(useChatStore.getState().selectedModel).toBe('initial-model');
  });

  it('スレッドIDが設定される', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);
    const user = userEvent.setup();

    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    // スレッドIDが設定される
    await waitFor(() => {
      expect(useChatStore.getState().currentThreadId).toBe('thread-1');
    });
  });

  it('選択中のスレッドがハイライトされる', () => {
    // スレッドを選択状態にする
    useChatStore.setState({
      currentThreadId: 'thread-1',
    });

    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    expect(button).toHaveClass('bg-accent');
  });

  it('選択されていないスレッドはハイライトされない', () => {
    // 別のスレッドを選択状態にする
    useChatStore.setState({
      currentThreadId: 'other-thread',
    });

    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    expect(button).not.toHaveClass('bg-accent');
  });

  it('モバイル表示時にサイドバーが閉じられる', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);

    // matchMediaをモバイルモードに設定
    mockMatchMedia(true);

    // サイドバーを開く
    useChatStore.setState({
      sidebarOpen: true,
    });

    const user = userEvent.setup();
    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    // サイドバーが閉じられる
    await waitFor(() => {
      expect(useChatStore.getState().sidebarOpen).toBe(false);
    });
  });

  it('デスクトップ表示時にサイドバーは開いたまま', async () => {
    const mockResponse: ThreadMessagesResponse = {
      thread_id: 'thread-1',
      messages: [],
    };

    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockResponse);

    // デスクトップサイズに設定
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });

    // サイドバーを開く
    useChatStore.setState({
      sidebarOpen: true,
    });

    const user = userEvent.setup();
    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    await waitFor(() => {
      const messages = useChatStore.getState().messages;
      expect(messages).toHaveLength(0);
    });

    // サイドバーは開いたまま
    expect(useChatStore.getState().sidebarOpen).toBe(true);
  });

  it('エラー時にtoastを表示する', async () => {
    vi.spyOn(api, 'getThreadMessages').mockRejectedValue(new Error('Network error'));

    const user = userEvent.setup();
    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    await waitFor(() => {
      expect(useChatStore.getState().activeView).toBe('chat');
    });
  });

  it('AbortErrorは無視される', async () => {
    const abortError = new Error('Aborted');
    abortError.name = 'AbortError';

    vi.spyOn(api, 'getThreadMessages').mockRejectedValue(abortError);

    // alertのモック
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

    const user = userEvent.setup();
    render(<ThreadItem thread={mockThread} />);

    const button = screen.getByRole('button');
    await user.click(button);

    // アラートは表示されない
    await waitFor(() => {
      // 少し待つ
      return new Promise((resolve) => setTimeout(resolve, 100));
    });

    expect(alertSpy).not.toHaveBeenCalled();

    alertSpy.mockRestore();
  });
});
