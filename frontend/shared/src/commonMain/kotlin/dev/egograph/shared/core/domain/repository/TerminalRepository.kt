package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.terminal.Session
import kotlinx.coroutines.flow.Flow

/**
 * Terminal Repository
 *
 * ターミナルセッションの一覧取得、詳細取得を担当します。
 */
interface TerminalRepository {
    /**
     * セッション一覧を取得する（Flowベース）
     *
     * @param forceRefresh trueの場合、キャッシュを無視してAPIから取得
     * @return セッション一覧のFlow
     */
    fun getSessions(forceRefresh: Boolean = false): Flow<RepositoryResult<List<Session>>>

    /**
     * 特定のセッションを取得する（Flowベース）
     *
     * @param sessionId セッションID
     * @param forceRefresh trueの場合、キャッシュを無視してAPIから取得
     * @return セッションのFlow
     */
    fun getSession(
        sessionId: String,
        forceRefresh: Boolean = false,
    ): Flow<RepositoryResult<Session>>
}
