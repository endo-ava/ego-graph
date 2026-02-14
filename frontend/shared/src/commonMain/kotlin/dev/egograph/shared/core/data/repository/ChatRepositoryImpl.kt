package dev.egograph.shared.core.data.repository

import dev.egograph.shared.core.data.repository.internal.RepositoryClient
import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.ChatResponse
import dev.egograph.shared.core.domain.model.ModelsResponse
import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.core.domain.model.StreamChunkType
import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.ChatRepository
import dev.egograph.shared.core.domain.repository.RepositoryResult
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * ChatRepositoryの実装
 *
 * RepositoryClientを使用してバックエンドAPIと通信します。
 * ストリーミングレスポンスにはServer-Sent Events (SSE)を使用します。
 */
class ChatRepositoryImpl(
    private val repositoryClient: RepositoryClient,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        },
) : ChatRepository,
    BaseRepository {
    override fun sendMessage(request: ChatRequest): Flow<RepositoryResult<StreamChunk>> =
        flow {
            try {
                val response =
                    repositoryClient.postWithResponse("/v1/chat", request.copy(stream = true)) {
                        contentType(ContentType.Application.Json)
                    }

                val channel = response.bodyAsChannel()
                val eventBuffer = StringBuilder()

                while (!channel.isClosedForRead) {
                    currentCoroutineContext().ensureActive()
                    val line = channel.readUTF8Line() ?: break

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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: ApiError) {
                emit(Result.failure(e))
            } catch (e: Exception) {
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<RepositoryResult<StreamChunk>>.emitSseEvent(event: String) {
        currentCoroutineContext().ensureActive()
        if (event.isBlank()) return

        val dataLines =
            event
                .lineSequence()
                .map { it.trimEnd() }
                .filter { it.startsWith("data:") }
                .toList()

        for (line in dataLines) {
            currentCoroutineContext().ensureActive()
            val payload = line.removePrefix("data:").trimStart()
            if (payload.isBlank() || payload == "[DONE]") continue
            emitChunk(payload)
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<RepositoryResult<StreamChunk>>.emitChunk(data: String) {
        currentCoroutineContext().ensureActive()
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SerializationException) {
            emit(Result.failure(ApiError.SerializationError(e)))
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            emit(Result.failure(ApiError.UnknownError(e)))
        }
    }

    override suspend fun sendMessageSync(request: ChatRequest): RepositoryResult<ChatResponse> =
        wrapRepositoryOperation {
            repositoryClient.post<ChatResponse>("/v1/chat", request.copy(stream = false))
        }

    override suspend fun getModels(): RepositoryResult<ModelsResponse> =
        wrapRepositoryOperation {
            repositoryClient.get<ModelsResponse>("/v1/chat/models")
        }
}
