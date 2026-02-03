package dev.egograph.shared.repository

import co.touchlab.kermit.Logger
import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ThreadRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 */
class ThreadRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String = "",
    private val diskCache: DiskCache? = null,
) : ThreadRepository {
    private val logger = Logger.withTag("ThreadRepository")

    /**
     * Generate a stable context hash from baseUrl and apiKey.
     * This prevents cache collisions across different API configurations.
     */
    private val contextHash: String by lazy {
        generateContextHash(baseUrl, apiKey)
    }

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val threadsCacheMutex = Mutex()
    private var threadsCache: Map<String, CacheEntry<ThreadListResponse>> = emptyMap()

    private val threadCacheMutex = Mutex()
    private var threadCache: Map<String, CacheEntry<Thread>> = emptyMap()

    private val cacheDurationMs = 60000L

    private fun generateContextHash(
        baseUrl: String,
        apiKey: String,
    ): String {
        val combined = "$baseUrl:$apiKey"
        var hash = 0
        for (char in combined) {
            hash = 31 * hash + char.code
        }
        val positiveHash = hash.toLong() and 0xFFFFFFFFL
        return positiveHash.toString(16).padStart(8, '0')
    }

    override fun getThreads(
        limit: Int,
        offset: Int,
    ): Flow<RepositoryResult<ThreadListResponse>> =
        flow {
            val cacheKey = "$contextHash:list:$limit:$offset"
            val cached = threadsCacheMutex.withLock { threadsCache[cacheKey] }
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                emit(Result.success(cached.data))
                return@flow
            }
            try {
                val body =
                    if (diskCache != null) {
                        diskCache.getOrFetch(
                            key = cacheKey,
                            serializer = ThreadListResponse.serializer(),
                        ) {
                            fetchThreadList(limit, offset)
                        }
                    } else {
                        fetchThreadList(limit, offset)
                    }
                threadsCacheMutex.withLock {
                    threadsCache = threadsCache + (cacheKey to CacheEntry(body))
                }
                emit(Result.success(body))
            } catch (e: ApiError) {
                threadsCacheMutex.withLock {
                    threadsCache = threadsCache - cacheKey
                }
                diskCache?.remove(cacheKey)
                emit(Result.failure(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                threadsCacheMutex.withLock {
                    threadsCache = threadsCache - cacheKey
                }
                diskCache?.remove(cacheKey)
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    override fun getThread(threadId: String): Flow<RepositoryResult<Thread>> =
        flow {
            val cacheKey = "$contextHash:thread:$threadId"
            val cached = threadCacheMutex.withLock { threadCache[cacheKey] }
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                emit(Result.success(cached.data))
                return@flow
            }
            try {
                val body =
                    if (diskCache != null) {
                        diskCache.getOrFetch(
                            key = cacheKey,
                            serializer = Thread.serializer(),
                        ) {
                            fetchThread(threadId)
                        }
                    } else {
                        fetchThread(threadId)
                    }
                threadCacheMutex.withLock {
                    threadCache = threadCache + (cacheKey to CacheEntry(body))
                }
                emit(Result.success(body))
            } catch (e: ApiError) {
                threadCacheMutex.withLock {
                    threadCache = threadCache - cacheKey
                }
                diskCache?.remove(cacheKey)
                emit(Result.failure(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                threadCacheMutex.withLock {
                    threadCache = threadCache - cacheKey
                }
                diskCache?.remove(cacheKey)
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun createThread(title: String): RepositoryResult<Thread> =
        Result.failure(
            ApiError.HttpError(
                code = 501,
                errorMessage = "Not Implemented",
                detail = "Thread creation is not yet supported",
            ),
        )

    private suspend fun fetchThreadList(
        limit: Int,
        offset: Int,
    ): ThreadListResponse {
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

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> {
                val errorDetail =
                    try {
                        response.body<String>()
                    } catch (e: Exception) {
                        logger.w(e) { "Failed to read error response body" }
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

    private suspend fun fetchThread(threadId: String): Thread {
        val response =
            httpClient.get("$baseUrl/v1/threads/$threadId") {
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
                        logger.w(e) { "Failed to read error response body" }
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
