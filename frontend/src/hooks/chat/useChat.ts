/**
 * チャット機能のカスタムフック
 * TanStack Query と Zustand を組み合わせて使用します
 */

import { useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useShallow } from 'zustand/shallow';
import { useChatStore } from '@/lib/store';
import { sendChatMessageStream, ApiRequestError } from '@/lib/api';
import { toast } from 'sonner';
import type { ChatMessage, Message } from '@/types/chat';

function getErrorMessage(error: unknown): string {
  let errorMessage = 'エラーが発生しました';
  if (error instanceof ApiRequestError) {
    if (error.status === 501) {
      errorMessage =
        'LLM設定が不足しています。バックエンドの設定を確認してください。';
    } else if (error.status === 504) {
      errorMessage =
        'リクエストがタイムアウトしました。しばらく待ってから再試行してください。';
    } else if (error.status === 502) {
      errorMessage = `LLM APIエラー: ${error.detail}`;
    } else if (error.status === 401) {
      errorMessage = '認証に失敗しました。API Keyを確認してください。';
    } else if (error.status === 400) {
      errorMessage = error.detail.startsWith('invalid_model_name:')
        ? '指定されたモデルは使用できません'
        : error.detail;
    } else {
      errorMessage = error.detail;
    }
  } else if (error instanceof Error) {
    errorMessage = error.message;
  }
  return errorMessage;
}

export function useChat() {
  const {
    messages,
    currentThreadId,
    addMessage,
    updateLastMessage,
    updateLastMessageWithModel,
    setLastMessageError,
    clearMessages,
    setCurrentThreadId,
    selectedModel,
  } = useChatStore(
    useShallow((state) => ({
      messages: state.messages,
      currentThreadId: state.currentThreadId,
      addMessage: state.addMessage,
      updateLastMessage: state.updateLastMessage,
      updateLastMessageWithModel: state.updateLastMessageWithModel,
      setLastMessageError: state.setLastMessageError,
      clearMessages: state.clearMessages,
      setCurrentThreadId: state.setCurrentThreadId,
      selectedModel: state.selectedModel,
    })),
  );
  const queryClient = useQueryClient();

  const [isLoading, setIsLoading] = useState(false);

  const sendMessage = useCallback(
    async (content: string) => {
      const userMessage: ChatMessage = {
        id: `user-${Date.now()}`,
        role: 'user',
        content,
        timestamp: new Date(),
      };

      addMessage(userMessage);

      const apiMessages: Message[] = [
        ...messages.map((m: ChatMessage) => ({
          role: m.role,
          content: m.content,
        })),
        {
          role: 'user' as const,
          content,
        },
      ];

      const assistantMessage: ChatMessage = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: '',
        timestamp: new Date(),
        isLoading: true,
        model_name: selectedModel,
      };
      addMessage(assistantMessage);

      setIsLoading(true);

      let accumulatedContent = '';

      try {
        for await (const chunk of sendChatMessageStream({
          messages: apiMessages,
          stream: true,
          thread_id: currentThreadId,
          model_name: selectedModel,
        })) {
          if (chunk.type === 'delta' && chunk.delta) {
            accumulatedContent += chunk.delta;
            updateLastMessage(accumulatedContent);
          } else if (chunk.type === 'done') {
            updateLastMessageWithModel(
              accumulatedContent,
              selectedModel || null,
            );
            setLastMessageError(false);
            if (chunk.thread_id) {
              setCurrentThreadId(chunk.thread_id);
            }
            queryClient.invalidateQueries({ queryKey: ['threads'] });
          } else if (chunk.type === 'error') {
            setLastMessageError(true);
            updateLastMessage(chunk.error || 'エラーが発生しました');
          }
        }
      } catch (error) {
        const errorMessage = getErrorMessage(error);
        toast.error(errorMessage);
        setLastMessageError(true);

        if (accumulatedContent) {
          updateLastMessage(accumulatedContent);
        } else {
          updateLastMessage(errorMessage);
        }
      } finally {
        setIsLoading(false);
      }
    },
    [
      messages,
      currentThreadId,
      selectedModel,
      addMessage,
      updateLastMessage,
      updateLastMessageWithModel,
      setLastMessageError,
      setCurrentThreadId,
      queryClient,
    ],
  );

  return {
    messages,
    sendMessage,
    clearMessages,
    isLoading,
  };
}
