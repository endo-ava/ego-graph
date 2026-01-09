/**
 * useThreads フックのテスト
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useThreads, useThreadMessages } from '../useThreads';
import * as api from '@/lib/api';
import type { ThreadListResponse, ThreadMessagesResponse } from '@/types/chat';

// モックデータ
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

const mockThreadMessagesResponse: ThreadMessagesResponse = {
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
    },
  ],
};

describe('useThreads', () => {
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

  it('スレッド一覧を取得できる', async () => {
    vi.spyOn(api, 'getThreads').mockResolvedValue(mockThreadsResponse);

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useThreads(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data?.pages[0]?.threads).toHaveLength(2);
    expect(result.current.data?.pages[0]?.threads[0]?.thread_id).toBe('thread-1');
  });

  it('次のページを取得できる（fetchNextPage）', async () => {
    const mockPage1: ThreadListResponse = {
      ...mockThreadsResponse,
      total: 40,
      offset: 0,
    };

    const mockPage2: ThreadListResponse = {
      threads: [
        {
          thread_id: 'thread-3',
          user_id: 'default_user',
          title: 'Test Thread 3',
          preview: 'Preview 3',
          message_count: 1,
          created_at: '2024-01-03T00:00:00Z',
          last_message_at: '2024-01-03T00:00:00Z',
        },
      ],
      total: 40,
      limit: 20,
      offset: 20,
    };

    vi.spyOn(api, 'getThreads')
      .mockResolvedValueOnce(mockPage1)
      .mockResolvedValueOnce(mockPage2);

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useThreads(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.hasNextPage).toBe(true);

    // 次のページを取得
    result.current.fetchNextPage();

    await waitFor(() => {
      expect(result.current.data?.pages).toHaveLength(2);
    });

    expect(result.current.data?.pages[1]?.threads[0]?.thread_id).toBe('thread-3');
  });

  it('エラー時に適切に処理される', async () => {
    vi.spyOn(api, 'getThreads').mockRejectedValue(new Error('Network error'));

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useThreads(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeDefined();
  });
});

describe('useThreadMessages', () => {
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

  it('スレッドメッセージを取得できる', async () => {
    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockThreadMessagesResponse);

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useThreadMessages('thread-1'), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data?.messages).toHaveLength(2);
    expect(result.current.data?.thread_id).toBe('thread-1');
  });

  it('threadIdがnullの場合はクエリが実行されない', () => {
    vi.spyOn(api, 'getThreadMessages').mockResolvedValue(mockThreadMessagesResponse);

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useThreadMessages(null), { wrapper });

    expect(result.current.data).toBeUndefined();
    expect(result.current.isLoading).toBe(false);
    expect(api.getThreadMessages).not.toHaveBeenCalled();
  });

  it('エラー時に適切に処理される', async () => {
    vi.spyOn(api, 'getThreadMessages').mockRejectedValue(new Error('Network error'));

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useThreadMessages('thread-1'), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeDefined();
  });
});
