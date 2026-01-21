/**
 * ThreadList コンポーネントのテスト
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThreadList } from '../ThreadList';
import * as api from '@/lib/api';
import type { ThreadListResponse } from '@/types/chat';

// ThreadItemをモック
vi.mock('../ThreadItem', () => ({
  ThreadItem: ({ thread }: { thread: { thread_id: string; title: string } }) => (
    <div data-testid={`thread-item-${thread.thread_id}`}>{thread.title}</div>
  ),
}));

const mockThreadsResponse: ThreadListResponse = {
  threads: [
    {
      thread_id: 'thread-1',
      user_id: 'default_user',
      title: 'Test Thread 1',
      preview: 'Preview 1',
      message_count: 2,
      created_at: '2024-01-01T00:00:00Z',
      last_message_at: '2024-01-01T00:00:00Z',
    },
    {
      thread_id: 'thread-2',
      user_id: 'default_user',
      title: 'Test Thread 2',
      preview: 'Preview 2',
      message_count: 1,
      created_at: '2024-01-02T00:00:00Z',
      last_message_at: '2024-01-02T00:00:00Z',
    },
  ],
  total: 2,
  limit: 20,
  offset: 0,
};

describe('ThreadList', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    vi.clearAllMocks();
  });

  const renderThreadList = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <ThreadList />
      </QueryClientProvider>,
    );
  };

  it('ローディング状態を表示する', () => {
    vi.spyOn(api, 'getThreads').mockImplementation(
      () => new Promise(() => {}), // 永遠に解決しないPromise
    );

    renderThreadList();

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('スレッド一覧を正しく表示する', async () => {
    vi.spyOn(api, 'getThreads').mockResolvedValue(mockThreadsResponse);

    renderThreadList();

    await waitFor(() => {
      expect(screen.getByTestId('thread-item-thread-1')).toBeInTheDocument();
    });

    expect(screen.getByTestId('thread-item-thread-1')).toHaveTextContent('Test Thread 1');
    expect(screen.getByTestId('thread-item-thread-2')).toHaveTextContent('Test Thread 2');
  });

  it('空状態を表示する', async () => {
    vi.spyOn(api, 'getThreads').mockResolvedValue({
      threads: [],
      total: 0,
      limit: 20,
      offset: 0,
    });

    renderThreadList();

    await waitFor(() => {
      expect(
        screen.getByText('No conversations yet. Start a new chat'),
      ).toBeInTheDocument();
    });
  });

  it('エラー状態を表示する', async () => {
    vi.spyOn(api, 'getThreads').mockRejectedValue(new Error('Network error'));

    renderThreadList();

    await waitFor(() => {
      expect(screen.getByText('Failed to load threads')).toBeInTheDocument();
    });
  });

  it('無限スクロールの監視要素が存在する', async () => {
    vi.spyOn(api, 'getThreads').mockResolvedValue(mockThreadsResponse);

    const { container } = renderThreadList();

    await waitFor(() => {
      expect(screen.getByTestId('thread-item-thread-1')).toBeInTheDocument();
    });

    // aria-hidden="true"の要素を確認
    const observer = container.querySelector('[aria-hidden="true"]');
    expect(observer).toBeInTheDocument();
  });
});
