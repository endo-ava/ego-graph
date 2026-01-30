package dev.egograph.shared.repository

import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * ThreadRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 */
class ThreadRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : ThreadRepository {

    override fun getThreads(limit: Int, offset: Int): Flow<RepositoryResult<ThreadListResponse>> = flow {
        val response = httpClient.get("$baseUrl/v1/threads") {
            parameter("limit", limit)
            parameter("offset", offset)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                emit(Result.success(response.body<ThreadListResponse>()))
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

    override fun getThread(threadId: String): Flow<RepositoryResult<Thread>> = flow {
        val response = httpClient.get("$baseUrl/v1/threads/$threadId")

        when (response.status) {
            HttpStatusCode.OK -> {
                emit(Result.success(response.body<Thread>()))
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

    override suspend fun createThread(title: String): RepositoryResult<Thread> {
        return Result.failure(
            ApiError.HttpError(
                code = 501,
                errorMessage = "Not Implemented",
                detail = "Thread creation is not yet supported"
            )
        )
    }
}
