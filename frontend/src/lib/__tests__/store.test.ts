/**
 * useChatStoreのテスト
 * 状態管理, localStorage連携, メッセージ更新を検証
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useChatStore } from '../store';
import type { ChatMessage } from '@/types/chat';

// localStorageのモック
const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    clear: () => {
      store = {};
    },
  };
})();

describe('useChatStore', () => {
  beforeEach(() => {
    // localStorageのモック
    Object.defineProperty(window, 'localStorage', {
      value: localStorageMock,
      writable: true,
    });
    localStorageMock.clear();

    // ストアをリセット
    useChatStore.setState({
      messages: [],
      currentThreadId: null,
      sidebarOpen: false,
      selectedModel: 'tngtech/deepseek-r1t2-chimera:free',
    });
  });

  describe('selectedModel', () => {
    it('setSelectedModelで設定した値が反映される', () => {
      const store = useChatStore.getState();

      // モデルを設定
      store.setSelectedModel('custom-model-id');

      // ストアの状態を再取得
      expect(useChatStore.getState().selectedModel).toBe('custom-model-id');
    });

    it('localStorageに値がない場合、デフォルトモデルが使用される', () => {
      // localStorageをクリア
      localStorageMock.clear();

      // ストアをリセット
      useChatStore.setState({
        selectedModel: 'tngtech/deepseek-r1t2-chimera:free',
      });

      const store = useChatStore.getState();
      expect(store.selectedModel).toBe('tngtech/deepseek-r1t2-chimera:free');
    });

    it('setSelectedModelがlocalStorageに保存する', () => {
      const store = useChatStore.getState();

      store.setSelectedModel('new-model-id');

      // ストアが更新される
      expect(useChatStore.getState().selectedModel).toBe('new-model-id');

      // localStorageに保存される
      expect(localStorageMock.getItem('selected_model')).toBe('new-model-id');
    });

    it('localStorageから初期値を読み込む (persist middleware経由)', () => {
      localStorageMock.setItem('selected_model', 'saved-model-id');

      vi.resetModules();

      const { useChatStore: newUseChatStore } = require('../store');
      const store = newUseChatStore.getState();

      expect(store.selectedModel).toBe('saved-model-id');
    });
  });

  describe('updateLastMessageWithModel', () => {
    it('最後のメッセージを正しく更新する', () => {
      const store = useChatStore.getState();

      // メッセージを追加
      const message1: ChatMessage = {
        id: 'msg-1',
        role: 'user',
        content: 'Hello',
        timestamp: new Date(),
      };
      const message2: ChatMessage = {
        id: 'msg-2',
        role: 'assistant',
        content: '',
        timestamp: new Date(),
        isLoading: true,
      };

      store.addMessage(message1);
      store.addMessage(message2);

      // 最後のメッセージを更新
      store.updateLastMessageWithModel('Hi there!', 'test-model');

      const updatedMessages = useChatStore.getState().messages;

      // 最後のメッセージが更新される
      expect(updatedMessages).toHaveLength(2);
      expect(updatedMessages[1]).toMatchObject({
        id: 'msg-2',
        role: 'assistant',
        content: 'Hi there!',
        isLoading: false,
        model_name: 'test-model',
      });

      // 1つ目のメッセージは変更されない
      expect(updatedMessages[0]).toMatchObject({
        id: 'msg-1',
        role: 'user',
        content: 'Hello',
      });
    });

    it('model_nameがnullの場合、undefinedに変換する', () => {
      const store = useChatStore.getState();

      // メッセージを追加
      const message: ChatMessage = {
        id: 'msg-1',
        role: 'assistant',
        content: '',
        timestamp: new Date(),
        isLoading: true,
      };

      store.addMessage(message);

      // model_nameにnullを渡す
      store.updateLastMessageWithModel('Response', null);

      const updatedMessages = useChatStore.getState().messages;

      // model_nameがundefinedになる
      expect(updatedMessages[0]).toMatchObject({
        id: 'msg-1',
        role: 'assistant',
        content: 'Response',
        isLoading: false,
      });
      expect(updatedMessages[0].model_name).toBeUndefined();
    });

    it('メッセージが空の場合は何もしない', () => {
      const store = useChatStore.getState();

      // メッセージが空の状態で更新を試みる
      store.updateLastMessageWithModel('Content', 'model');

      // メッセージは追加されない
      expect(useChatStore.getState().messages).toHaveLength(0);
    });

    it('isLoadingをfalseに設定する', () => {
      const store = useChatStore.getState();

      // ローディング中のメッセージを追加
      const message: ChatMessage = {
        id: 'msg-1',
        role: 'assistant',
        content: '',
        timestamp: new Date(),
        isLoading: true,
      };

      store.addMessage(message);

      // 更新
      store.updateLastMessageWithModel('Updated content', 'model');

      // isLoadingがfalseになる
      expect(useChatStore.getState().messages[0].isLoading).toBe(false);
    });
  });

  describe('addMessage', () => {
    it('メッセージを追加する', () => {
      const store = useChatStore.getState();

      const message: ChatMessage = {
        id: 'msg-1',
        role: 'user',
        content: 'Hello',
        timestamp: new Date(),
      };

      store.addMessage(message);

      expect(useChatStore.getState().messages).toHaveLength(1);
      expect(useChatStore.getState().messages[0]).toMatchObject(message);
    });
  });

  describe('updateLastMessage', () => {
    it('最後のメッセージのコンテンツを更新する', () => {
      const store = useChatStore.getState();

      const message: ChatMessage = {
        id: 'msg-1',
        role: 'assistant',
        content: 'Original',
        timestamp: new Date(),
        isLoading: true,
      };

      store.addMessage(message);
      store.updateLastMessage('Updated');

      const updatedMessages = useChatStore.getState().messages;

      expect(updatedMessages[0]).toMatchObject({
        id: 'msg-1',
        role: 'assistant',
        content: 'Updated',
        isLoading: false,
      });
    });
  });

  describe('clearMessages', () => {
    it('全メッセージとスレッドIDをクリアする', () => {
      const store = useChatStore.getState();

      // メッセージを追加
      store.addMessage({
        id: 'msg-1',
        role: 'user',
        content: 'Hello',
        timestamp: new Date(),
      });

      store.setCurrentThreadId('thread-1');

      // クリア
      store.clearMessages();

      expect(useChatStore.getState().messages).toHaveLength(0);
      expect(useChatStore.getState().currentThreadId).toBeNull();
    });
  });

  describe('setMessages', () => {
    it('メッセージ一覧を設定する', () => {
      const store = useChatStore.getState();

      const messages: ChatMessage[] = [
        {
          id: 'msg-1',
          role: 'user',
          content: 'Hello',
          timestamp: new Date(),
        },
        {
          id: 'msg-2',
          role: 'assistant',
          content: 'Hi',
          timestamp: new Date(),
        },
      ];

      store.setMessages(messages);

      expect(useChatStore.getState().messages).toHaveLength(2);
      expect(useChatStore.getState().messages).toEqual(messages);
    });
  });

  describe('sidebar', () => {
    it('setSidebarOpenでサイドバーの開閉を設定する', () => {
      const store = useChatStore.getState();

      store.setSidebarOpen(true);
      expect(useChatStore.getState().sidebarOpen).toBe(true);

      store.setSidebarOpen(false);
      expect(useChatStore.getState().sidebarOpen).toBe(false);
    });

    it('toggleSidebarでサイドバーの開閉を切り替える', () => {
      const store = useChatStore.getState();

      // 初期状態 (false)
      expect(useChatStore.getState().sidebarOpen).toBe(false);

      // トグル
      store.toggleSidebar();
      expect(useChatStore.getState().sidebarOpen).toBe(true);

      // トグル
      store.toggleSidebar();
      expect(useChatStore.getState().sidebarOpen).toBe(false);
    });
  });
});
