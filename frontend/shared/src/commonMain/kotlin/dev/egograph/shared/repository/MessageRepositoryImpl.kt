package dev.egograph.shared.repository

import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.dto.ThreadMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val messagesCacheMutex = Mutex()
    private var messagesCache: Map<String, CacheEntry<ThreadMessagesResponse>> = emptyMap()

    override fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>> =
        flow {
            val cached = messagesCacheMutex.withLock { messagesCache[threadId] }
            if (cached != null && System.currentTimeMillis() - cached.timestamp < DEFAULT_CACHE_DURATION_MS) {
                emit(Result.success(cached.data))
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

                messagesCacheMutex.withLock {
                    messagesCache = messagesCache + (threadId to CacheEntry(body))
                }
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
        messagesCacheMutex.withLock {
            messagesCache = messagesCache - threadId
        }
        diskCache?.remove(threadId)
    }

    private suspend fun fetchThreadMessages(threadId: String): ThreadMessagesResponse =
        httpClient
            .get("$baseUrl/v1/threads/$threadId/messages") {
                configureAuth(apiKey)
            }.bodyOrThrow()
}
