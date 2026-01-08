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
} from '@/types/chat';

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
