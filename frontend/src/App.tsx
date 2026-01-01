/**
 * メインAppコンポーネント
 * チャットUIのレイアウトを管理
 */

import { useChat } from '@/hooks/useChat';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { Button } from '@/components/ui/button';

export default function App() {
  const { messages, sendMessage, clearMessages, isLoading } = useChat();

  return (
    <div className="flex h-[100dvh] flex-col overflow-hidden">
      {/* ヘッダー */}
      <header className="flex shrink-0 items-center justify-between border-b bg-background px-4 py-3">
        <h1 className="text-lg font-semibold">EgoGraph Chat</h1>
        <Button
          variant="outline"
          size="sm"
          onClick={clearMessages}
          disabled={messages.length === 0}
        >
          Clear
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
    </div>
  );
}
