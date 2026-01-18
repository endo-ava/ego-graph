import { useChatStore } from '@/lib/store';
import { Button } from '@/components/ui/button';

export function NewChatButton() {
  const { clearMessages, setSidebarOpen } = useChatStore();

  const handleNewChat = () => {
    clearMessages();
    if (window.innerWidth < 768) {
      setSidebarOpen(false);
    }
  };

  return (
    <div className="px-4 py-3 border-b">
      <Button variant="secondary" size="sm" className="w-full" onClick={handleNewChat}>
        新規チャット
      </Button>
    </div>
  );
}
