/**
 * SystemPromptEditor コンポーネントのテスト
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SystemPromptEditor } from '../SystemPromptEditor';
import { useChatStore } from '@/lib/store';
import { useSystemPromptEditor } from '@/hooks/system_prompt/useSystemPromptEditor';
import type { SystemPromptTab } from '@/hooks/system_prompt/useSystemPromptEditor';

vi.mock('@/hooks/system_prompt/useSystemPromptEditor', () => ({
  useSystemPromptEditor: vi.fn(),
}));

const mockUseSystemPromptEditor = vi.mocked(useSystemPromptEditor);

const baseTabs: SystemPromptTab[] = [
  { key: 'user', label: 'USER', filename: 'USER.md' },
  { key: 'identity', label: 'IDENTITY', filename: 'IDENTITY.md' },
];

describe('SystemPromptEditor', () => {
  beforeEach(() => {
    useChatStore.setState({ activeView: 'system_prompt' });
    mockUseSystemPromptEditor.mockReturnValue({
      tabs: baseTabs,
      activeTab: 'user',
      setActiveTab: vi.fn(),
      content: 'hello',
      setContent: vi.fn(),
      isLoading: false,
      error: null,
      save: vi.fn(),
      isSaving: false,
      saveError: null,
      isDirty: () => false,
    });
  });

  it('タブとコンテンツが表示される', () => {
    render(<SystemPromptEditor />);

    expect(screen.getByText('USER')).toBeInTheDocument();
    expect(screen.getByText('USER.md')).toBeInTheDocument();
    expect(screen.getByDisplayValue('hello')).toBeInTheDocument();
  });

  it('保存ボタンでsaveが呼ばれる', async () => {
    const save = vi.fn();
    mockUseSystemPromptEditor.mockReturnValue({
      tabs: baseTabs,
      activeTab: 'user',
      setActiveTab: vi.fn(),
      content: 'hello',
      setContent: vi.fn(),
      isLoading: false,
      error: null,
      save,
      isSaving: false,
      saveError: null,
      isDirty: () => false,
    });

    const user = userEvent.setup();
    render(<SystemPromptEditor />);

    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(save).toHaveBeenCalledTimes(1);
  });

  it('戻るボタンでチャット画面へ戻る', async () => {
    const user = userEvent.setup();
    render(<SystemPromptEditor />);

    await user.click(screen.getByRole('button', { name: 'Back to Chat' }));

    expect(useChatStore.getState().activeView).toBe('chat');
  });

  it('ガイドボタンでポップオーバーが表示される', async () => {
    const user = userEvent.setup();
    render(<SystemPromptEditor />);

    await user.click(screen.getByRole('button', { name: 'Toggle guide' }));

    expect(screen.getByText('GUIDE: System Prompt Files')).toBeInTheDocument();
    expect(screen.getByText('AGENTS.md')).toBeInTheDocument();
    expect(screen.getByText('BOOTSTRAP.md')).toBeInTheDocument();
  });
});
