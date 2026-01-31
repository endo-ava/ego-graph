import { useRef, useCallback } from 'react';
import { useShallow } from 'zustand/shallow';
import { toast } from 'sonner';
import { useChatStore } from '@/lib/store';
import { useIsMobile } from '@/hooks/ui/useIsMobile';
import { getThreadMessages } from '@/lib/api';
import type { ChatMessage } from '@/types/chat';

export function useThreadSelection() {
  const {
    setCurrentThreadId,
    setMessages,
    setSidebarOpen,
    setSelectedModel,
    setActiveView,
    activeView,
  } = useChatStore(
    useShallow((state) => ({
      setCurrentThreadId: state.setCurrentThreadId,
      setMessages: state.setMessages,
      setSidebarOpen: state.setSidebarOpen,
      setSelectedModel: state.setSelectedModel,
      setActiveView: state.setActiveView,
      activeView: state.activeView,
    })),
  );
  const abortControllerRef = useRef<AbortController | null>(null);
  const isMobile = useIsMobile();

  const selectThread = useCallback(
    async (threadId: string) => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }

      const controller = new AbortController();
      abortControllerRef.current = controller;
      const previousView = activeView;

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

        setActiveView('chat');

        if (isMobile) {
          setSidebarOpen(false);
        }
      } catch (error) {
        if (error instanceof Error && error.name === 'AbortError') {
          return;
        }
        setActiveView(previousView);
        console.error('Failed to load thread messages:', error);
        toast.error('スレッドの読み込みに失敗しました');
      }
    },
    [
      activeView,
      isMobile,
      setActiveView,
      setCurrentThreadId,
      setMessages,
      setSidebarOpen,
      setSelectedModel,
    ],
  );

  return { selectThread };
}
