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
  thread_id?: string | null;
  model_name?: string;
}

/**
 * チャットレスポンス
 */
export interface ChatResponse {
  id: string;
  message: Message;
  tool_calls?: ToolCall[] | null;
  usage?: Record<string, unknown> | null;
  thread_id: string;
  model_name?: string;
}

/**
 * クライアント用拡張メッセージ型（UI状態管理用）
 */
export interface ChatMessage extends Message {
  id: string;
  timestamp: Date;
  isLoading?: boolean;
  isError?: boolean;
  model_name?: string;
}

/**
 * スレッド情報
 */
export interface Thread {
  thread_id: string;
  user_id: string;
  title: string;
  preview: string | null;
  message_count: number;
  created_at: string;
  last_message_at: string;
}

/**
 * スレッド一覧レスポンス
 */
export interface ThreadListResponse {
  threads: Thread[];
  total: number;
  limit: number;
  offset: number;
}

/**
 * スレッドメッセージ
 * 注: チャット履歴として保存されたメッセージはcontentが常に存在します（null不可）
 */
export interface ThreadMessage {
  message_id: string;
  thread_id: string;
  role: 'user' | 'assistant'; // システムメッセージは履歴に保存されない
  content: string; // 常に存在（Message型のようにnullableではない）
  created_at: string;
  model_name?: string | null; // 使用したLLMモデル名（assistantメッセージのみ）
}

/**
 * スレッドメッセージ一覧レスポンス
 */
export interface ThreadMessagesResponse {
  thread_id: string;
  // Backendはtotalを返さないため、必要になったらAPI拡張する
  messages: ThreadMessage[];
}

/**
 * API エラーレスポンス
 */
export interface ApiError {
  detail: string;
}

/**
 * LLMモデル情報
 */
export interface LLMModel {
  id: string;
  name: string;
  provider: string;
  input_cost_per_1m: number;
  output_cost_per_1m: number;
  is_free: boolean;
}

/**
 * モデル一覧レスポンス
 */
export interface ModelsResponse {
  models: LLMModel[];
  default_model: string;
}
