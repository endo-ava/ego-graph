import { useThreads } from '@/hooks/useThreads';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { ThreadItem } from './ThreadItem';
import { ThreadListLoading } from './ThreadListLoading';
import { ThreadListError } from './ThreadListError';
import { ThreadListEmpty } from './ThreadListEmpty';

export function ThreadList() {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError } =
    useThreads();

  const observerTarget = useInfiniteScroll({
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  });

  if (isLoading) {
    return <ThreadListLoading />;
  }

  if (isError) {
    return <ThreadListError />;
  }

  const threads = data?.pages.flatMap((page) => page.threads) ?? [];

  if (threads.length === 0) {
    return <ThreadListEmpty />;
  }

  return (
    <nav aria-label="スレッド一覧" className="flex flex-col">
      <ul role="list" className="flex flex-col">
        {threads.map((thread) => (
          <li key={thread.thread_id}>
            <ThreadItem thread={thread} />
          </li>
        ))}
      </ul>

      <div ref={observerTarget} className="h-4" aria-hidden="true" />

      {isFetchingNextPage && (
        <div className="flex items-center justify-center py-4" role="status" aria-live="polite">
          <span className="text-sm text-muted-foreground">読み込み中...</span>
        </div>
      )}
    </nav>
  );
}
