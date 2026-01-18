import { useChatStore } from '@/lib/store';
import { Button } from '@/components/ui/button';

export function SidebarHeader() {
  const { setSidebarOpen, onSidebarClose } = useChatStore();

  const handleClose = () => {
    onSidebarClose?.();
    setSidebarOpen(false);
  };

  return (
    <header className="flex items-center justify-between px-4 pb-3 pt-[calc(0.75rem+env(safe-area-inset-top))] border-b shrink-0">
      <h2 className="font-semibold text-lg">会話履歴</h2>
      <Button
        variant="ghost"
        size="sm"
        onClick={handleClose}
        className="md:hidden"
        aria-label="サイドバーを閉じる"
      >
        ✕
      </Button>
    </header>
  );
}
