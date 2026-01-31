package dev.egograph.shared.repository

import dev.egograph.shared.dto.SystemPromptName
import dev.egograph.shared.dto.SystemPromptResponse
import dev.egograph.shared.dto.SystemPromptUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

interface SystemPromptRepository {
    suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse>
    suspend fun updateSystemPrompt(name: SystemPromptName, content: String): RepositoryResult<SystemPromptResponse>
}

class SystemPromptRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : SystemPromptRepository {

    override suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse> {
        return try {
            val response = httpClient.get("$baseUrl/v1/system-prompts/${name.apiName}")

            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body())
                else -> {
                    val errorDetail = try { response.body<String>() } catch (e: Exception) { null }
                    Result.failure(
                        ApiError.HttpError(
                            code = response.status.value,
                            errorMessage = response.status.description,
                            detail = errorDetail
                        )
                    )
                }
            }
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }
    }

    override suspend fun updateSystemPrompt(name: SystemPromptName, content: String): RepositoryResult<SystemPromptResponse> {
        return try {
            val response = httpClient.put("$baseUrl/v1/system-prompts/${name.apiName}") {
                contentType(ContentType.Application.Json)
                setBody(SystemPromptUpdateRequest(content))
            }

            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body())
                else -> {
                    val errorDetail = try { response.body<String>() } catch (e: Exception) { null }
                    Result.failure(
                        ApiError.HttpError(
                            code = response.status.value,
                            errorMessage = response.status.description,
                            detail = errorDetail
                        )
                    )
                }
            }
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }
    }
}
