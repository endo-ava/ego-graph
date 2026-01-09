/**
 * スレッド管理のカスタムフック
 * 無限スクロール対応のスレッド一覧とメッセージ取得を提供します
 */

import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { getThreads, getThreadMessages } from '@/lib/api';
import type { ThreadListResponse, ThreadMessagesResponse } from '@/types/chat';

const THREADS_PER_PAGE = 20;

/**
 * スレッド一覧取得フック（無限スクロール対応）
 */
export function useThreads() {
  return useInfiniteQuery<ThreadListResponse, Error>({
    queryKey: ['threads'],
    queryFn: async ({ pageParam = 0 }) => {
      try {
        return await getThreads(
          THREADS_PER_PAGE,
          (pageParam as number) * THREADS_PER_PAGE,
        );
      } catch (error) {
        console.error('[useThreads] Failed to fetch threads:', error);
        throw error; // React Queryのエラーハンドリングに委譲
      }
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => {
      const totalFetched = allPages.reduce(
        (acc, page) => acc + page.threads.length,
        0,
      );
      if (totalFetched >= lastPage.total) {
        return undefined; // これ以上ページがない
      }
      return allPages.length; // 次のページ番号
    },
    staleTime: 1000 * 60, // 1分間はキャッシュを新鮮と見なす
  });
}

/**
 * スレッドメッセージ取得フック
 */
export function useThreadMessages(threadId: string | null) {
  return useQuery<ThreadMessagesResponse, Error>({
    queryKey: ['thread-messages', threadId],
    queryFn: async () => {
      if (!threadId) {
        throw new Error('Thread ID is required');
      }
      try {
        return await getThreadMessages(threadId);
      } catch (error) {
        console.error(
          '[useThreadMessages] Failed to fetch messages for thread:',
          threadId,
          error,
        );
        throw error; // React Queryのエラーハンドリングに委譲
      }
    },
    enabled: !!threadId, // threadIdがある場合のみクエリを実行
    staleTime: 1000 * 60 * 5, // 5分間はキャッシュを新鮮と見なす
  });
}
