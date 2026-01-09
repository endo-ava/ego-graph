/**
 * スレッド一覧コンポーネント
 * 無限スクロール対応のスレッド一覧を表示します
 */

import { useEffect, useRef } from 'react';
import { useThreads } from '@/hooks/useThreads';
import { ThreadItem } from './ThreadItem';

export function ThreadList() {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError } =
    useThreads();

  const observerTarget = useRef<HTMLDivElement>(null);

  // IntersectionObserverで無限スクロール実装
  useEffect(() => {
    const target = observerTarget.current;
    if (!target) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 0.1 },
    );

    observer.observe(target);

    return () => {
      observer.disconnect();
    };
  }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

  // ローディング状態
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-32">
        <div className="text-sm text-muted-foreground">読み込み中...</div>
      </div>
    );
  }

  // エラー状態
  if (isError) {
    return (
      <div className="flex items-center justify-center h-32">
        <div className="text-sm text-destructive">スレッドの読み込みに失敗しました</div>
      </div>
    );
  }

  // スレッドを全ページから取得
  const threads = data?.pages.flatMap((page) => page.threads) ?? [];

  // 空状態
  if (threads.length === 0) {
    return (
      <div className="flex items-center justify-center h-32">
        <div className="text-sm text-muted-foreground">
          まだ会話がありません。新規チャットを始めましょう
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col">
      {threads.map((thread) => (
        <ThreadItem key={thread.thread_id} thread={thread} />
      ))}

      {/* 無限スクロール用の監視要素 */}
      <div ref={observerTarget} className="h-4" aria-hidden="true" />

      {/* 次ページ読み込み中表示 */}
      {isFetchingNextPage && (
        <div className="flex items-center justify-center py-4">
          <div className="text-sm text-muted-foreground">読み込み中...</div>
        </div>
      )}
    </div>
  );
}
