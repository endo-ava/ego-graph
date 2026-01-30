/**
 * System Prompt API の型定義
 */

export type SystemPromptName = 'user' | 'identity' | 'soul' | 'tools';

export interface SystemPromptResponse {
  name: SystemPromptName;
  content: string;
}

export interface SystemPromptUpdateRequest {
  content: string;
}
