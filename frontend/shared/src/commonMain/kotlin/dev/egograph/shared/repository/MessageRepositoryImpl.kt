package dev.egograph.shared.repository

import co.touchlab.kermit.Logger
import dev.egograph.shared.dto.ThreadMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.atomic.AtomicReference

/**
 * MessageRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 */
class MessageRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String = "",
) : MessageRepository {
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val messagesCache = AtomicReference<Map<String, CacheEntry<ThreadMessagesResponse>>>(emptyMap())
    private val cacheDurationMs = 5000L

    override fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>> =
        flow {
            val cached = messagesCache.get()[threadId]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                emit(Result.success(cached.data))
                return@flow
            }
            try {
                val response =
                    httpClient.get("$baseUrl/v1/threads/$threadId/messages") {
                        if (apiKey.isNotEmpty()) {
                            headers {
                                append("X-API-Key", apiKey)
                            }
                        }
                    }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val body = response.body<ThreadMessagesResponse>()
                        messagesCache.updateAndGet { current -> current + (threadId to CacheEntry(body)) }
                        emit(Result.success(body))
                    }
                    else -> {
                        messagesCache.updateAndGet { current -> current - threadId }
                        val errorDetail =
                            try {
                                response.body<String>()
                            } catch (e: Exception) {
                                Logger.w(e) { "Failed to read error response body" }
                                null
                            }
                        emit(
                            Result.failure(
                                ApiError.HttpError(
                                    code = response.status.value,
                                    errorMessage = response.status.description,
                                    detail = errorDetail,
                                ),
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                messagesCache.updateAndGet { current -> current - threadId }
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }
            .flowOn(Dispatchers.IO)
}
