/**
 * Zustand チャット状態管理ストア
 * メッセージ履歴の管理を行います
 */

import { create } from 'zustand';
import type { ChatMessage } from '@/types/chat';

interface ChatStore {
  messages: ChatMessage[];
  addMessage: (message: ChatMessage) => void;
  updateLastMessage: (content: string) => void;
  setLastMessageError: (isError: boolean) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatStore>((set) => ({
  messages: [],

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

  clearMessages: () => set({ messages: [] }),
}));
