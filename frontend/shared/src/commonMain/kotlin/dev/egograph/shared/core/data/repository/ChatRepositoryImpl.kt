package dev.egograph.shared.core.data.repository

import dev.egograph.shared.core.data.repository.internal.bodyOrThrow
import dev.egograph.shared.core.data.repository.internal.configureAuth
import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.ChatResponse
import dev.egograph.shared.core.domain.model.ModelsResponse
import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.core.domain.model.StreamChunkType
import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.ChatRepository
import dev.egograph.shared.core.domain.repository.RepositoryResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    private val apiKey: String = "",
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        },
) : ChatRepository {
    override fun sendMessage(request: ChatRequest): Flow<RepositoryResult<StreamChunk>> =
        flow {
            try {
                val response =
                    httpClient.post("$baseUrl/v1/chat") {
                        contentType(ContentType.Application.Json)
                        configureAuth(apiKey)
                        setBody(request.copy(stream = true))
                    }

                if (response.status == HttpStatusCode.OK) {
                    val channel = response.bodyAsChannel()
                    val eventBuffer = StringBuilder()

                    while (!channel.isClosedForRead) {
                        currentCoroutineContext().ensureActive() // Check for cancellation
                        val line = channel.readUTF8Line()
                        if (line == null) {
                            break
                        }

                        if (line.isBlank()) {
                            if (eventBuffer.isNotEmpty()) {
                                emitSseEvent(eventBuffer.toString())
                                eventBuffer.clear()
                            }
                            continue
                        }

                        eventBuffer.appendLine(line)
                    }

                    if (eventBuffer.isNotBlank()) {
                        emitSseEvent(eventBuffer.toString())
                    }
                } else {
                    response.bodyOrThrow<Unit>(fallbackDetail = response.status.description)
                }
            } catch (e: ApiError) {
                emit(Result.failure(e))
            } catch (e: Exception) {
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<RepositoryResult<StreamChunk>>.emitSseEvent(event: String) {
        currentCoroutineContext().ensureActive() // Check for cancellation
        if (event.isBlank()) {
            return
        }

        val dataLines =
            event
                .lineSequence()
                .map { it.trimEnd() }
                .filter { it.startsWith("data:") }
                .toList()

        for (line in dataLines) {
            currentCoroutineContext().ensureActive() // Check for cancellation
            val payload = line.removePrefix("data:").trimStart()
            if (payload.isBlank() || payload == "[DONE]") {
                continue
            }
            emitChunk(payload)
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<RepositoryResult<StreamChunk>>.emitChunk(data: String) {
        currentCoroutineContext().ensureActive() // Check for cancellation
        try {
            val chunk = json.decodeFromString(StreamChunk.serializer(), data)
            if (chunk.type == StreamChunkType.ERROR) {
                throw ApiError.HttpError(
                    code = 500,
                    errorMessage = "Stream error",
                    detail = chunk.error,
                )
            }

            emit(Result.success(chunk))
        } catch (e: Exception) {
            if (e is ApiError) throw e
            return
        }
    }

    override suspend fun sendMessageSync(request: ChatRequest): RepositoryResult<ChatResponse> =
        try {
            val response =
                httpClient.post("$baseUrl/v1/chat") {
                    contentType(ContentType.Application.Json)
                    configureAuth(apiKey)
                    setBody(request.copy(stream = false))
                }

            Result.success(response.bodyOrThrow())
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }

    override suspend fun getModels(): RepositoryResult<ModelsResponse> =
        try {
            val response =
                httpClient.get("$baseUrl/v1/chat/models") {
                    configureAuth(apiKey)
                }

            Result.success(response.bodyOrThrow())
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }
}
