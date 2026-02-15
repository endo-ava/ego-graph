package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.ThreadMessagesResponse
import kotlinx.coroutines.flow.Flow

/**
 * メッセージRepository
 *
 * スレッド内のメッセージ取得を担当します。
 */
interface MessageRepository {
    /**
     * スレッド内のメッセージ一覧を取得する（Flowベース）
     *
     * @param threadId スレッドID
     * @return メッセージ一覧のFlow
     */
    fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>>

    /**
     * 指定されたスレッドIDのメッセージキャッシュを無効化する
     *
     * @param threadId スレッドID
     */
    suspend fun invalidateCache(threadId: String)
}
