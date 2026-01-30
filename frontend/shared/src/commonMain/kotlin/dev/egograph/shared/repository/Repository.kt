package dev.egograph.shared.repository

import dev.egograph.shared.dto.ChatRequest
import dev.egograph.shared.dto.ChatResponse
import dev.egograph.shared.dto.LLMModel
import dev.egograph.shared.dto.Message
import dev.egograph.shared.dto.StreamChunk
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import dev.egograph.shared.dto.ThreadMessage
import dev.egograph.shared.dto.ThreadMessagesResponse
import kotlinx.coroutines.flow.Flow

/**
 * スレッドRepository
 *
 * スレッドの一覧取得、詳細取得、作成を担当します。
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
        offset: Int = 0
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

/**
 * チャットRepository
 *
 * メッセージ送信、ストリーミングチャット、モデル一覧取得を担当します。
 */
interface ChatRepository {
    /**
     * チャットメッセージを送信する（ストリーミング）
     *
     * @param request チャットリクエスト
     * @return ストリーミングレスポンスのFlow
     */
    fun sendMessage(request: ChatRequest): Flow<RepositoryResult<StreamChunk>>

    /**
     * チャットメッセージを送信する（非ストリーミング）
     *
     * @param request チャットリクエスト
     * @return チャットレスポンス
     */
    suspend fun sendMessageSync(request: ChatRequest): RepositoryResult<ChatResponse>

    /**
     * 利用可能なモデル一覧を取得する
     *
     * @return モデル一覧
     */
    suspend fun getModels(): RepositoryResult<List<LLMModel>>
}
