package dev.egograph.shared.core.data.repository

import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.core.data.repository.internal.InMemoryCache
import dev.egograph.shared.core.data.repository.internal.RepositoryClient
import dev.egograph.shared.core.domain.model.Thread
import dev.egograph.shared.core.domain.model.ThreadListResponse
import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.RepositoryResult
import dev.egograph.shared.core.domain.repository.ThreadRepository
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * ThreadRepositoryの実装
 *
 * RepositoryClientを使用してバックエンドAPIと通信します。
 */
class ThreadRepositoryImpl(
    private val repositoryClient: RepositoryClient,
    private val diskCache: DiskCache? = null,
) : ThreadRepository,
    BaseRepository {
    private val threadsCache = InMemoryCache<String, ThreadListResponse>()
    private val threadCache = InMemoryCache<String, Thread>()

    override fun getThreads(
        limit: Int,
        offset: Int,
    ): Flow<RepositoryResult<ThreadListResponse>> =
        flow {
            val cacheKey = "threads:list:$limit:$offset"
            val cached = threadsCache.get(cacheKey)
            if (cached != null) {
                emit(Result.success(cached))
                return@flow
            }

            val result =
                wrapRepositoryOperation {
                    diskCache?.getOrFetch(
                        key = cacheKey,
                        serializer = ThreadListResponse.serializer(),
                    ) {
                        repositoryClient.get<ThreadListResponse>("/v1/threads") {
                            parameter("limit", limit)
                            parameter("offset", offset)
                        }
                    } ?: repositoryClient.get<ThreadListResponse>("/v1/threads") {
                        parameter("limit", limit)
                        parameter("offset", offset)
                    }
                }

            result.onSuccess { threadsCache.put(cacheKey, it) }
            result.onFailure { invalidateThreadsCache(cacheKey) }
            emit(result)
        }.flowOn(Dispatchers.IO)

    override fun getThread(threadId: String): Flow<RepositoryResult<Thread>> =
        flow {
            val cacheKey = "thread:$threadId"
            val cached = threadCache.get(cacheKey)
            if (cached != null) {
                emit(Result.success(cached))
                return@flow
            }

            val result =
                wrapRepositoryOperation {
                    diskCache?.getOrFetch(
                        key = cacheKey,
                        serializer = Thread.serializer(),
                    ) {
                        repositoryClient.get<Thread>("/v1/threads/$threadId")
                    } ?: repositoryClient.get<Thread>("/v1/threads/$threadId")
                }

            result.onSuccess { threadCache.put(cacheKey, it) }
            result.onFailure { invalidateThreadCache(cacheKey) }
            emit(result)
        }.flowOn(Dispatchers.IO)

    override suspend fun createThread(title: String): RepositoryResult<Thread> =
        Result.failure(
            ApiError.HttpError(
                code = 501,
                errorMessage = "Not Implemented",
                detail = "Thread creation is not yet supported",
            ),
        )

    private suspend fun invalidateThreadsCache(cacheKey: String) {
        threadsCache.remove(cacheKey)
        diskCache?.remove(cacheKey)
    }

    private suspend fun invalidateThreadCache(cacheKey: String) {
        threadCache.remove(cacheKey)
        diskCache?.remove(cacheKey)
    }
}
