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
  const { clearMessages, setOnSidebarClose } = useChatStore();
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

  const { sidebarOpen } = useChatStore((state) => ({ sidebarOpen: state.sidebarOpen }));

  useEffect(() => {
    if (sidebarOpen) {
      chatInputRef.current?.blur();
    }
  }, [sidebarOpen]);

  const handleToggleSidebar = () => {
    chatInputRef.current?.blur();
    useChatStore.getState().toggleSidebar();
  };

  return (
    <>
      <Toaster />
      <AppLayout>
        {/* ヘッダー */}
        <header className="flex shrink-0 items-center justify-between border-b bg-background px-4 pb-3 pt-[calc(0.75rem+env(safe-area-inset-top))]">
          <div className="flex items-center gap-2">
            {/* ハンバーガーメニューボタン */}
            <Button
              variant="ghost"
              size="sm"
              onClick={handleToggleSidebar}
              className="md:hidden"
              aria-label="メニューを開く"
            >
              ☰
            </Button>
            <h1 className="text-lg font-semibold">EgoGraph Chat</h1>
          </div>
          <Button variant="outline" size="sm" onClick={clearMessages}>
            新規チャット
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
