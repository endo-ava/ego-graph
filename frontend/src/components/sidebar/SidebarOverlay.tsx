import { useChatStore } from '@/lib/store';

export function SidebarOverlay() {
  const { sidebarOpen, setSidebarOpen, onSidebarClose } = useChatStore();

  if (!sidebarOpen) return null;

  const handleClose = () => {
    onSidebarClose?.();
    setSidebarOpen(false);
  };

  return (
    <div
      className="fixed inset-0 bg-black/50 z-40 md:hidden"
      onClick={handleClose}
      aria-hidden="true"
    />
  );
}
