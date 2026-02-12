package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.terminal.Session
import dev.egograph.shared.repository.RepositoryResult
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
     * @return セッション一覧のFlow
     */
    fun getSessions(): Flow<RepositoryResult<List<Session>>>

    /**
     * 特定のセッションを取得する（Flowベース）
     *
     * @param sessionId セッションID
     * @return セッションのFlow
     */
    fun getSession(sessionId: String): Flow<RepositoryResult<Session>>
}
