/**
 * ChatInputコンポーネント
 * 自動拡張するテキストエリアと送信ボタン
 */

import { useState, useRef, useEffect, KeyboardEvent } from 'react';
import TextareaAutosize from 'react-textarea-autosize';
import { Capacitor } from '@capacitor/core';
import { Button } from '@/components/ui/button';
import { useChatStore } from '@/lib/store';

interface ChatInputProps {
  onSendMessage: (message: string) => void;
  disabled?: boolean;
}

export function ChatInput({ onSendMessage, disabled = false }: ChatInputProps) {
  const [input, setInput] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const sidebarOpen = useChatStore((state) => state.sidebarOpen);

  // サイドバーが開いたときに入力欄のフォーカスを外す
  useEffect(() => {
    if (sidebarOpen && textareaRef.current) {
      textareaRef.current.blur();
    }
  }, [sidebarOpen]);

  const handleSubmit = () => {
    if (!input.trim() || disabled) return;

    onSendMessage(input.trim());
    setInput('');
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    // モバイル（iOS/Android）ではEnterキーで改行のみ、デスクトップ（Web）ではEnter送信、Shift+Enter改行
    const isNative = Capacitor.isNativePlatform();

    if (e.key === 'Enter' && !e.shiftKey && !isNative) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="border-t bg-background p-4">
      <div className="mx-auto flex max-w-3xl gap-2">
        <TextareaAutosize
          ref={textareaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="メッセージを入力..."
          disabled={disabled}
          minRows={1}
          maxRows={5}
          className="flex-1 resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
        />
        <Button
          onClick={handleSubmit}
          disabled={!input.trim() || disabled}
          size="icon"
          className="shrink-0"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="h-4 w-4"
          >
            <path d="m22 2-7 20-4-9-9-4Z" />
            <path d="M22 2 11 13" />
          </svg>
          <span className="sr-only">送信</span>
        </Button>
      </div>
    </div>
  );
}
