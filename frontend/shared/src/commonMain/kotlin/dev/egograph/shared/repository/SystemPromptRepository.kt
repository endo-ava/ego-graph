package dev.egograph.shared.repository

import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.dto.SystemPromptName
import dev.egograph.shared.dto.SystemPromptResponse
import dev.egograph.shared.dto.SystemPromptUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException

interface SystemPromptRepository {
    suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse>

    suspend fun updateSystemPrompt(
        name: SystemPromptName,
        content: String,
    ): RepositoryResult<SystemPromptResponse>
}

class SystemPromptRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String = "",
    private val diskCache: DiskCache? = null,
) : SystemPromptRepository {
    private val systemPromptCache = InMemoryCache<SystemPromptName, SystemPromptResponse>()

    override suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse> =
        try {
            val cached = systemPromptCache.get(name)
            if (cached != null) {
                Result.success(cached)
            } else {
                val body =
                    diskCache?.getOrFetch(
                        key = name.apiName,
                        serializer = SystemPromptResponse.serializer(),
                    ) {
                        fetchSystemPrompt(name)
                    } ?: fetchSystemPrompt(name)

                systemPromptCache.put(name, body)
                Result.success(body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiError) {
            invalidateCache(name)
            Result.failure(e)
        } catch (e: Exception) {
            invalidateCache(name)
            Result.failure(ApiError.NetworkError(e))
        }

    override suspend fun updateSystemPrompt(
        name: SystemPromptName,
        content: String,
    ): RepositoryResult<SystemPromptResponse> =
        try {
            val response =
                httpClient.put("$baseUrl/v1/system-prompts/${name.apiName}") {
                    contentType(ContentType.Application.Json)
                    configureAuth(apiKey)
                    setBody(SystemPromptUpdateRequest(content))
                }

            val body = response.bodyOrThrow<SystemPromptResponse>()
            invalidateCache(name)
            Result.success(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }

    private suspend fun invalidateCache(name: SystemPromptName) {
        systemPromptCache.remove(name)
        diskCache?.remove(name.apiName)
    }

    private suspend fun fetchSystemPrompt(name: SystemPromptName): SystemPromptResponse =
        httpClient
            .get("$baseUrl/v1/system-prompts/${name.apiName}") {
                configureAuth(apiKey)
            }.bodyOrThrow()
}
