package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.ThreadMessagesResponse
import dev.egograph.shared.repository.RepositoryResult
import kotlinx.coroutines.flow.Flow

/**
 * メッセージRepository
 *
 * スレッド内のメッセージ取得、メッセージ送信を担当します。
 */
interface MessageRepository {
    /**
     * スレッド内のメッセージ一覧を取得する（Flowベース）
     *
     * @param threadId スレッドID
     * @return メッセージ一覧のFlow
     */
    fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>>
}
