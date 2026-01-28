/**
 * バックエンドAPIクライアント
 * TanStack Query と統合して使用します
 */

import { QueryClient } from '@tanstack/react-query';
import type {
  ChatRequest,
  ChatResponse,
  ApiError,
  Thread,
  ThreadListResponse,
  ThreadMessagesResponse,
  ModelsResponse,
  StreamChunk,
} from '@/types/chat';
import type {
  SystemPromptName,
  SystemPromptResponse,
  SystemPromptUpdateRequest,
} from '@/types/system_prompt';

interface ApiConfig {
  apiUrl: string;
  apiKey: string;
  debug: boolean;
}

function getApiConfig(): ApiConfig {
  return {
    apiUrl: import.meta.env.VITE_API_URL || 'http://localhost:8000',
    apiKey: import.meta.env.VITE_API_KEY || '',
    debug: import.meta.env.VITE_DEBUG === 'true',
  };
}

/**
 * APIエラークラス
 */
export class ApiRequestError extends Error {
  constructor(
    public status: number,
    public detail: string,
  ) {
    super(`API Error ${status}: ${detail}`);
    this.name = 'ApiRequestError';
  }
}

/**
 * チャットAPIリクエスト
 */
export async function sendChatMessage(
  request: ChatRequest,
): Promise<ChatResponse> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  // API Keyが設定されている場合のみヘッダーに追加
  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Sending chat request:', request);
  }

  const response = await fetch(`${apiUrl}/v1/chat`, {
    method: 'POST',
    headers,
    body: JSON.stringify(request),
    mode: 'cors',
    credentials: 'omit', // 開発環境では認証情報を送らない
  });

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Request failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  const data = (await response.json()) as ChatResponse;

  if (debug) {
    console.log('[API] Response:', data);
  }

  return data;
}

/**
 * チャットAPIリクエスト（ストリーミング）
 */
export async function* sendChatMessageStream(
  request: ChatRequest,
): AsyncGenerator<StreamChunk, void> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  // API Keyが設定されている場合のみヘッダーに追加
  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Sending chat stream request:', request);
  }

  const response = await fetch(`${apiUrl}/v1/chat`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ...request, stream: true }),
    mode: 'cors',
    credentials: 'omit',
  });

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Request failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  // ReadableStream を読み取る
  if (!response.body) {
    throw new ApiRequestError(500, 'No response body');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();

    if (done) {
      // デコーダーをフラッシュして残りのバッファを処理
      if (buffer.trim()) {
        buffer += decoder.decode();
        const lines = buffer.split('\n\n');
        for (const line of lines) {
          if (!line.trim()) {
            continue;
          }
          const eventLines = line.split('\n');
          const dataLine = eventLines.find((l) => l.startsWith('data: '));
          if (dataLine) {
            try {
              const chunk = JSON.parse(dataLine.slice(6)) as StreamChunk;
              yield chunk;
            } catch (e) {
              console.error('[API] Failed to parse final chunk:', e);
            }
          }
        }
      }
      if (debug) {
        console.log('[API] Stream completed');
      }
      break;
    }

    // チャンクをデコード
    buffer += decoder.decode(value, { stream: true });

    // SSE イベントをパース
    const lines = buffer.split('\n\n');
    buffer = lines.pop() || ''; // 最後の不完全な行をバッファに戻す

    for (const line of lines) {
      if (!line.trim()) {
        continue;
      }

      // data: ... の形式
      const dataMatch = line.match(/^data: (.+)$/m);

      if (dataMatch && dataMatch[1]) {
        try {
          const data = JSON.parse(dataMatch[1]) as StreamChunk;
          if (debug && data.type !== 'delta') {
            console.log('[API] Stream chunk:', data);
          }
          yield data;

          // エラーが発生した場合
          if (data.type === 'error') {
            const errorMsg = data.error || 'Unknown error';
            if (debug) {
              console.error('[API] Stream error:', errorMsg);
            }
            throw new ApiRequestError(500, errorMsg);
          }
        } catch (e) {
          if (debug) {
            console.error('[API] Failed to parse SSE data:', line, e);
          }
          // エラーが ApiRequestError の場合は再スロー
          if (e instanceof ApiRequestError) {
            throw e;
          }
        }
      }
    }
  }
}

