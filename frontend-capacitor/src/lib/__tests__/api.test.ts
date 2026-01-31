/**
 * APIクライアントのテスト
 * fetch呼び出し, ヘッダー設定, エラーハンドリングを検証
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  sendChatMessage,
  ApiRequestError,
  getSystemPrompt,
  updateSystemPrompt,
} from '../api';
import type { ChatRequest, ChatResponse } from '@/types/chat';
import type { SystemPromptResponse } from '@/types/system_prompt';

// グローバルfetchのモック型定義
const mockFetch = vi.fn();

describe('sendChatMessage', () => {
  beforeEach(() => {
    // fetchをモック
    global.fetch = mockFetch;
    // 環境変数のモック
    vi.stubEnv('VITE_API_URL', 'http://localhost:8000');
    vi.stubEnv('VITE_API_KEY', '');
    vi.stubEnv('VITE_DEBUG', 'false');
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it('sends chat message successfully', async () => {
    const mockResponse: ChatResponse = {
      id: 'response-1',
      thread_id: 'thread-1',
      message: {
        role: 'assistant',
        content: 'Hello! How can I help you?',
      },
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    });

    const request: ChatRequest = {
      messages: [
        {
          role: 'user',
          content: 'Hello',
        },
      ],
      stream: false,
    };

    const result = await sendChatMessage(request);

    // レスポンス検証
    expect(result).toEqual(mockResponse);

    // fetch呼び出し検証
    expect(mockFetch).toHaveBeenCalledTimes(1);
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/v1/chat',
      expect.objectContaining({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      }),
    );
  });

  it('includes X-API-Key header when API key is set', async () => {
    // API Keyを設定
    vi.stubEnv('VITE_API_KEY', 'test_key_123');

    const mockResponse: ChatResponse = {
      id: 'response-1',
      thread_id: 'thread-1',
      message: {
        role: 'assistant',
        content: 'Response',
      },
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await sendChatMessage(request);

    // X-API-Keyヘッダーが含まれることを確認
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/v1/chat',
      expect.objectContaining({
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': 'test_key_123',
        },
      }),
    );
  });

  it('does not include X-API-Key header when API key is empty', async () => {
    // API Keyを空に設定
    vi.stubEnv('VITE_API_KEY', '');

    const mockResponse: ChatResponse = {
      id: 'response-1',
      thread_id: 'thread-1',
      message: {
        role: 'assistant',
        content: 'Response',
      },
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await sendChatMessage(request);

    // X-API-Keyヘッダーが含まれないことを確認
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/v1/chat',
      expect.objectContaining({
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    );
  });

  it('uses custom API URL from environment variable', async () => {
    // カスタムAPI URLを設定
    vi.stubEnv('VITE_API_URL', 'https://api.example.com');

    const mockResponse: ChatResponse = {
      id: 'response-1',
      thread_id: 'thread-1',
      message: {
        role: 'assistant',
        content: 'Response',
      },
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await sendChatMessage(request);

    // カスタムURLが使用されることを確認
    expect(mockFetch).toHaveBeenCalledWith(
      'https://api.example.com/v1/chat',
      expect.any(Object),
    );
  });

  it('throws ApiRequestError on 401 status', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({
        detail: 'Unauthorized',
      }),
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await expect(sendChatMessage(request)).rejects.toThrow(ApiRequestError);

    try {
      await sendChatMessage(request);
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(401);
      expect((error as ApiRequestError).detail).toBe('Unauthorized');
    }
  });

  it('throws ApiRequestError on 501 status (LLM not configured)', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 501,
      json: async () => ({
        detail: 'LLM provider not configured',
      }),
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await expect(sendChatMessage(request)).rejects.toThrow(ApiRequestError);

    try {
      await sendChatMessage(request);
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(501);
      expect((error as ApiRequestError).detail).toBe(
        'LLM provider not configured',
      );
    }
  });

  it('throws ApiRequestError on 504 status (timeout)', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 504,
      json: async () => ({
        detail: 'Gateway timeout',
      }),
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await expect(sendChatMessage(request)).rejects.toThrow(ApiRequestError);

    try {
      await sendChatMessage(request);
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(504);
      expect((error as ApiRequestError).detail).toBe('Gateway timeout');
    }
  });

  it('throws ApiRequestError on 502 status (LLM API error)', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 502,
      json: async () => ({
        detail: 'OpenAI API error',
      }),
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await expect(sendChatMessage(request)).rejects.toThrow(ApiRequestError);

    try {
      await sendChatMessage(request);
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(502);
      expect((error as ApiRequestError).detail).toBe('OpenAI API error');
    }
  });

  it('handles error response without detail field', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => {
        throw new Error('Invalid JSON');
      },
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await expect(sendChatMessage(request)).rejects.toThrow(ApiRequestError);

    try {
      await sendChatMessage(request);
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(500);
      expect((error as ApiRequestError).detail).toBe('HTTP 500');
    }
  });

  it('handles network error', async () => {
    mockFetch.mockRejectedValue(new Error('Network error'));

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await expect(sendChatMessage(request)).rejects.toThrow('Network error');
  });

  it('logs requests and responses when DEBUG is enabled', async () => {
    // DEBUGモードを有効化
    vi.stubEnv('VITE_DEBUG', 'true');

    const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

    const mockResponse: ChatResponse = {
      id: 'response-1',
      thread_id: 'thread-1',
      message: {
        role: 'assistant',
        content: 'Response',
      },
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    await sendChatMessage(request);

    // デバッグログが出力されることを確認
    expect(consoleSpy).toHaveBeenCalledWith(
      '[API] Sending chat request:',
      request,
    );
    expect(consoleSpy).toHaveBeenCalledWith('[API] Response:', mockResponse);

    consoleSpy.mockRestore();
  });

  it('logs errors when DEBUG is enabled', async () => {
    // DEBUGモードを有効化
    vi.stubEnv('VITE_DEBUG', 'true');

    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => {});

    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({
        detail: 'Internal server error',
      }),
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
    };

    try {
      await sendChatMessage(request);
    } catch {
      // エラーを無視
    }

    // エラーログが出力されることを確認
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      '[API] Request failed:',
      'Internal server error',
    );

    consoleErrorSpy.mockRestore();
  });

  it('sends stream parameter correctly', async () => {
    const mockResponse: ChatResponse = {
      id: 'response-1',
      thread_id: 'thread-1',
      message: {
        role: 'assistant',
        content: 'Response',
      },
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    });

    const request: ChatRequest = {
      messages: [{ role: 'user', content: 'Test' }],
      stream: true,
    };

    await sendChatMessage(request);

    // streamパラメータが正しく送信されることを確認
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/v1/chat',
      expect.objectContaining({
        body: JSON.stringify({
          messages: [{ role: 'user', content: 'Test' }],
          stream: true,
        }),
      }),
    );
  });
});

describe('system prompt API', () => {
  beforeEach(() => {
    global.fetch = mockFetch;
    vi.stubEnv('VITE_API_URL', 'http://localhost:8000');
    vi.stubEnv('VITE_API_KEY', 'test-key');
    vi.stubEnv('VITE_DEBUG', 'false');
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it('gets system prompt successfully', async () => {
    const response: SystemPromptResponse = {
      name: 'user',
      content: 'content',
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => response,
    });

    const result = await getSystemPrompt('user');

    expect(result).toEqual(response);
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/v1/system-prompts/user',
      expect.objectContaining({
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': 'test-key',
        },
      }),
    );
  });

  it('updates system prompt successfully', async () => {
    const response: SystemPromptResponse = {
      name: 'user',
      content: 'updated',
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => response,
    });

    const result = await updateSystemPrompt('user', { content: 'updated' });

    expect(result).toEqual(response);
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/v1/system-prompts/user',
      expect.objectContaining({
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': 'test-key',
        },
        body: JSON.stringify({ content: 'updated' }),
      }),
    );
  });

  it('throws ApiRequestError when system prompt request fails', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({
        detail: 'invalid_name: name must be one of user',
      }),
    });

    await expect(getSystemPrompt('user')).rejects.toThrow(ApiRequestError);

    try {
      await getSystemPrompt('user');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(400);
      expect((error as ApiRequestError).detail).toBe(
        'invalid_name: name must be one of user',
      );
    }
  });

  it('throws ApiRequestError when updateSystemPrompt fails with 400', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({
        detail: 'invalid_content: content must be <= 20000 characters',
      }),
    });

    await expect(
      updateSystemPrompt('user', { content: 'x'.repeat(30000) }),
    ).rejects.toThrow(ApiRequestError);

    try {
      await updateSystemPrompt('user', { content: 'x'.repeat(30000) });
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(400);
      expect((error as ApiRequestError).detail).toBe(
        'invalid_content: content must be <= 20000 characters',
      );
    }
  });

  it('throws ApiRequestError when updateSystemPrompt fails with 500', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({
        detail: 'Internal server error',
      }),
    });

    await expect(
      updateSystemPrompt('user', { content: 'test' }),
    ).rejects.toThrow(ApiRequestError);

    try {
      await updateSystemPrompt('user', { content: 'test' });
    } catch (error) {
      expect(error).toBeInstanceOf(ApiRequestError);
      expect((error as ApiRequestError).status).toBe(500);
      expect((error as ApiRequestError).detail).toBe('Internal server error');
    }
  });
});
