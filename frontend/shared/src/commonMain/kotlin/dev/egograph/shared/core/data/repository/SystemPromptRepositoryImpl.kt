package dev.egograph.shared.core.data.repository

import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.core.data.repository.internal.InMemoryCache
import dev.egograph.shared.core.data.repository.internal.RepositoryClient
import dev.egograph.shared.core.domain.model.SystemPromptName
import dev.egograph.shared.core.domain.model.SystemPromptResponse
import dev.egograph.shared.core.domain.model.SystemPromptUpdateRequest
import dev.egograph.shared.core.domain.repository.RepositoryResult
import dev.egograph.shared.core.domain.repository.SystemPromptRepository

/**
 * SystemPromptRepositoryの実装
 *
 * RepositoryClientを使用してバックエンドAPIと通信します。
 */
class SystemPromptRepositoryImpl(
    private val repositoryClient: RepositoryClient,
    private val diskCache: DiskCache? = null,
) : SystemPromptRepository,
    BaseRepository {
    private val systemPromptCache = InMemoryCache<SystemPromptName, SystemPromptResponse>()

    override suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse> {
        val cached = systemPromptCache.get(name)
        if (cached != null) {
            return Result.success(cached)
        }

        return wrapRepositoryOperation {
            val body =
                diskCache?.getOrFetch(
                    key = name.apiName,
                    serializer = SystemPromptResponse.serializer(),
                ) {
                    repositoryClient.get<SystemPromptResponse>("/v1/system-prompts/${name.apiName}")
                } ?: repositoryClient.get<SystemPromptResponse>("/v1/system-prompts/${name.apiName}")

            systemPromptCache.put(name, body)
            body
        }.onFailure { invalidateCache(name) }
    }

    override suspend fun updateSystemPrompt(
        name: SystemPromptName,
        content: String,
    ): RepositoryResult<SystemPromptResponse> =
        wrapRepositoryOperation {
            val response =
                repositoryClient.put<SystemPromptResponse>(
                    "/v1/system-prompts/${name.apiName}",
                    SystemPromptUpdateRequest(content),
                )
            invalidateCache(name)
            response
        }

    private suspend fun invalidateCache(name: SystemPromptName) {
        systemPromptCache.remove(name)
        diskCache?.remove(name.apiName)
    }
}
