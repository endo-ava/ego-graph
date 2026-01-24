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
import { AppHeader } from '@/components/layout/AppHeader';
import { Button } from '@/components/ui/button';
import { SystemPromptEditor } from '@/components/system-prompt/SystemPromptEditor';
import { Toaster } from 'sonner';

export default function App() {
  const { clearMessages, setOnSidebarClose, sidebarOpen, toggleSidebar, activeView } = useChatStore();
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
        {activeView === 'system_prompt' ? (
          <>
            {/* System Prompt ビュー */}
            <AppHeader title="System Prompt Editor" onMenuClick={handleToggleSidebar} />

            <main className="flex-1 min-h-0 overflow-hidden">
              <SystemPromptEditor />
            </main>
          </>
        ) : (
          <>
            {/* Chat ビュー */}
            <AppHeader
              title="EgoGraph Chat"
              onMenuClick={handleToggleSidebar}
              rightAction={
                <Button
                  variant="outline"
                  size="sm"
                  onClick={clearMessages}
                  className="h-8 px-3"
                  aria-label="New chat"
                >
                  New
                </Button>
              }
            />

            {/* メッセージリスト */}
            <main className="flex-1 min-h-0 overflow-hidden">
              <MessageList messages={messages} />
            </main>

            {/* 入力エリア */}
            <footer className="shrink-0">
              <ChatInput ref={chatInputRef} onSendMessage={sendMessage} disabled={isLoading} />
            </footer>
          </>
        )}
      </AppLayout>
    </>
  );
}
