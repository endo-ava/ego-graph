import { useRef, useCallback } from 'react';
import { useShallow } from 'zustand/shallow';
import { useChatStore } from '@/lib/store';
import { getThreadMessages } from '@/lib/api';
import type { ChatMessage } from '@/types/chat';

export function useThreadSelection() {
  const {
    setCurrentThreadId,
    setMessages,
    setSidebarOpen,
    setSelectedModel,
  } = useChatStore(
    useShallow((state) => ({
      setCurrentThreadId: state.setCurrentThreadId,
      setMessages: state.setMessages,
      setSidebarOpen: state.setSidebarOpen,
      setSelectedModel: state.setSelectedModel,
    })),
  );
  const abortControllerRef = useRef<AbortController | null>(null);

  const selectThread = useCallback(async (threadId: string) => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      const response = await getThreadMessages(threadId, {
        signal: controller.signal,
      });

      setCurrentThreadId(threadId);

      const messages: ChatMessage[] = response.messages.map((msg) => ({
        id: msg.message_id,
        role: msg.role,
        content: msg.content,
        timestamp: new Date(msg.created_at),
        model_name: msg.model_name || undefined,
      }));
      setMessages(messages);

      const lastAssistantMessage = [...response.messages]
        .reverse()
        .find((msg) => msg.role === 'assistant');
      if (lastAssistantMessage?.model_name) {
        setSelectedModel(lastAssistantMessage.model_name);
      }

      if (window.innerWidth < 768) {
        setSidebarOpen(false);
      }
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        return;
      }
      console.error('Failed to load thread messages:', error);
      alert('スレッドの読み込みに失敗しました。もう一度試してください。');
    }
  }, [setCurrentThreadId, setMessages, setSidebarOpen, setSelectedModel]);

  return { selectThread };
}
