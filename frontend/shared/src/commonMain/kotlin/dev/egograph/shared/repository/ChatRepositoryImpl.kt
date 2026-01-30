package dev.egograph.shared.repository

import dev.egograph.shared.dto.ChatRequest
import dev.egograph.shared.dto.ChatResponse
import dev.egograph.shared.dto.LLMModel
import dev.egograph.shared.dto.ModelsResponse
import dev.egograph.shared.dto.StreamChunk
import dev.egograph.shared.dto.StreamChunkType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * ChatRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 * ストリーミングレスポンスにはServer-Sent Events (SSE)を使用します。
 */
class ChatRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) : ChatRepository {

    override fun sendMessage(request: ChatRequest): Flow<RepositoryResult<StreamChunk>> = flow {
        try {
            val response = httpClient.post("$baseUrl/v1/chat") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(request.copy(stream = true))
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseBody = response.body<String>()
                    parseSSEChunks(responseBody)
                }
                else -> {
                    val errorDetail = try {
                        response.body<String>()
                    } catch (e: Exception) {
                        response.status.description
                    }
                    emit(Result.failure(
                        ApiError.HttpError(
                            code = response.status.value,
                            errorMessage = response.status.description,
                            detail = errorDetail
                        )
                    ))
                }
            }
        } catch (e: ApiError) {
            emit(Result.failure(e))
        } catch (e: Exception) {
            emit(Result.failure(ApiError.NetworkError(e)))
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<RepositoryResult<StreamChunk>>.parseSSEChunks(responseBody: String) {
        val lines = responseBody.split("\n")
        for (line in lines) {
            if (line.startsWith("data: ")) {
                try {
                    val jsonData = line.substring(6)
                    val chunk = json.decodeFromString(StreamChunk.serializer(), jsonData)
                    emit(Result.success(chunk))

                    if (chunk.type == StreamChunkType.ERROR) {
                        throw ApiError.HttpError(
                            code = 500,
                            errorMessage = "Stream error",
                            detail = chunk.error
                        )
                    }
                } catch (e: Exception) {
                    if (e is ApiError) throw e
                    emit(Result.failure(ApiError.SerializationError(e)))
                }
            }
        }
    }

    override suspend fun sendMessageSync(request: ChatRequest): RepositoryResult<ChatResponse> {
        return try {
            val response = httpClient.post("$baseUrl/v1/chat") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(request.copy(stream = false))
            }

            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body<ChatResponse>())
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

    override suspend fun getModels(): RepositoryResult<List<LLMModel>> {
        return try {
            val response = httpClient.get("$baseUrl/v1/chat/models")

            when (response.status) {
                HttpStatusCode.OK -> {
                    val modelsResponse = response.body<ModelsResponse>()
                    Result.success(modelsResponse.models)
                }
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

    /**
     * SSEチャンクをパースする
     */
    private fun parseSSEChunk(data: String): StreamChunk {
        return try {
            json.decodeFromString(StreamChunk.serializer(), data)
        } catch (e: Exception) {
            throw ApiError.SerializationError(e)
        }
    }
}
