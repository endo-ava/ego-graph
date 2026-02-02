package dev.egograph.shared.repository

import co.touchlab.kermit.Logger
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
import java.util.concurrent.atomic.AtomicReference

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
) : SystemPromptRepository {
    private val logger = Logger.withTag("SystemPromptRepository")

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val systemPromptCache =
        AtomicReference<Map<SystemPromptName, CacheEntry<SystemPromptResponse>>>(emptyMap())
    private val cacheDurationMs = 5000L

    override suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse> =
        try {
            val cached = systemPromptCache.get()[name]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheDurationMs) {
                return Result.success(cached.data)
            }
            val response =
                httpClient.get("$baseUrl/v1/system-prompts/${name.apiName}") {
                    if (apiKey.isNotEmpty()) {
                        headers {
                            append("X-API-Key", apiKey)
                        }
                    }
                }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.body<SystemPromptResponse>()
                    systemPromptCache.updateAndGet { current -> current + (name to CacheEntry(body)) }
                    Result.success(body)
                }
                else -> {
                    systemPromptCache.updateAndGet { current -> current - name }
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
        } catch (e: Exception) {
            systemPromptCache.updateAndGet { current -> current - name }
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
                HttpStatusCode.OK -> Result.success(response.body())
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
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }
}
