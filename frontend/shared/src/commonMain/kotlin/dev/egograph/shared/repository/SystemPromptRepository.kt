package dev.egograph.shared.repository

import co.touchlab.kermit.Logger
import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.dto.SystemPromptName
import dev.egograph.shared.dto.SystemPromptResponse
import dev.egograph.shared.dto.SystemPromptUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val logger = Logger.withTag("SystemPromptRepository")

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val systemPromptCacheMutex = Mutex()
    private var systemPromptCache: Map<SystemPromptName, CacheEntry<SystemPromptResponse>> = emptyMap()
    private val cacheDurationMs = 60000L

    override suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse> =
        try {
            val cached = systemPromptCacheMutex.withLock { systemPromptCache[name] }
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                Result.success(cached.data)
            } else {
                val cacheKey = name.apiName
                val body =
                    if (diskCache != null) {
                        diskCache.getOrFetch(
                            key = cacheKey,
                            serializer = SystemPromptResponse.serializer(),
                        ) {
                            fetchSystemPrompt(name)
                        }
                    } else {
                        fetchSystemPrompt(name)
                    }
                systemPromptCacheMutex.withLock {
                    systemPromptCache = systemPromptCache + (name to CacheEntry(body))
                }
                Result.success(body)
            }
        } catch (e: ApiError) {
            systemPromptCacheMutex.withLock {
                systemPromptCache = systemPromptCache - name
            }
            diskCache?.remove(name.apiName)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            systemPromptCacheMutex.withLock {
                systemPromptCache = systemPromptCache - name
            }
            diskCache?.remove(name.apiName)
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
                    if (apiKey.isNotEmpty()) {
                        headers {
                            append("X-API-Key", apiKey)
                        }
                    }
                    setBody(SystemPromptUpdateRequest(content))
                }

            when (response.status) {
                HttpStatusCode.OK -> {
                    systemPromptCacheMutex.withLock {
                        systemPromptCache = systemPromptCache - name
                    }
                    diskCache?.remove(name.apiName)
                    Result.success(response.body())
                }
                else -> {
                    val errorDetail =
                        try {
                            response.body<String>()
                        } catch (e: Exception) {
                            logger.w(e) { "Failed to read error response body" }
                            null
                        }
                    Result.failure(
                        ApiError.HttpError(
                            code = response.status.value,
                            errorMessage = response.status.description,
                            detail = errorDetail,
                        ),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }

    private suspend fun fetchSystemPrompt(name: SystemPromptName): SystemPromptResponse {
        val response =
            httpClient.get("$baseUrl/v1/system-prompts/${name.apiName}") {
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
