package dev.egograph.shared.core.data.repository

import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.core.data.repository.internal.InMemoryCache
import dev.egograph.shared.core.data.repository.internal.bodyOrThrow
import dev.egograph.shared.core.data.repository.internal.configureAuth
import dev.egograph.shared.core.domain.model.ThreadMessagesResponse
import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.MessageRepository
import dev.egograph.shared.core.domain.repository.RepositoryResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.cancellation.CancellationException

/**
 * MessageRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 */
class MessageRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String = "",
    private val diskCache: DiskCache? = null,
) : MessageRepository {
    private val messagesCache = InMemoryCache<String, ThreadMessagesResponse>()

    override fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>> =
        flow {
            val cached = messagesCache.get(threadId)
            if (cached != null) {
                emit(Result.success(cached))
                return@flow
            }
            try {
                val body =
                    diskCache?.getOrFetch(
                        key = threadId,
                        serializer = ThreadMessagesResponse.serializer(),
                    ) {
                        fetchThreadMessages(threadId)
                    } ?: fetchThreadMessages(threadId)

                messagesCache.put(threadId, body)
                emit(Result.success(body))
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                invalidateCache(threadId)
                emit(Result.failure(e))
            } catch (e: Exception) {
                invalidateCache(threadId)
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun invalidateCache(threadId: String) {
        messagesCache.remove(threadId)
        diskCache?.remove(threadId)
    }

    private suspend fun fetchThreadMessages(threadId: String): ThreadMessagesResponse =
        httpClient
            .get("$baseUrl/v1/threads/$threadId/messages") {
                configureAuth(apiKey)
            }.bodyOrThrow()
}
