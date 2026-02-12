package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.ChatResponse
import dev.egograph.shared.core.domain.model.ModelsResponse
import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.repository.RepositoryResult
import kotlinx.coroutines.flow.Flow

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
     * チャットメッセージを送信する（SSEストリーミング）
     *
     * SSEのdataフィールドを逐次パースし、StreamChunkを順次emitします。
     * errorタイプのチャンクを受信した場合はApiErrorとして扱います。
     *
     * @param request チャットリクエスト
     * @return ストリーミングレスポンスのFlow
     */
    fun streamChatResponse(request: ChatRequest): Flow<RepositoryResult<StreamChunk>>

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
     * @return モデル一覧とデフォルトモデルIDを含むレスポンス
     */
    suspend fun getModels(): RepositoryResult<ModelsResponse>
}
