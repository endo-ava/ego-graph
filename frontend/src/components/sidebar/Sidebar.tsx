/**
 * サイドバーコンポーネント
 * スレッド一覧を表示し、モバイルではオーバーレイとして表示されます
 */

import { ThreadList } from './ThreadList';
import { SidebarOverlay } from './SidebarOverlay';
import { SidebarHeader } from './SidebarHeader';
import { useChatStore } from '@/lib/store';

export function Sidebar() {
  const { sidebarOpen } = useChatStore();

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
        <nav className="flex-1 overflow-y-auto" aria-label="Thread list">
          <ThreadList />
        </nav>
      </aside>
    </>
  );
}
