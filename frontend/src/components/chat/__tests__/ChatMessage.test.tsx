/**
 * ChatMessageコンポーネントのテスト
 * Markdown, コードハイライト, ローディング状態を検証
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ChatMessage } from '../ChatMessage';
import type { ChatMessage as ChatMessageType } from '@/types/chat';

describe('ChatMessage', () => {
  // 基本的なユーザーメッセージ
  it('renders user message correctly', () => {
    const message: ChatMessageType = {
      id: 'user-1',
      role: 'user',
      content: 'Hello, world!',
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    render(<ChatMessage message={message} />);

    // ユーザーラベルとメッセージ内容を確認
    expect(screen.getByText('You')).toBeInTheDocument();
    expect(screen.getByText('Hello, world!')).toBeInTheDocument();

    // タイムスタンプの確認（HH:mm形式）
    expect(screen.getByText(/\d{2}:\d{2}/)).toBeInTheDocument();

    // ユーザーアバター（背景色クラス）の確認
    const avatar = screen.getByText('U');
    expect(avatar).toBeInTheDocument();
    expect(avatar).toHaveClass('bg-primary');
  });

  // アシスタントメッセージ
  it('renders assistant message correctly', () => {
    const message: ChatMessageType = {
      id: 'assistant-1',
      role: 'assistant',
      content: 'Hi there!',
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    render(<ChatMessage message={message} />);

    // アシスタントラベルとメッセージ内容を確認
    expect(screen.getByText('Assistant')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();

    // アシスタントアバター（背景色クラス）の確認
    const avatar = screen.getByText('A');
    expect(avatar).toBeInTheDocument();
    expect(avatar).toHaveClass('bg-secondary');
  });

  // Markdownレンダリング（太字）
  it('renders markdown bold text correctly', () => {
    const message: ChatMessageType = {
      id: 'assistant-2',
      role: 'assistant',
      content: 'This is **bold** text.',
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    render(<ChatMessage message={message} />);

    // <strong>タグでレンダリングされることを確認
    const boldElement = screen.getByText('bold');
    expect(boldElement.tagName).toBe('STRONG');
  });

  // Markdownレンダリング（斜体）
  it('renders markdown italic text correctly', () => {
    const message: ChatMessageType = {
      id: 'assistant-3',
      role: 'assistant',
      content: 'This is *italic* text.',
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    render(<ChatMessage message={message} />);

    // <em>タグでレンダリングされることを確認
    const italicElement = screen.getByText('italic');
    expect(italicElement.tagName).toBe('EM');
  });

  // インラインコード
  it('renders inline code correctly', () => {
    const message: ChatMessageType = {
      id: 'assistant-4',
      role: 'assistant',
      content: 'Use `const` for constants.',
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    render(<ChatMessage message={message} />);

    // <code>タグでレンダリングされることを確認
    const codeElement = screen.getByText('const');
    expect(codeElement.tagName).toBe('CODE');
  });

  // コードブロック（シンタックスハイライト）
  it('renders code block with syntax highlighting', () => {
    const message: ChatMessageType = {
      id: 'assistant-5',
      role: 'assistant',
      content: '```python\ndef hello():\n    print("Hello")\n```',
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    const { container } = render(<ChatMessage message={message} />);

    // コードブロックの内容を確認（シンタックスハイライトで要素が分割されるため、テキストコンテンツ全体をチェック）
    const codeBlock = container.querySelector('code');
    expect(codeBlock).toBeInTheDocument();
    expect(codeBlock?.textContent).toContain('def');
    expect(codeBlock?.textContent).toContain('hello');
    expect(codeBlock?.textContent).toContain('print("Hello")');
  });

  // ローディング状態
  it('renders loading state with animated dots', () => {
    const message: ChatMessageType = {
      id: 'assistant-6',
      role: 'assistant',
      content: '',
      timestamp: new Date('2025-12-31T10:00:00Z'),
      isLoading: true,
    };

    render(<ChatMessage message={message} />);

    // animateクラスを持つドット要素を確認
    const animatedDots = document.querySelectorAll('.animate-bounce');
    expect(animatedDots).toHaveLength(3);
  });

  // エラー状態
  it('renders error message with destructive styling', () => {
    const message: ChatMessageType = {
      id: 'assistant-7',
      role: 'assistant',
      content: 'API request failed',
      timestamp: new Date('2025-12-31T10:00:00Z'),
      isError: true,
    };

    render(<ChatMessage message={message} />);

    // エラーメッセージの表示を確認
    expect(screen.getByText('API request failed')).toBeInTheDocument();

    // エラー用のスタイルクラスを確認
    const errorContent = screen.getByText('API request failed').closest('div');
    expect(errorContent).toHaveClass('text-destructive');
  });

  // コンテンツが空の場合
  it('renders "No content" when content is null', () => {
    const message: ChatMessageType = {
      id: 'assistant-8',
      role: 'assistant',
      content: null,
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    render(<ChatMessage message={message} />);

    // "No content" テキストを確認
    expect(screen.getByText('No content')).toBeInTheDocument();
  });

  // 空文字列のコンテンツ（ローディング中でない場合）
  it('renders "No content" when content is empty string and not loading', () => {
    const message: ChatMessageType = {
      id: 'assistant-9',
      role: 'assistant',
      content: '',
      timestamp: new Date('2025-12-31T10:00:00Z'),
      isLoading: false,
    };

    render(<ChatMessage message={message} />);

    // "No content" テキストを確認
    expect(screen.getByText('No content')).toBeInTheDocument();
  });

  // タイムスタンプフォーマット
  it('formats timestamp correctly (HH:mm)', () => {
    const message: ChatMessageType = {
      id: 'user-2',
      role: 'user',
      content: 'Test',
      timestamp: new Date('2025-12-31T14:30:45Z'),
    };

    render(<ChatMessage message={message} />);

    // 時刻が正しくフォーマットされることを確認（14:30形式）
    // 注意: タイムゾーンの影響を受ける可能性があるため、正規表現で検証
    const timeText = screen.getByText(/\d{2}:\d{2}/);
    expect(timeText).toBeInTheDocument();
  });

  // 複数の段落を持つMarkdown
  it('renders multi-paragraph markdown correctly', () => {
    const message: ChatMessageType = {
      id: 'assistant-10',
      role: 'assistant',
      content: 'First paragraph.\n\nSecond paragraph.',
      timestamp: new Date('2025-12-31T10:00:00Z'),
    };

    render(<ChatMessage message={message} />);

    // 両方の段落が表示されることを確認
    expect(screen.getByText(/First paragraph/)).toBeInTheDocument();
    expect(screen.getByText(/Second paragraph/)).toBeInTheDocument();
  });
});
