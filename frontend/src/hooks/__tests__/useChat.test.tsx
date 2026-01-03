/**
 * useChatフックのテスト
 * 楽観的更新, エラーハンドリング, メッセージ履歴管理を検証
 */

import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useChat } from '../useChat';
import { useChatStore } from '@/lib/store';
import * as api from '@/lib/api';
import type { ChatResponse } from '@/types/chat';

// TanStack Query Providerのラッパー
function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe('useChat', () => {
  beforeEach(() => {
    // 各テスト前にストアをクリア
    useChatStore.getState().clearMessages();
    // モックをリセット
    vi.restoreAllMocks();
  });

  it('initializes with empty messages', () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    expect(result.current.messages).toEqual([]);
    expect(result.current.isLoading).toBe(false);
  });

  it('sends message with optimistic update', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // APIモック（遅延を追加してローディング状態をテスト可能にする）
    const mockResponse: ChatResponse = {
      id: 'response-1',
      message: {
        role: 'assistant',
        content: 'Hello! How can I help you?',
      },
    };
    vi.spyOn(api, 'sendChatMessage').mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(mockResponse), 100)),
    );

    // メッセージ送信
    act(() => {
      result.current.sendMessage('Hello');
    });

    // 楽観的更新：ユーザーメッセージとローディング中のアシスタントメッセージが即座に追加される
    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    // ユーザーメッセージの検証
    expect(result.current.messages[0]).toMatchObject({
      role: 'user',
      content: 'Hello',
    });

    // ローディング中のアシスタントメッセージ
    expect(result.current.messages[1]).toMatchObject({
      role: 'assistant',
      content: '',
      isLoading: true,
    });

    // APIレスポンス後、アシスタントメッセージが更新される
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await waitFor(() => {
      expect(result.current.messages[1]).toMatchObject({
        role: 'assistant',
        content: 'Hello! How can I help you?',
        isLoading: false,
      });
    });

    // メッセージ総数は2つのまま（更新のみ、追加されない）
    expect(result.current.messages).toHaveLength(2);
  });

  it('handles API error with 501 status', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // 501エラー（LLM設定不足）をモック
    vi.spyOn(api, 'sendChatMessage').mockRejectedValue(
      new api.ApiRequestError(501, 'LLM provider not configured'),
    );

    result.current.sendMessage('Test message');

    // 楽観的更新でメッセージが追加される
    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    // エラー処理後、アシスタントメッセージがエラーメッセージに更新される
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await waitFor(() => {
      expect(result.current.messages[1]).toMatchObject({
        role: 'assistant',
        content: 'LLM設定が不足しています。バックエンドの設定を確認してください。',
        isError: true,
        isLoading: false,
      });
    });
  });

  it('handles API error with 504 status (timeout)', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // 504エラー（タイムアウト）をモック
    vi.spyOn(api, 'sendChatMessage').mockRejectedValue(
      new api.ApiRequestError(504, 'Request timeout'),
    );

    result.current.sendMessage('Test message');

    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await waitFor(() => {
      expect(result.current.messages[1]).toMatchObject({
        content: 'リクエストがタイムアウトしました。しばらく待ってから再試行してください。',
        isError: true,
      });
    });
  });

  it('handles API error with 502 status (LLM API error)', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // 502エラー（LLM APIエラー）をモック
    vi.spyOn(api, 'sendChatMessage').mockRejectedValue(
      new api.ApiRequestError(502, 'OpenAI API rate limit exceeded'),
    );

    result.current.sendMessage('Test message');

    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await waitFor(() => {
      expect(result.current.messages[1]?.content).toContain('LLM APIエラー');
      expect(result.current.messages[1]?.content).toContain(
        'OpenAI API rate limit exceeded',
      );
      expect(result.current.messages[1]?.isError).toBe(true);
    });
  });

  it('handles API error with 401 status (authentication)', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // 401エラー（認証失敗）をモック
    vi.spyOn(api, 'sendChatMessage').mockRejectedValue(
      new api.ApiRequestError(401, 'Unauthorized'),
    );

    result.current.sendMessage('Test message');

    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await waitFor(() => {
      expect(result.current.messages[1]).toMatchObject({
        content: '認証に失敗しました。API Keyを確認してください。',
        isError: true,
      });
    });
  });

  it('handles generic error', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // 一般的なエラーをモック
    vi.spyOn(api, 'sendChatMessage').mockRejectedValue(
      new Error('Network error'),
    );

    result.current.sendMessage('Test message');

    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    await waitFor(() => {
      expect(result.current.messages[1]).toMatchObject({
        content: 'Network error',
        isError: true,
      });
    });
  });

  it('clears all messages', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // APIモック
    const mockResponse: ChatResponse = {
      id: 'response-1',
      message: {
        role: 'assistant',
        content: 'Response',
      },
    };
    vi.spyOn(api, 'sendChatMessage').mockResolvedValue(mockResponse);

    // メッセージ送信
    result.current.sendMessage('Hello');

    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    // メッセージクリア
    act(() => {
      result.current.clearMessages();
    });

    await waitFor(() => {
      expect(result.current.messages).toEqual([]);
    });
  });

  it('sends conversation history with new message', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // APIモック
    const sendChatMessageSpy = vi
      .spyOn(api, 'sendChatMessage')
      .mockResolvedValue({
        id: 'response-1',
        message: {
          role: 'assistant',
          content: 'Response 1',
        },
      });

    // 最初のメッセージ送信
    result.current.sendMessage('First message');

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // APIが正しいリクエストで呼ばれたことを確認（第1引数のみをチェック）
    const firstCall = sendChatMessageSpy.mock.calls[0]?.[0];
    expect(firstCall).toMatchObject({
      messages: [
        {
          role: 'user',
          content: 'First message',
        },
      ],
      stream: false,
    });

    // 2つ目のメッセージ送信（会話履歴含む）
    sendChatMessageSpy.mockResolvedValue({
      id: 'response-2',
      message: {
        role: 'assistant',
        content: 'Response 2',
      },
    });

    result.current.sendMessage('Second message');

    await waitFor(() => {
      expect(sendChatMessageSpy).toHaveBeenCalledTimes(2);
    });

    // 会話履歴が含まれていることを確認
    const secondCall = sendChatMessageSpy.mock.calls[1]?.[0];
    expect(secondCall?.messages).toHaveLength(3); // 前回のuser + assistant + 今回のuser

    expect(secondCall?.messages).toEqual([
      {
        role: 'user',
        content: 'First message',
      },
      {
        role: 'assistant',
        content: 'Response 1',
      },
      {
        role: 'user',
        content: 'Second message',
      },
    ]);
  });

  it('sets isLoading to true during API request', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // APIモック（遅延あり）
    vi.spyOn(api, 'sendChatMessage').mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            resolve({
              id: 'response-1',
              message: {
                role: 'assistant',
                content: 'Response',
              },
            });
          }, 100);
        }),
    );

    // メッセージ送信
    result.current.sendMessage('Hello');

    // isLoadingがtrueになることを確認
    await waitFor(() => {
      expect(result.current.isLoading).toBe(true);
    });

    // API完了後、isLoadingがfalseになることを確認
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });
  });
});
