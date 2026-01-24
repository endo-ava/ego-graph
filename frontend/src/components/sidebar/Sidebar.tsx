/**
 * サイドバーコンポーネント
 * スレッド一覧を表示し、モバイルではオーバーレイとして表示されます
 */

import { ThreadList } from './ThreadList';
import { SidebarOverlay } from './SidebarOverlay';
import { SidebarHeader } from './SidebarHeader';
import { useChatStore } from '@/lib/store';
import { useIsMobile } from '@/hooks/ui/useIsMobile';
import { Button } from '@/components/ui/button';

export function Sidebar() {
  const { sidebarOpen, activeView, setActiveView, setSidebarOpen } = useChatStore();
  const isMobile = useIsMobile();

  const handleNavigate = (view: 'chat' | 'system_prompt') => {
    setActiveView(view);
    if (isMobile) {
      setSidebarOpen(false);
    }
  };

  return (
    <>
      <SidebarOverlay />
      <aside
        className={`
          fixed md:relative
          top-0 left-0 bottom-0
          w-[320px]
          bg-secondary
          border-r border-border
          flex flex-col
          z-50
          transition-transform duration-300 ease-in-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
        `}
      >
        <SidebarHeader />

        <div className="shrink-0 border-b border-border px-4 py-3">
          <Button
            variant={activeView === 'system_prompt' ? 'secondary' : 'ghost'}
            onClick={() => handleNavigate('system_prompt')}
            className="w-full justify-start"
            aria-current={activeView === 'system_prompt' ? 'page' : undefined}
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
              className="mr-2"
            >
              <path d="M15 6v12a3 3 0 1 0 3-3H6a3 3 0 1 0 3 3V6a3 3 0 1 0-3 3h12a3 3 0 1 0-3-3" />
            </svg>
            System Prompt
          </Button>
        </div>

        <nav className="flex-1 overflow-y-auto" aria-label="Thread list">
          <ThreadList />
        </nav>
      </aside>
    </>
  );
}
