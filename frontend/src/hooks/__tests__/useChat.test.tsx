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

    // APIモック（ストリーミング）
    async function* mockStream() {
      yield { type: 'delta' as const, delta: 'Hello! How can I help you?' };
      yield {
        type: 'done' as const,
        finish_reason: 'end_turn',
        usage: { prompt_tokens: 10, completion_tokens: 5, total_tokens: 15 },
        thread_id: 'thread-1',
      };
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

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

    // ローディング中のアシスタントメッセージ（まだデルタが来てないためisLoading: false）
    expect(result.current.messages[1]).toMatchObject({
      role: 'assistant',
      content: 'Hello! How can I help you?',
      isLoading: false,
    });

    // メッセージ総数は2つのまま（更新のみ、追加されない）
    expect(result.current.messages).toHaveLength(2);
  });

  it('handles API error with 501 status', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useChat(), { wrapper });

    // 501エラー（LLM設定不足）をモック
    async function* mockStream() {
      throw new api.ApiRequestError(501, 'LLM provider not configured');
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    act(() => {
      result.current.sendMessage('Test message');
    });

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
    async function* mockStream() {
      throw new api.ApiRequestError(504, 'Request timeout');
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    act(() => {
      result.current.sendMessage('Test message');
    });

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
    async function* mockStream() {
      throw new api.ApiRequestError(502, 'OpenAI API rate limit exceeded');
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    act(() => {
      result.current.sendMessage('Test message');
    });

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
    async function* mockStream() {
      throw new api.ApiRequestError(401, 'Unauthorized');
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    act(() => {
      result.current.sendMessage('Test message');
    });

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
    async function* mockStream() {
      throw new Error('Network error');
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    act(() => {
      result.current.sendMessage('Test message');
    });

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
    async function* mockStream() {
      yield { type: 'delta' as const, delta: 'Response' };
      yield { type: 'done' as const, thread_id: 'thread-1' };
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    // メッセージ送信
    act(() => {
      result.current.sendMessage('Hello');
    });

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

    // APIモック - 1回目のストリーム
    async function* mockStream1() {
      yield { type: 'delta' as const, delta: 'Response 1' };
      yield { type: 'done' as const, thread_id: 'thread-1' };
    }
    const sendChatMessageSpy = vi
      .spyOn(api, 'sendChatMessageStream')
      .mockReturnValueOnce(mockStream1());

    // 最初のメッセージ送信
    act(() => {
      result.current.sendMessage('First message');
    });

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
      stream: true,
    });

    // 2つ目のメッセージ送信（会話履歴含む）
    async function* mockStream2() {
      yield { type: 'delta' as const, delta: 'Response 2' };
      yield { type: 'done' as const, thread_id: 'thread-1' };
    }
    sendChatMessageSpy.mockReturnValueOnce(mockStream2());

    act(() => {
      result.current.sendMessage('Second message');
    });

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
    async function* mockStream() {
      await new Promise((resolve) => setTimeout(resolve, 100));
      yield { type: 'delta' as const, delta: 'Response' };
      yield { type: 'done' as const, thread_id: 'thread-1' };
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    // メッセージ送信
    act(() => {
      result.current.sendMessage('Hello');
    });

    // isLoadingがtrueになることを確認
    await waitFor(() => {
      expect(result.current.isLoading).toBe(true);
    });

    // API完了後、isLoadingがfalseになることを確認
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });
  });

  it('チャット送信時にmodel_nameがリクエストに含まれる', async () => {
    const wrapper = createWrapper();

    // ストアに選択モデルを設定
    useChatStore.setState({
      selectedModel: 'test-model-id',
    });

    async function* mockStream() {
      yield { type: 'delta' as const, delta: 'Response' };
      yield { type: 'done' as const, thread_id: 'thread-1' };
    }
    const sendChatMessageSpy = vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    const { result } = renderHook(() => useChat(), { wrapper });

    // メッセージ送信
    act(() => {
      result.current.sendMessage('Test message');
    });

    await waitFor(() => {
      expect(sendChatMessageSpy).toHaveBeenCalledTimes(1);
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // model_nameがリクエストに含まれることを確認
    const callArgs = sendChatMessageSpy.mock.calls[0]?.[0];
    expect(callArgs).toMatchObject({
      messages: [
        {
          role: 'user',
          content: 'Test message',
        },
      ],
      stream: true,
      model_name: 'test-model-id',
    });
  });

  it('レスポンスのmodel_nameがメッセージに保存される', async () => {
    const wrapper = createWrapper();

    // ストアに選択モデルを設定
    useChatStore.setState({
      selectedModel: 'gpt-4',
    });

    const { result } = renderHook(() => useChat(), { wrapper });

    // APIレスポンスをモック
    async function* mockStream() {
      yield { type: 'delta' as const, delta: 'Hello! How can I help you?' };
      yield { type: 'done' as const, thread_id: 'thread-1' };
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    // メッセージ送信
    act(() => {
      result.current.sendMessage('Hello');
    });

    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    // レスポンスのmodel_nameがメッセージに保存される
    await waitFor(() => {
      expect(result.current.messages[1]).toMatchObject({
        role: 'assistant',
        content: 'Hello! How can I help you?',
        model_name: 'gpt-4',
        isLoading: false,
      });
    });
  });

  it('楽観的更新時にselectedModelがメッセージに設定される', async () => {
    const wrapper = createWrapper();

    // ストアに選択モデルを設定
    useChatStore.setState({
      selectedModel: 'local-model',
    });

    // APIモック（遅延あり）
    async function* mockStream() {
      await new Promise((resolve) => setTimeout(resolve, 100));
      yield { type: 'delta' as const, delta: 'Response' };
      yield { type: 'done' as const, thread_id: 'thread-1' };
    }
    vi.spyOn(api, 'sendChatMessageStream').mockReturnValue(mockStream());

    const { result } = renderHook(() => useChat(), { wrapper });

    // メッセージ送信
    act(() => {
      result.current.sendMessage('Hello');
    });

    // 楽観的更新でアシスタントメッセージが追加される
    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
    });

    // 楽観的更新時のアシスタントメッセージにmodel_nameが設定される
    expect(result.current.messages[1]).toMatchObject({
      role: 'assistant',
      content: '',
      isLoading: true,
      model_name: 'local-model',
    });

    // API完了を待つ
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });
  });
});
