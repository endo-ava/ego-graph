/**
 * Zustand チャット状態管理ストア
 * メッセージ履歴の管理を行います
 */

import { create } from 'zustand';
import { toast } from 'sonner';
import type { ChatMessage } from '@/types/chat';

const STORAGE_KEY = 'selected_model';
const DEFAULT_MODEL = 'tngtech/deepseek-r1t2-chimera:free'; // フォールバック用（APIからデフォルトモデルを取得）

const loadSelectedModel = (): string => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored || DEFAULT_MODEL;
  } catch {
    return DEFAULT_MODEL;
  }
};

interface ChatStore {
  messages: ChatMessage[];
  currentThreadId: string | null;
  sidebarOpen: boolean;
  activeView: 'chat' | 'system_prompt';
  selectedModel: string;
  onSidebarClose: (() => void) | null;
  addMessage: (message: ChatMessage) => void;
  updateLastMessage: (content: string) => void;
  updateLastMessageWithModel: (content: string, modelName: string | null) => void;
  setLastMessageError: (isError: boolean) => void;
  clearMessages: () => void;
  setMessages: (messages: ChatMessage[]) => void;
  setCurrentThreadId: (threadId: string | null) => void;
  setSidebarOpen: (open: boolean) => void;
  toggleSidebar: () => void;
  setActiveView: (view: 'chat' | 'system_prompt') => void;
  setSelectedModel: (modelName: string) => void;
  setOnSidebarClose: (callback: (() => void) | null) => void;
}

export const useChatStore = create<ChatStore>((set) => ({
  messages: [],
  currentThreadId: null,
  sidebarOpen: false,
  activeView: 'chat',
  selectedModel: loadSelectedModel(),
  onSidebarClose: null,

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

  updateLastMessageWithModel: (content, modelName) =>
    set((state) => {
      const messages = [...state.messages];
      const lastMessage = messages[messages.length - 1];
      if (lastMessage) {
        messages[messages.length - 1] = {
          ...lastMessage,
          content,
          isLoading: false,
          model_name: modelName || undefined,
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

  setActiveView: (view) => set({ activeView: view }),

  setSelectedModel: (modelName) => {
    try {
      localStorage.setItem(STORAGE_KEY, modelName);
    } catch {
      toast.error('モデル選択の保存に失敗しました');
    }
    set({ selectedModel: modelName });
  },

  setOnSidebarClose: (callback) => set({ onSidebarClose: callback }),
}));
