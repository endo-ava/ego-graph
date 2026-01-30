package dev.egograph.shared.repository

import dev.egograph.shared.dto.ThreadMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * MessageRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 */
class MessageRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : MessageRepository {

    override fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>> = flow {
        val response = httpClient.get("$baseUrl/v1/threads/$threadId/messages")

        when (response.status) {
            HttpStatusCode.OK -> {
                emit(Result.success(response.body<ThreadMessagesResponse>()))
            }
            else -> {
                val errorDetail = try { response.body<String>() } catch (e: Exception) { null }
                emit(Result.failure(
                    ApiError.HttpError(
                        code = response.status.value,
                        errorMessage = response.status.description,
                        detail = errorDetail
                    )
                ))
            }
        }
    }
}
