/**
 * ModelSelectorコンポーネントのテスト
 * モデル一覧取得, フォールバック, モデル選択, コスト表示を検証
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type React from 'react';
import { ModelSelector } from '../ModelSelector';
import { useChatStore } from '@/lib/store';
import * as api from '@/lib/api';
import type { ModelsResponse } from '@/types/chat';

// QueryClient Providerのラッパー
function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

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

describe('ModelSelector', () => {
  beforeEach(() => {
    // localStorageのモック
    Object.defineProperty(window, 'localStorage', {
      value: localStorageMock,
      writable: true,
    });
    localStorageMock.clear();

    // ストアをクリア
    useChatStore.setState({
      selectedModel: 'tngtech/deepseek-r1t2-chimera:free',
    });

    // モックをリセット
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('APIからモデル一覧を取得して表示する', async () => {
    const mockResponse: ModelsResponse = {
      models: [
        {
          id: 'model-1',
          name: 'Test Model 1',
          provider: 'openrouter',
          input_cost_per_1m: 0.5,
          output_cost_per_1m: 1.5,
          is_free: false,
        },
        {
          id: 'model-2',
          name: 'Test Model 2',
          provider: 'openrouter',
          input_cost_per_1m: 0.0,
          output_cost_per_1m: 0.0,
          is_free: true,
        },
      ],
      default_model: 'model-1',
    };

    vi.spyOn(api, 'getModels').mockResolvedValue(mockResponse);

    const wrapper = createWrapper();
    const user = userEvent.setup();

    render(<ModelSelector />, { wrapper });

    // 初期状態で最初のモデルが表示される
    await waitFor(() => {
      expect(screen.getByText('Test Model 1')).toBeInTheDocument();
    });

    // ドロップダウンを開く
    const button = screen.getByRole('button');
    await user.click(button);

    // モデル一覧が表示される
    await waitFor(() => {
      expect(screen.getAllByText('Test Model 1')).toHaveLength(2); // ボタンとドロップダウン
      expect(screen.getAllByText('Test Model 2')).toHaveLength(1);
    });

    // API が呼ばれたことを確認
    expect(api.getModels).toHaveBeenCalledTimes(1);
  });

  it('API失敗時にエラーメッセージを表示する', async () => {
    // API エラーをモック
    vi.spyOn(api, 'getModels').mockRejectedValue(new Error('Network error'));
    vi.spyOn(console, 'error').mockImplementation(() => {});

    const wrapper = createWrapper();
    render(<ModelSelector />, { wrapper });

    // エラーメッセージが表示される
    await waitFor(() => {
      expect(screen.getByText('Failed to load models')).toBeInTheDocument();
    });

    // ボタンがdisabledであることを確認
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });

  it('モデルを選択するとストアが更新される', async () => {
    const mockResponse: ModelsResponse = {
      models: [
        {
          id: 'model-1',
          name: 'Test Model 1',
          provider: 'openrouter',
          input_cost_per_1m: 0.0,
          output_cost_per_1m: 0.0,
          is_free: true,
        },
        {
          id: 'model-2',
          name: 'Test Model 2',
          provider: 'openrouter',
          input_cost_per_1m: 0.25,
          output_cost_per_1m: 0.5,
          is_free: false,
        },
      ],
      default_model: 'model-1',
    };

    vi.spyOn(api, 'getModels').mockResolvedValue(mockResponse);

    const wrapper = createWrapper();
    const user = userEvent.setup();

    render(<ModelSelector />, { wrapper });

    // 初期状態を確認
    await waitFor(() => {
      expect(screen.getByText('Test Model 1')).toBeInTheDocument();
    });

    // ドロップダウンを開く
    const button = screen.getByRole('button');
    await user.click(button);

    // 2つ目のモデルを選択
    const model2Button = screen
      .getByText('Test Model 2')
      .closest('button') as HTMLButtonElement | null;
    expect(model2Button).not.toBeNull();
    await user.click(model2Button!);

    // ストアが更新されたことを確認
    await waitFor(() => {
      expect(useChatStore.getState().selectedModel).toBe('model-2');
    });

    // localStorageに保存されたことを確認
    expect(localStorageMock.getItem('selected_model')).toBe('model-2');

    // ドロップダウンが閉じられたことを確認
    await waitFor(() => {
      expect(screen.getAllByText('Test Model 2')).toHaveLength(1); // ボタンのみ表示
    });
  });

  it('選択されたモデルが表示される', async () => {
    const mockResponse: ModelsResponse = {
      models: [
        {
          id: 'model-1',
          name: 'Test Model 1',
          provider: 'openrouter',
          input_cost_per_1m: 0.0,
          output_cost_per_1m: 0.0,
          is_free: true,
        },
        {
          id: 'model-2',
          name: 'Test Model 2',
          provider: 'openrouter',
          input_cost_per_1m: 0.25,
          output_cost_per_1m: 0.5,
          is_free: false,
        },
      ],
      default_model: 'model-1',
    };

    vi.spyOn(api, 'getModels').mockResolvedValue(mockResponse);

    // ストアに model-2 を設定
    useChatStore.setState({
      selectedModel: 'model-2',
    });

    const wrapper = createWrapper();
    const user = userEvent.setup();

    render(<ModelSelector />, { wrapper });

    // model-2 が表示される
    await waitFor(() => {
      expect(screen.getByText('Test Model 2')).toBeInTheDocument();
    });

    // ドロップダウンを開く
    const button = screen.getByRole('button');
    await user.click(button);

    // model-2 がハイライトされている
    const model2Button = screen
      .getAllByText('Test Model 2')[1]!
      .closest('button')!;
    expect(model2Button).toHaveClass('bg-accent');
  });

  it('コスト表示が正しいフォーマットである (無料モデル)', async () => {
    const mockResponse: ModelsResponse = {
      models: [
        {
          id: 'free-model',
          name: 'Free Model',
          provider: 'openrouter',
          input_cost_per_1m: 0.0,
          output_cost_per_1m: 0.0,
          is_free: true,
        },
      ],
      default_model: 'free-model',
    };

    vi.spyOn(api, 'getModels').mockResolvedValue(mockResponse);

    const wrapper = createWrapper();
    render(<ModelSelector />, { wrapper });

    // "Free" が表示される
    await waitFor(() => {
      expect(screen.getByText('Free')).toBeInTheDocument();
    });
  });

  it('コスト表示が正しいフォーマットである (有料モデル)', async () => {
    const mockResponse: ModelsResponse = {
      models: [
        {
          id: 'paid-model',
          name: 'Paid Model',
          provider: 'openrouter',
          input_cost_per_1m: 0.25,
          output_cost_per_1m: 0.5,
          is_free: false,
        },
      ],
      default_model: 'paid-model',
    };

    vi.spyOn(api, 'getModels').mockResolvedValue(mockResponse);

    const wrapper = createWrapper();
    render(<ModelSelector />, { wrapper });

    // コスト情報が表示される
    await waitFor(() => {
      expect(screen.getByText(/In: \$0\.25 \/ 1M/)).toBeInTheDocument();
      expect(screen.getByText(/Out: \$0\.5 \/ 1M/)).toBeInTheDocument();
    });
  });

  it('選択されたモデルが一覧に存在しない場合、最初のモデルを選択する', async () => {
    const mockResponse: ModelsResponse = {
      models: [
        {
          id: 'model-7',
          name: 'Test Model 1',
          provider: 'openrouter',
          input_cost_per_1m: 0.0,
          output_cost_per_1m: 0.0,
          is_free: true,
        },
        {
          id: 'model-2',
          name: 'Test Model 2',
          provider: 'openrouter',
          input_cost_per_1m: 0.25,
          output_cost_per_1m: 0.5,
          is_free: false,
        },
      ],
      default_model: 'model-7',
    };

    vi.spyOn(api, 'getModels').mockResolvedValue(mockResponse);

    // 存在しないモデルIDをストアに設定
    useChatStore.setState({
      selectedModel: 'non-existent-model',
    });

    const wrapper = createWrapper();
    render(<ModelSelector />, { wrapper });

    // 最初のモデルが自動選択される
    await waitFor(() => {
      expect(useChatStore.getState().selectedModel).toBe('model-7');
    });

    // 画面にも反映される
    await waitFor(() => {
      expect(screen.getByText('Test Model 1')).toBeInTheDocument();
    });
  });
});
