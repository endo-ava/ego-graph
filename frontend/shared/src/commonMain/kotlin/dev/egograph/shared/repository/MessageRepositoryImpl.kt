package dev.egograph.shared.repository

import co.touchlab.kermit.Logger
import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.dto.ThreadMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
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
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val messagesCacheMutex = Mutex()
    private var messagesCache: Map<String, CacheEntry<ThreadMessagesResponse>> = emptyMap()
    private val cacheDurationMs = 60000L
    private val inFlightMutex = Mutex()
    private var inFlightRequests: Map<String, CompletableDeferred<RepositoryResult<ThreadMessagesResponse>>> = emptyMap()

    override fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>> =
        flow {
            val cached = messagesCacheMutex.withLock { messagesCache[threadId] }
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                emit(Result.success(cached.data))
                return@flow
            }

            val (deferred, isOwner) =
                inFlightMutex.withLock {
                    val existing = inFlightRequests[threadId]
                    if (existing != null) {
                        existing to false
                    } else {
                        val created = CompletableDeferred<RepositoryResult<ThreadMessagesResponse>>()
                        inFlightRequests = inFlightRequests + (threadId to created)
                        created to true
                    }
                }

            if (isOwner) {
                try {
                    val result =
                        try {
                            val body =
                                if (diskCache != null) {
                                    diskCache.getOrFetch(
                                        key = threadId,
                                        serializer = ThreadMessagesResponse.serializer(),
                                    ) {
                                        fetchThreadMessages(threadId)
                                    }
                                } else {
                                    fetchThreadMessages(threadId)
                                }
                            messagesCacheMutex.withLock {
                                messagesCache = messagesCache + (threadId to CacheEntry(body))
                            }
                            Result.success(body)
                        } catch (e: ApiError) {
                            messagesCacheMutex.withLock {
                                messagesCache = messagesCache - threadId
                            }
                            diskCache?.remove(threadId)
                            Result.failure(e)
                        } catch (e: Exception) {
                            messagesCacheMutex.withLock {
                                messagesCache = messagesCache - threadId
                            }
                            diskCache?.remove(threadId)
                            Result.failure(ApiError.NetworkError(e))
                        }

                    deferred.complete(result)
                } catch (e: CancellationException) {
                    deferred.cancel(e)
                    throw e
                } finally {
                    inFlightMutex.withLock {
                        inFlightRequests = inFlightRequests - threadId
                    }
                }
            }

            emit(deferred.await())
        }.flowOn(Dispatchers.IO)

    private suspend fun fetchThreadMessages(threadId: String): ThreadMessagesResponse {
        val response =
            httpClient.get("$baseUrl/v1/threads/$threadId/messages") {
                if (apiKey.isNotEmpty()) {
                    headers {
                        append("X-API-Key", apiKey)
                    }
                }
            }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> {
                val errorDetail =
                    try {
                        response.body<String>()
                    } catch (e: Exception) {
                        Logger.w(e) { "Failed to read error response body" }
                        null
                    }
                throw ApiError.HttpError(
                    code = response.status.value,
                    errorMessage = response.status.description,
                    detail = errorDetail,
                )
            }
        }
    }
}
