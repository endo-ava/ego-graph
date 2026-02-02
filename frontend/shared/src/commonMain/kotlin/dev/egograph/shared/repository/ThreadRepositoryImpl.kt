package dev.egograph.shared.repository

import co.touchlab.kermit.Logger
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.atomic.AtomicReference

/**
 * ThreadRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 */
class ThreadRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String = "",
) : ThreadRepository {
    private val logger = Logger.withTag("ThreadRepository")

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val threadsCache = AtomicReference<Map<String, CacheEntry<ThreadListResponse>>>(emptyMap())
    private val threadCache = AtomicReference<Map<String, CacheEntry<Thread>>>(emptyMap())
    private val cacheDurationMs = 5000L

    override fun getThreads(
        limit: Int,
        offset: Int,
    ): Flow<RepositoryResult<ThreadListResponse>> =
        flow {
            val cacheKey = "$limit:$offset"
            val cached = threadsCache.get()[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                emit(Result.success(cached.data))
                return@flow
            }
            try {
                val response =
                    httpClient.get("$baseUrl/v1/threads") {
                        parameter("limit", limit)
                        parameter("offset", offset)
                        if (apiKey.isNotEmpty()) {
                            headers {
                                append("X-API-Key", apiKey)
                            }
                        }
                    }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val body = response.body<ThreadListResponse>()
                        threadsCache.updateAndGet { current -> current + (cacheKey to CacheEntry(body)) }
                        emit(Result.success(body))
                    }
                    else -> {
                        threadsCache.updateAndGet { current -> current - cacheKey }
                        val errorDetail =
                            try {
                                response.body<String>()
                            } catch (e: Exception) {
                                logger.w(e) { "Failed to read error response body" }
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
                threadsCache.updateAndGet { current -> current - cacheKey }
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }
            .flowOn(Dispatchers.IO)

    override fun getThread(threadId: String): Flow<RepositoryResult<Thread>> =
        flow {
            val cached = threadCache.get()[threadId]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                emit(Result.success(cached.data))
                return@flow
            }
            try {
                val response =
                    httpClient.get("$baseUrl/v1/threads/$threadId") {
                        if (apiKey.isNotEmpty()) {
                            headers {
                                append("X-API-Key", apiKey)
                            }
                        }
                    }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val body = response.body<Thread>()
                        threadCache.updateAndGet { current -> current + (threadId to CacheEntry(body)) }
                        emit(Result.success(body))
                    }
                    else -> {
                        threadCache.updateAndGet { current -> current - threadId }
                        val errorDetail =
                            try {
                                response.body<String>()
                            } catch (e: Exception) {
                                logger.w(e) { "Failed to read error response body" }
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
                threadCache.updateAndGet { current -> current - threadId }
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }
            .flowOn(Dispatchers.IO)

    override suspend fun createThread(title: String): RepositoryResult<Thread> =
        Result.failure(
            ApiError.HttpError(
                code = 501,
                errorMessage = "Not Implemented",
                detail = "Thread creation is not yet supported",
            ),
        )
}
