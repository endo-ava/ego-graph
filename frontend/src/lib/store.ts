/**
 * Zustand チャット状態管理ストア
 * メッセージ履歴の管理を行います
 */

import { create } from 'zustand';
import type { ChatMessage } from '@/types/chat';

interface ChatStore {
  messages: ChatMessage[];
  currentThreadId: string | null;
  sidebarOpen: boolean;
  addMessage: (message: ChatMessage) => void;
  updateLastMessage: (content: string) => void;
  setLastMessageError: (isError: boolean) => void;
  clearMessages: () => void;
  setMessages: (messages: ChatMessage[]) => void;
  setCurrentThreadId: (threadId: string | null) => void;
  setSidebarOpen: (open: boolean) => void;
  toggleSidebar: () => void;
}

export const useChatStore = create<ChatStore>((set) => ({
  messages: [],
  currentThreadId: null,
  sidebarOpen: false,

  addMessage: (message) =>
    set((state) => ({
      messages: [...state.messages, message],
    })),

  updateLastMessage: (content) =>
    set((state) => {
      const messages = [...state.messages];
      const lastMessage = messages[messages.length - 1];
      if (lastMessage) {
        messages[messages.length - 1] = {
          ...lastMessage,
          content,
          isLoading: false,
        };
      }
      return { messages };
    }),

  setLastMessageError: (isError) =>
    set((state) => {
      const messages = [...state.messages];
      const lastMessage = messages[messages.length - 1];
      if (lastMessage) {
        messages[messages.length - 1] = {
          ...lastMessage,
          isError,
          isLoading: false,
        };
      }
      return { messages };
    }),

  clearMessages: () => set({ messages: [], currentThreadId: null }),

  setMessages: (messages) => set({ messages }),

  setCurrentThreadId: (threadId) => set({ currentThreadId: threadId }),

  setSidebarOpen: (open) => set({ sidebarOpen: open }),

  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
}));
