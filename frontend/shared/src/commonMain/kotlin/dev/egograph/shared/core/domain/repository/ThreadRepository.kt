package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.Thread
import dev.egograph.shared.core.domain.model.ThreadListResponse
import kotlinx.coroutines.flow.Flow

/**
 * スレッドリポジトリ
 *
 * スレッド一覧取得、詳細取得、作成を担当します。
 */
interface ThreadRepository {
    /**
     * スレッド一覧を取得する（Flowベース）
     *
     * @param limit 取得件数
     * @param offset オフセット
     * @return スレッド一覧のFlow
     */
    fun getThreads(
        limit: Int = 20,
        offset: Int = 0,
    ): Flow<RepositoryResult<ThreadListResponse>>

    /**
     * 特定のスレッドを取得する（Flowベース）
     *
     * @param threadId スレッドID
     * @return スレッドのFlow
     */
    fun getThread(threadId: String): Flow<RepositoryResult<Thread>>

    /**
     * 新しいスレッドを作成する
     *
     * @param title スレッドタイトル
     * @return 作成されたスレッド
     */
    suspend fun createThread(title: String): RepositoryResult<Thread>
}
