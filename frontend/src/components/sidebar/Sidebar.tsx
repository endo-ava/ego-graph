/**
 * サイドバーコンポーネント
 * スレッド一覧を表示し、モバイルではオーバーレイとして表示されます
 */

import { useChatStore } from '@/lib/store';
import { ThreadList } from './ThreadList';
import { Button } from '@/components/ui/button';

export function Sidebar() {
  const { sidebarOpen, setSidebarOpen, clearMessages } = useChatStore();

  const handleNewChat = () => {
    clearMessages();
    if (window.innerWidth < 768) {
      setSidebarOpen(false);
    }
  };

  return (
    <>
      {/* オーバーレイ（モバイルのみ） */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 md:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* サイドバー本体 */}
      <aside
        className={`
          fixed md:relative
          top-0 left-0 bottom-0
          w-[320px]
          bg-background
          border-r
          flex flex-col
          z-50
          transition-transform duration-300 ease-in-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
        `}
        aria-label="スレッド一覧"
      >
        {/* ヘッダー */}
        <header className="flex items-center justify-between px-4 pb-3 pt-[calc(0.75rem+env(safe-area-inset-top))] border-b shrink-0">
          <h2 className="font-semibold text-lg">会話履歴</h2>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setSidebarOpen(false)}
            className="md:hidden"
            aria-label="サイドバーを閉じる"
          >
            ✕
          </Button>
        </header>

        <div className="px-4 py-3 border-b">
          <Button variant="secondary" size="sm" className="w-full" onClick={handleNewChat}>
            新規チャット
          </Button>
        </div>

        {/* スレッド一覧 */}
        <div className="flex-1 overflow-y-auto">
          <ThreadList />
        </div>
      </aside>
    </>
  );
}
