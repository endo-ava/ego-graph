/**
 * チャット機能の型定義
 * バックエンドAPI (backend/api/chat.py, backend/llm/models.py) と型を一致させています
 */

/**
 * メッセージの役割
 */
export type MessageRole = 'user' | 'assistant' | 'system';

/**
 * バックエンドAPIのメッセージ型
 */
export interface Message {
  role: MessageRole;
  content: string | null;
}

/**
 * ツール呼び出しリクエスト
 */
export interface ToolCall {
  id: string;
  name: string;
  parameters: Record<string, unknown>;
}

/**
 * チャットリクエスト
 */
export interface ChatRequest {
  messages: Message[];
  stream?: boolean;
}

/**
 * チャットレスポンス
 */
export interface ChatResponse {
  id: string;
  message: Message;
  tool_calls?: ToolCall[] | null;
  usage?: Record<string, unknown> | null;
}

/**
 * クライアント用拡張メッセージ型（UI状態管理用）
 */
export interface ChatMessage extends Message {
  id: string;
  timestamp: Date;
  isLoading?: boolean;
  isError?: boolean;
}

/**
 * API エラーレスポンス
 */
export interface ApiError {
  detail: string;
}
