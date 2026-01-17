/**
 * チャット機能のカスタムフック
 * TanStack Query と Zustand を組み合わせて使用します
 */

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useChatStore } from '@/lib/store';
import { sendChatMessage, sendChatMessageStream, ApiRequestError } from '@/lib/api';
import type { ChatMessage, Message } from '@/types/chat';

/**
 * エラーからユーザー向けメッセージを生成する
 */
function getErrorMessage(error: unknown): string {
  let errorMessage = 'エラーが発生しました';
  if (error instanceof ApiRequestError) {
    if (error.status === 501) {
      errorMessage = 'LLM設定が不足しています。バックエンドの設定を確認してください。';
    } else if (error.status === 504) {
      errorMessage = 'リクエストがタイムアウトしました。しばらく待ってから再試行してください。';
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
    } = useChatStore();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: sendChatMessage,
    onSuccess: (data) => {
      // アシスタントのメッセージを更新（model_nameも含めて更新）
      if (data.message.content) {
        updateLastMessageWithModel(data.message.content, data.model_name || null);
      }
      // スレッドIDを保存
      if (data.thread_id) {
        setCurrentThreadId(data.thread_id);
      }
      // スレッド一覧を無効化して再取得をトリガー
      queryClient.invalidateQueries({ queryKey: ['threads'] });
    },
    onError: (error) => {
      const errorMessage = getErrorMessage(error);
      // エラーメッセージを最後のメッセージとして設定
      updateLastMessage(errorMessage);
      setLastMessageError(true);
    },
  });

  const sendMessage = async (content: string) => {
    // APIリクエスト送信用のメッセージ配列を先に準備（楽観的更新前のmessagesを使用）
    const apiMessages: Message[] = [
      ...messages.map((m) => ({
        role: m.role,
        content: m.content,
      })),
      {
        role: 'user' as const,
        content,
      },
    ];

    // ユーザーメッセージを即座に追加（楽観的更新）
    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content,
      timestamp: new Date(),
    };
    addMessage(userMessage);

    // アシスタントメッセージのプレースホルダーを追加
    const assistantMessage: ChatMessage = {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isLoading: true,
      model_name: selectedModel,
    };
    addMessage(assistantMessage);

    // ストリーミングで API リクエスト送信
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
          // 完了
          updateLastMessageWithModel(accumulatedContent, selectedModel || null);
          setLastMessageError(false);
          // スレッドIDをキャプチャ
          if (chunk.thread_id) {
            setCurrentThreadId(chunk.thread_id);
          }
          // スレッド一覧を無効化して再取得をトリガー
          queryClient.invalidateQueries({ queryKey: ['threads'] });
        } else if (chunk.type === 'error') {
          setLastMessageError(true);
          updateLastMessage(chunk.error || 'エラーが発生しました');
        }
      }
    } catch (error) {
      // エラーハンドリング
      const errorMessage = getErrorMessage(error);
      setLastMessageError(true);
      updateLastMessage(errorMessage);
    }
  };

  return {
    messages,
    sendMessage,
    clearMessages,
    isLoading: mutation.isPending,
  };
}
