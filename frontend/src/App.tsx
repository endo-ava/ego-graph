/**
 * メインAppコンポーネント
 * チャットUIのレイアウトを管理
 */

import { useChat } from '@/hooks/useChat';
import { useChatStore } from '@/lib/store';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/button';
import { Toaster } from 'sonner';

export default function App() {
  const { messages, sendMessage, clearMessages, isLoading } = useChat();
  const { toggleSidebar } = useChatStore();

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
              onClick={toggleSidebar}
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
          <ChatInput onSendMessage={sendMessage} disabled={isLoading} />
        </footer>
      </AppLayout>
    </>
  );
}
