package dev.egograph.shared.core.data.repository

import co.touchlab.kermit.Logger
import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.core.data.repository.internal.InMemoryCache
import dev.egograph.shared.core.data.repository.internal.bodyOrThrow
import dev.egograph.shared.core.data.repository.internal.configureAuth
import dev.egograph.shared.core.data.repository.internal.generateContextHash
import dev.egograph.shared.core.domain.model.Thread
import dev.egograph.shared.core.domain.model.ThreadListResponse
import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.RepositoryResult
import dev.egograph.shared.core.domain.repository.ThreadRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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

    private val threadsCache = InMemoryCache<String, ThreadListResponse>()
    private val threadCache = InMemoryCache<String, Thread>()

    override fun getThreads(
        limit: Int,
        offset: Int,
    ): Flow<RepositoryResult<ThreadListResponse>> =
        flow {
            val cacheKey = "$contextHash:list:$limit:$offset"
            val cached = threadsCache.get(cacheKey)
            if (cached != null) {
                emit(Result.success(cached))
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
                threadsCache.put(cacheKey, body)
                emit(Result.success(body))
            } catch (e: ApiError) {
                invalidateThreadsCache(cacheKey)
                emit(Result.failure(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                invalidateThreadsCache(cacheKey)
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    override fun getThread(threadId: String): Flow<RepositoryResult<Thread>> =
        flow {
            val cacheKey = "$contextHash:thread:$threadId"
            val cached = threadCache.get(cacheKey)
            if (cached != null) {
                emit(Result.success(cached))
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
                threadCache.put(cacheKey, body)
                emit(Result.success(body))
            } catch (e: ApiError) {
                invalidateThreadCache(cacheKey)
                emit(Result.failure(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                invalidateThreadCache(cacheKey)
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun invalidateThreadsCache(cacheKey: String) {
        threadsCache.remove(cacheKey)
        diskCache?.remove(cacheKey)
    }

    private suspend fun invalidateThreadCache(cacheKey: String) {
        threadCache.remove(cacheKey)
        diskCache?.remove(cacheKey)
    }

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
                configureAuth(apiKey)
            }

        return response.bodyOrThrow(logError = { e -> logger.w(e) { "Failed to read error response body" } })
    }

    private suspend fun fetchThread(threadId: String): Thread {
        val response =
            httpClient.get("$baseUrl/v1/threads/$threadId") {
                configureAuth(apiKey)
            }

        return response.bodyOrThrow(logError = { e -> logger.w(e) { "Failed to read error response body" } })
    }
}
