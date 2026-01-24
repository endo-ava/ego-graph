/**
 * useSystemPromptEditor のテスト
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useSystemPromptEditor } from '@/hooks/system_prompt/useSystemPromptEditor';
import type { SystemPromptResponse } from '@/types/system_prompt';

const mockGetSystemPrompt = vi.fn();
const mockUpdateSystemPrompt = vi.fn();
const toastSuccess = vi.fn();
const toastError = vi.fn();

vi.mock('@/lib/api', () => ({
  getSystemPrompt: (...args: unknown[]) => mockGetSystemPrompt(...args),
  updateSystemPrompt: (...args: unknown[]) => mockUpdateSystemPrompt(...args),
}));

vi.mock('sonner', () => ({
  toast: {
    success: (...args: unknown[]) => toastSuccess(...args),
    error: (...args: unknown[]) => toastError(...args),
  },
}));

describe('useSystemPromptEditor', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
        mutations: {
          retry: false,
        },
      },
    });
    mockGetSystemPrompt.mockReset();
    mockUpdateSystemPrompt.mockReset();
    toastSuccess.mockReset();
    toastError.mockReset();
  });

  afterEach(() => {
    queryClient.clear();
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  it('初期ロードでコンテンツが設定される', async () => {
    const response: SystemPromptResponse = {
      name: 'user',
      content: 'hello',
    };
    mockGetSystemPrompt.mockResolvedValue(response);

    const { result } = renderHook(() => useSystemPromptEditor(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.content).toBe('hello');
  });

  it('保存時にAPIを呼び、成功トーストを表示する', async () => {
    const response: SystemPromptResponse = {
      name: 'user',
      content: 'initial',
    };

    mockGetSystemPrompt.mockResolvedValue(response);
    mockUpdateSystemPrompt.mockResolvedValue({
      name: 'user',
      content: 'updated',
    });

    const { result } = renderHook(() => useSystemPromptEditor(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    act(() => {
      result.current.setContent('updated');
    });

    act(() => {
      result.current.save();
    });

    await waitFor(() => {
      expect(mockUpdateSystemPrompt).toHaveBeenCalledWith('user', {
        content: 'updated',
      });
      expect(toastSuccess).toHaveBeenCalledWith('Saved');
    });
  });

  it('取得失敗時にエラートーストを表示する', async () => {
    mockGetSystemPrompt.mockRejectedValue(new Error('Failed to fetch'));

    const { result } = renderHook(() => useSystemPromptEditor(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBeTruthy();
  });

  it('保存失敗時にエラートーストを表示し、コンテンツを保持する', async () => {
    const response: SystemPromptResponse = {
      name: 'user',
      content: 'initial',
    };

    mockGetSystemPrompt.mockResolvedValue(response);
    mockUpdateSystemPrompt.mockRejectedValue(new Error('Save failed'));

    const { result } = renderHook(() => useSystemPromptEditor(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    act(() => {
      result.current.setContent('updated');
    });

    act(() => {
      result.current.save();
    });

    await waitFor(() => {
      expect(toastError).toHaveBeenCalled();
    });

    expect(result.current.content).toBe('updated');
  });
});
