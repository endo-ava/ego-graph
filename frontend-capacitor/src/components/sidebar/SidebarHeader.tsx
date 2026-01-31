import { useChatStore } from '@/lib/store';
import { useIsMobile } from '@/hooks/ui/useIsMobile';
import { Button } from '@/components/ui/button';

export function SidebarHeader() {
  const { clearMessages, setSidebarOpen, setActiveView } = useChatStore();
  const isMobile = useIsMobile();

  const handleNewChat = () => {
    setActiveView('chat');
    clearMessages();
    if (isMobile) {
      setSidebarOpen(false);
    }
  };

  return (
    <header className="flex shrink-0 items-center justify-between border-b border-border px-4 pb-3 pt-[calc(0.75rem+env(safe-area-inset-top))]">
      <h2 className="text-base font-semibold">History</h2>
      <Button
        variant="outline"
        size="sm"
        onClick={handleNewChat}
        className="border-border h-8 px-3"
        aria-label="New chat"
      >
        New
      </Button>
    </header>
  );
}