/**
 * モデル一覧取得
 */
export async function getModels(): Promise<ModelsResponse> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Fetching models');
  }

  const response = await fetch(`${apiUrl}/v1/chat/models`, {
    method: 'GET',
    headers,
    mode: 'cors',
    credentials: 'omit',
  });

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Get models failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  const data = (await response.json()) as ModelsResponse;

  if (debug) {
    console.log('[API] Models response:', data);
  }

  return data;
}

/**
 * スレッド一覧取得
 */
export async function getThreads(
  limit: number,
  offset: number,
): Promise<ThreadListResponse> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Fetching threads:', { limit, offset });
  }

  const response = await fetch(
    `${apiUrl}/v1/threads?limit=${limit}&offset=${offset}`,
    {
      method: 'GET',
      headers,
      mode: 'cors',
      credentials: 'omit',
    },
  );

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Get threads failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  const data = (await response.json()) as ThreadListResponse;

  if (debug) {
    console.log('[API] Threads response:', data);
  }

  return data;
}

/**
 * スレッド取得
 */
export async function getThread(threadId: string): Promise<Thread> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Fetching thread:', threadId);
  }

  const response = await fetch(
    `${apiUrl}/v1/threads/${encodeURIComponent(threadId)}`,
    {
      method: 'GET',
      headers,
      mode: 'cors',
      credentials: 'omit',
    },
  );

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Get thread failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  const data = (await response.json()) as Thread;

  if (debug) {
    console.log('[API] Thread response:', data);
  }

  return data;
}

/**
 * スレッドメッセージ取得
 */
export async function getThreadMessages(
  threadId: string,
  options?: { signal?: AbortSignal },
): Promise<ThreadMessagesResponse> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Fetching thread messages:', threadId);
  }

  const response = await fetch(
    `${apiUrl}/v1/threads/${encodeURIComponent(threadId)}/messages`,
    {
      method: 'GET',
      headers,
      mode: 'cors',
      credentials: 'omit',
      signal: options?.signal,
    },
  );

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Get thread messages failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  const data = (await response.json()) as ThreadMessagesResponse;

  if (debug) {
    console.log('[API] Thread messages response:', data);
  }

  return data;
}

/**
 * System Prompt取得
 */
export async function getSystemPrompt(
  name: SystemPromptName,
): Promise<SystemPromptResponse> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Fetching system prompt:', name);
  }

  const response = await fetch(`${apiUrl}/v1/system-prompts/${name}`, {
    method: 'GET',
    headers,
    mode: 'cors',
    credentials: 'omit',
  });

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Get system prompt failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  const data = (await response.json()) as SystemPromptResponse;

  if (debug) {
    console.log('[API] System prompt response:', data);
  }

  return data;
}

/**
 * System Prompt更新
 */
export async function updateSystemPrompt(
  name: SystemPromptName,
  request: SystemPromptUpdateRequest,
): Promise<SystemPromptResponse> {
  const { apiUrl, apiKey, debug } = getApiConfig();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  if (debug) {
    console.log('[API] Updating system prompt:', name);
  }

  const response = await fetch(`${apiUrl}/v1/system-prompts/${name}`, {
    method: 'PUT',
    headers,
    body: JSON.stringify(request),
    mode: 'cors',
    credentials: 'omit',
  });

  if (!response.ok) {
    let errorDetail = `HTTP ${response.status}`;
    try {
      const errorData = (await response.json()) as ApiError;
      errorDetail = errorData.detail || errorDetail;
    } catch {
      // JSON parseに失敗した場合はHTTPステータスをそのまま使用
    }

    if (debug) {
      console.error('[API] Update system prompt failed:', errorDetail);
    }

    throw new ApiRequestError(response.status, errorDetail);
  }

  const data = (await response.json()) as SystemPromptResponse;

  if (debug) {
    console.log('[API] System prompt update response:', data);
  }

  return data;
}

/**
 * TanStack Query クライアント設定
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 1000 * 60 * 5, // 5分
    },
    mutations: {
      retry: false,
    },
  },
});
