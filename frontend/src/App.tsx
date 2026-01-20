/**
 * メインAppコンポーネント
 * チャットUIのレイアウトを管理
 */

import { useEffect, useRef } from 'react';
import { useChat } from '@/hooks/chat/useChat';
import { useChatStore } from '@/lib/store';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput, type ChatInputRef } from '@/components/chat/ChatInput';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/button';
import { Toaster } from 'sonner';

export default function App() {
  const { clearMessages, setOnSidebarClose, sidebarOpen, toggleSidebar } = useChatStore();
  const chatInputRef = useRef<ChatInputRef>(null);
  const { messages, sendMessage, isLoading } = useChat();

  useEffect(() => {
    const handleSidebarAction = () => {
      chatInputRef.current?.blur();
    };

    setOnSidebarClose(handleSidebarAction);

    return () => {
      setOnSidebarClose(null);
    };
  }, [setOnSidebarClose]);

  useEffect(() => {
    if (sidebarOpen) {
      chatInputRef.current?.blur();
    }
  }, [sidebarOpen]);

  const handleToggleSidebar = () => {
    chatInputRef.current?.blur();
    toggleSidebar();
  };

  return (
    <>
      <Toaster />
      <AppLayout>
        {/* ヘッダー */}
        <header className="relative flex shrink-0 items-center justify-between border-b bg-background px-4 pb-3 pt-[calc(0.75rem+env(safe-area-inset-top))]">
          {/* ハンバーガーメニューボタン */}
          <Button
            variant="ghost"
            size="icon"
            onClick={handleToggleSidebar}
            className="md:hidden"
            aria-label="Open menu"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </Button>

          {/* タイトル（中央配置） */}
          <h1 className="absolute left-1/2 -translate-x-1/2 max-w-[calc(100%-8rem)] truncate text-lg font-semibold tracking-tight">
            EgoGraph Chat
          </h1>

          {/* 新規チャットボタン */}
          <Button
            variant="outline"
            size="sm"
            onClick={clearMessages}
            className="h-8 px-3"
            aria-label="New chat"
          >
            New
          </Button>
        </header>

        {/* メッセージリスト */}
        <main className="flex-1 min-h-0 overflow-hidden">
          <MessageList messages={messages} />
        </main>

        {/* 入力エリア */}
        <footer className="shrink-0">
          <ChatInput ref={chatInputRef} onSendMessage={sendMessage} disabled={isLoading} />
        </footer>
      </AppLayout>
    </>
  );
}
