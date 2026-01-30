package dev.egograph.shared.repository

import dev.egograph.shared.dto.ChatRequest
import dev.egograph.shared.dto.ChatResponse
import dev.egograph.shared.dto.LLMModel
import dev.egograph.shared.dto.Message
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import dev.egograph.shared.dto.ThreadMessage
import dev.egograph.shared.dto.ThreadMessagesResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Repository層のテスト
 *
 * ApiErrorとRepositoryResultの動作を検証します。
 */
class RepositoryTest {

    /**
     * ApiError - sealed classのテスト
     */
    @Test
    fun `ApiError - all error types can be created`() {
        val networkError = ApiError.NetworkError(Exception("Network failed"))
        val httpError = ApiError.HttpError(404, "Not Found", "Resource not found")
        val serializationError = ApiError.SerializationError(Exception("Parse failed"))
        val unknownError = ApiError.UnknownError(Exception("Unknown"))
        val validationError = ApiError.ValidationError("Invalid input")

        assertEquals("Network error: Network failed", networkError.message)
        assertEquals("HTTP 404: Not Found - Resource not found", httpError.message)
        assertEquals("Serialization error: Parse failed", serializationError.message)
        assertEquals("Unknown error: Unknown", unknownError.message)
        assertEquals("Invalid input", validationError.message)
    }

    /**
     * ApiError - cause is propagated correctly
     */
    @Test
    fun `ApiError - cause is propagated correctly`() {
        val originalException = RuntimeException("Original error")
        val networkError = ApiError.NetworkError(originalException)
        val serializationError = ApiError.SerializationError(originalException)
        val unknownError = ApiError.UnknownError(originalException)

        assertEquals(originalException, networkError.cause)
        assertEquals(originalException, serializationError.cause)
        assertEquals(originalException, unknownError.cause)
    }

    /**
     * ApiError - HttpError fields are accessible
     */
    @Test
    fun `ApiError - HttpError fields are accessible`() {
        val httpError = ApiError.HttpError(500, "Internal Server Error", "Database connection failed")

        assertEquals(500, httpError.code)
        assertEquals("Internal Server Error", httpError.errorMessage)
        assertEquals("Database connection failed", httpError.detail)
    }

    /**
     * RepositoryResult - success case
     */
    @Test
    fun `RepositoryResult - returns success for valid result`() {
        val result: RepositoryResult<String> = Result.success("success")

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("success", result.getOrNull())
        assertEquals("success", result.getOrThrow())
    }

    /**
     * RepositoryResult - failure case
     */
    @Test
    fun `RepositoryResult - returns failure for exception`() {
        val result: RepositoryResult<String> = Result.failure(ApiError.ValidationError("test error"))

        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is ApiError.ValidationError)
    }

    /**
     * RepositoryResult - exception message is preserved
     */
    @Test
    fun `RepositoryResult - exception message is preserved`() {
        val result: RepositoryResult<String> = Result.failure(
            ApiError.HttpError(404, "Not Found", "Resource not found")
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? ApiError.HttpError
        assertEquals(404, exception?.code)
        assertEquals("Not Found", exception?.errorMessage)
    }

    /**
     * Repository interfaces - can be implemented
     */
    @Test
    fun `Repository interfaces - can be implemented`() {
        // Create mock implementations to verify interfaces are correctly defined
        val mockThreadRepo = object : ThreadRepository {
            override fun getThreads(limit: Int, offset: Int) = kotlinx.coroutines.flow.flow<RepositoryResult<ThreadListResponse>> {
                emit(Result.success(ThreadListResponse(emptyList(), 0, limit, offset)))
            }
            override fun getThread(threadId: String) = kotlinx.coroutines.flow.flow<RepositoryResult<Thread>> {
                emit(Result.success(Thread("", "", "", null, 0, "", "")))
            }
            override suspend fun createThread(title: String) = Result.success(
                Thread("", "", title, null, 0, "", "")
            )
        }

        val mockMessageRepo = object : MessageRepository {
            override fun getMessages(threadId: String) = kotlinx.coroutines.flow.flow<RepositoryResult<ThreadMessagesResponse>> {
                emit(Result.success(ThreadMessagesResponse(threadId, emptyList())))
            }
        }

        val mockChatRepo = object : ChatRepository {
            override fun sendMessage(request: ChatRequest) = kotlinx.coroutines.flow.flow<RepositoryResult<dev.egograph.shared.dto.StreamChunk>> {
            }
            override suspend fun sendMessageSync(request: ChatRequest) = Result.success(
                ChatResponse("", Message(MessageRole.ASSISTANT, "Response"), null, null, "", null)
            )
            override suspend fun getModels() = Result.success(emptyList<LLMModel>())
        }

        // Verify repositories can be created
        assertTrue(mockThreadRepo is ThreadRepository)
        assertTrue(mockMessageRepo is MessageRepository)
        assertTrue(mockChatRepo is ChatRepository)
    }

    /**
     * RepositoryResult - fold works correctly
     */
    @Test
    fun `RepositoryResult - fold works correctly`() {
        val successResult: RepositoryResult<String> = Result.success("value")
        val failureResult: RepositoryResult<String> = Result.failure(
            ApiError.ValidationError("error")
        )

        val successValue = successResult.fold(
            onSuccess = { it },
            onFailure = { "failure" }
        )
        assertEquals("value", successValue)

        val failureValue = failureResult.fold(
            onSuccess = { "success" },
            onFailure = { "failure: ${it.message}" }
        )
        assertEquals("failure: error", failureValue)
    }

    /**
     * RepositoryResult - map transforms success value
     */
    @Test
    fun `RepositoryResult - map transforms success value`() {
        val result: RepositoryResult<Int> = Result.success(5)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(10, mapped.getOrNull())
    }

    /**
     * RepositoryResult - mapCatches handles exceptions
     */
    @Test
    fun `RepositoryResult - mapCatches handles exceptions`() {
        val result: RepositoryResult<Int> = Result.failure(
            ApiError.NetworkError(Exception("Network error"))
        )
        val mapped = result.mapCatching { it * 2 }

        assertTrue(mapped.isFailure)
        assertTrue(mapped.exceptionOrNull() is ApiError.NetworkError)
    }
}
