/**
 * ChatInputコンポーネントのテスト
 * 入力、送信、サイドバー連携を検証
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ChatInput } from '../ChatInput';

// useChatStoreをモック
vi.mock('@/lib/store', () => ({
  useChatStore: vi.fn(),
}));

// Capacitorをモック
vi.mock('@capacitor/core', () => ({
  Capacitor: {
    isNativePlatform: vi.fn(() => false),
  },
}));

// ModelSelectorをモック
vi.mock('../ModelSelector', () => ({
  ModelSelector: () => <div data-testid="model-selector">Model Selector</div>,
}));

import { useChatStore } from '@/lib/store';
import { Capacitor } from '@capacitor/core';

describe('ChatInput', () => {
  const mockOnSendMessage = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    // デフォルトではサイドバーは閉じている
    // useChatStoreはセレクター関数を受け取るため、適切にモックする
    (useChatStore as unknown as ReturnType<typeof vi.fn>).mockImplementation((selector) =>
      selector({ sidebarOpen: false })
    );
  });

  // 基本的なレンダリング
  it('renders input field and submit button', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    expect(textarea).toBeInTheDocument();

    const submitButton = screen.getByRole('button', { name: '送信' });
    expect(submitButton).toBeInTheDocument();
  });

  // メッセージ入力
  it('allows typing in the textarea', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    await user.type(textarea, 'Hello, world!');

    expect(textarea).toHaveValue('Hello, world!');
  });

  // 送信ボタンクリックでメッセージ送信
  it('sends message when submit button is clicked', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    const submitButton = screen.getByRole('button', { name: '送信' });

    await user.type(textarea, 'Test message');
    await user.click(submitButton);

    expect(mockOnSendMessage).toHaveBeenCalledWith('Test message');
    expect(textarea).toHaveValue('');
  });

  // 空白のみのメッセージは送信しない
  it('does not send message with only whitespace', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    const submitButton = screen.getByRole('button', { name: '送信' });

    await user.type(textarea, '   ');
    await user.click(submitButton);

    expect(mockOnSendMessage).not.toHaveBeenCalled();
    expect(textarea).toHaveValue('   ');
  });

  // disabledプロパティが機能する
  it('disables input and button when disabled prop is true', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={true} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    const submitButton = screen.getByRole('button', { name: '送信' });

    expect(textarea).toBeDisabled();
    expect(submitButton).toBeDisabled();
  });

  // デスクトップ環境でEnterキーで送信
  it('sends message on Enter key press (desktop)', async () => {
    (Capacitor.isNativePlatform as ReturnType<typeof vi.fn>).mockReturnValue(false);

    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    await user.type(textarea, 'Test message');
    await user.keyboard('{Enter}');

    expect(mockOnSendMessage).toHaveBeenCalledWith('Test message');
    expect(textarea).toHaveValue('');
  });

  // デスクトップ環境でShift+Enterで改行
  it('adds newline on Shift+Enter (desktop)', async () => {
    (Capacitor.isNativePlatform as ReturnType<typeof vi.fn>).mockReturnValue(false);

    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    await user.type(textarea, 'First line{Shift>}{Enter}{/Shift}Second line');

    expect(textarea).toHaveValue('First line\nSecond line');
    expect(mockOnSendMessage).not.toHaveBeenCalled();
  });

  // モバイル環境でEnterキーは改行のみ
  it('does not send on Enter key press in mobile environment', async () => {
    (Capacitor.isNativePlatform as ReturnType<typeof vi.fn>).mockReturnValue(true);

    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    await user.type(textarea, 'Test{Enter}message');

    // モバイルではEnterで送信せず、改行が入る
    expect(mockOnSendMessage).not.toHaveBeenCalled();
    expect(textarea).toHaveValue('Test\nmessage');
  });

  // サイドバーが開いたときにフォーカスが外れる（今回の新機能）
  it('blurs textarea when sidebar is opened', async () => {
    let sidebarOpen = false;
    (useChatStore as unknown as ReturnType<typeof vi.fn>).mockImplementation((selector) =>
      selector({ sidebarOpen })
    );

    const { rerender } = render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...') as HTMLTextAreaElement;

    // テキストエリアにフォーカスを当てる
    textarea.focus();
    expect(textarea).toHaveFocus();

    // サイドバーを開く
    sidebarOpen = true;
    (useChatStore as unknown as ReturnType<typeof vi.fn>).mockImplementation((selector) =>
      selector({ sidebarOpen })
    );
    rerender(<ChatInput onSendMessage={mockOnSendMessage} />);

    // フォーカスが外れることを確認
    await waitFor(() => {
      expect(textarea).not.toHaveFocus();
    });
  });

  // サイドバーが閉じている状態ではフォーカスは維持される
  it('maintains focus when sidebar remains closed', () => {
    (useChatStore as unknown as ReturnType<typeof vi.fn>).mockImplementation((selector) =>
      selector({ sidebarOpen: false })
    );

    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...') as HTMLTextAreaElement;

    // テキストエリアにフォーカスを当てる
    textarea.focus();
    expect(textarea).toHaveFocus();

    // サイドバーは閉じたままなので、フォーカスは維持される
    expect(textarea).toHaveFocus();
  });

  // 前後の空白をトリムして送信
  it('trims whitespace before sending message', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    const submitButton = screen.getByRole('button', { name: '送信' });

    await user.type(textarea, '  Test message  ');
    await user.click(submitButton);

    expect(mockOnSendMessage).toHaveBeenCalledWith('Test message');
  });

  // 送信後に入力欄がクリアされる
  it('clears input after successful submission', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    const submitButton = screen.getByRole('button', { name: '送信' });

    await user.type(textarea, 'Test message');
    expect(textarea).toHaveValue('Test message');

    await user.click(submitButton);
    expect(textarea).toHaveValue('');
  });

  // 空の入力では送信ボタンが無効
  it('disables submit button when input is empty', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const submitButton = screen.getByRole('button', { name: '送信' });
    expect(submitButton).toBeDisabled();
  });

  // テキスト入力後は送信ボタンが有効
  it('enables submit button when input has text', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSendMessage={mockOnSendMessage} />);

    const textarea = screen.getByPlaceholderText('メッセージを入力...');
    const submitButton = screen.getByRole('button', { name: '送信' });

    expect(submitButton).toBeDisabled();

    await user.type(textarea, 'Test');
    expect(submitButton).not.toBeDisabled();
  });
});
