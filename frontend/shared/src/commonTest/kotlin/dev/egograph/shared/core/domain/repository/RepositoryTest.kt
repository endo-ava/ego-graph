package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.ChatResponse
import dev.egograph.shared.core.domain.model.Message
import dev.egograph.shared.core.domain.model.MessageRole
import dev.egograph.shared.core.domain.model.ModelsResponse
import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.core.domain.model.Thread
import dev.egograph.shared.core.domain.model.ThreadListResponse
import dev.egograph.shared.core.domain.model.ThreadMessagesResponse
import kotlinx.coroutines.flow.flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        // Arrange
        val networkError = ApiError.NetworkError(Exception("Network failed"))
        val httpError = ApiError.HttpError(404, "Not Found", "Resource not found")
        val serializationError = ApiError.SerializationError(Exception("Parse failed"))
        val unknownError = ApiError.UnknownError(Exception("Unknown"))
        val validationError = ApiError.ValidationError("Invalid input")

        // Act
        val networkMessage = networkError.message
        val httpMessage = httpError.message
        val serializationMessage = serializationError.message
        val unknownMessage = unknownError.message
        val validationMessage = validationError.message

        // Assert
        assertEquals("Network error: Network failed", networkMessage)
        assertEquals("HTTP 404: Not Found - Resource not found", httpMessage)
        assertEquals("Serialization error: Parse failed", serializationMessage)
        assertEquals("Unknown error: Unknown", unknownMessage)
        assertEquals("Invalid input", validationMessage)
    }

    /**
     * ApiError - cause is propagated correctly
     */
    @Test
    fun `ApiError - cause is propagated correctly`() {
        // Arrange
        val originalException = RuntimeException("Original error")
        val networkError = ApiError.NetworkError(originalException)
        val serializationError = ApiError.SerializationError(originalException)
        val unknownError = ApiError.UnknownError(originalException)

        // Act
        val networkCause = networkError.cause
        val serializationCause = serializationError.cause
        val unknownCause = unknownError.cause

        // Assert
        assertEquals(originalException, networkCause)
        assertEquals(originalException, serializationCause)
        assertEquals(originalException, unknownCause)
    }

    /**
     * ApiError - HttpError fields are accessible
     */
    @Test
    fun `ApiError - HttpError fields are accessible`() {
        // Arrange
        val httpError = ApiError.HttpError(500, "Internal Server Error", "Database connection failed")

        // Act
        val code = httpError.code
        val errorMessage = httpError.errorMessage
        val detail = httpError.detail

        // Assert
        assertEquals(500, code)
        assertEquals("Internal Server Error", errorMessage)
        assertEquals("Database connection failed", detail)
    }

    /**
     * RepositoryResult - success case
     */
    @Test
    fun `RepositoryResult - returns success for valid result`() {
        // Arrange
        val result: RepositoryResult<String> = Result.success("success")

        // Act
        val isSuccess = result.isSuccess
        val isFailure = result.isFailure
        val orNull = result.getOrNull()
        val orThrow = result.getOrThrow()

        // Assert
        assertTrue(isSuccess)
        assertFalse(isFailure)
        assertEquals("success", orNull)
        assertEquals("success", orThrow)
    }

    /**
     * RepositoryResult - failure case
     */
    @Test
    fun `RepositoryResult - returns failure for exception`() {
        // Arrange
        val result: RepositoryResult<String> = Result.failure(ApiError.ValidationError("test error"))

        // Act
        val isFailure = result.isFailure
        val isSuccess = result.isSuccess
        val exceptionOrNull = result.exceptionOrNull()

        // Assert
        assertTrue(isFailure)
        assertFalse(isSuccess)
        assertTrue(exceptionOrNull is ApiError.ValidationError)
    }

    /**
     * RepositoryResult - exception message is preserved
     */
    @Test
    fun `RepositoryResult - exception message is preserved`() {
        // Arrange
        val result: RepositoryResult<String> =
            Result.failure(
                ApiError.HttpError(404, "Not Found", "Resource not found"),
            )

        // Act
        val isFailure = result.isFailure
        val exception = result.exceptionOrNull()

        // Assert
        assertTrue(isFailure)
        assertNotNull(exception)
        assertTrue(exception is ApiError.HttpError)
        assertEquals(404, exception.code)
        assertEquals("Not Found", exception.errorMessage)
    }

    /**
     * Repository interfaces - can be implemented
     */
    @Test
    fun `Repository interfaces - can be implemented`() {
        // Arrange
        val mockThreadRepo =
            object : ThreadRepository {
                override fun getThreads(
                    limit: Int,
                    offset: Int,
                ) = flow<RepositoryResult<ThreadListResponse>> {
                    emit(Result.success(ThreadListResponse(emptyList(), 0, limit, offset)))
                }

                override fun getThread(threadId: String) =
                    flow<RepositoryResult<Thread>> {
                        emit(Result.success(Thread("", "", "", null, 0, "", "")))
                    }

                override suspend fun createThread(title: String) =
                    Result.success(
                        Thread("", "", title, null, 0, "", ""),
                    )
            }

        val mockMessageRepo =
            object : MessageRepository {
                override fun getMessages(threadId: String) =
                    flow<RepositoryResult<ThreadMessagesResponse>> {
                        emit(Result.success(ThreadMessagesResponse(threadId, emptyList())))
                    }

                override suspend fun invalidateCache(threadId: String) {
                    // No-op for test
                }
            }

        val mockChatRepo =
            object : ChatRepository {
                override fun sendMessage(request: ChatRequest) =
                    flow<RepositoryResult<StreamChunk>> {
                    }

                override suspend fun sendMessageSync(request: ChatRequest) =
                    Result.success(
                        ChatResponse(
                            "",
                            Message(MessageRole.ASSISTANT, "Response"),
                            null,
                            null,
                            "",
                            null,
                        ),
                    )

                override suspend fun getModels() =
                    Result.success(
                        ModelsResponse(
                            models = emptyList(),
                            defaultModel = "deepseek/deepseek-v3.2",
                        ),
                    )
            }

        // Act
        // Repository interfaces are implemented and instantiated

        // Assert
        // If we reach here, the interfaces are correctly defined
        assertNotNull(mockThreadRepo)
        assertNotNull(mockMessageRepo)
        assertNotNull(mockChatRepo)
    }

    /**
     * RepositoryResult - fold works correctly
     */
    @Test
    fun `RepositoryResult - fold works correctly`() {
        // Arrange
        val successResult: RepositoryResult<String> = Result.success("value")
        val failureResult: RepositoryResult<String> =
            Result.failure(
                ApiError.ValidationError("error"),
            )

        // Act
        val successValue =
            successResult.fold(
                onSuccess = { it },
                onFailure = { "failure" },
            )
        val failureValue =
            failureResult.fold(
                onSuccess = { "success" },
                onFailure = { "failure: ${it.message}" },
            )

        // Assert
        assertEquals("value", successValue)
        assertEquals("failure: error", failureValue)
    }

    /**
     * RepositoryResult - map transforms success value
     */
    @Test
    fun `RepositoryResult - map transforms success value`() {
        // Arrange
        val result: RepositoryResult<Int> = Result.success(5)

        // Act
        val mapped = result.map { it * 2 }

        // Assert
        assertTrue(mapped.isSuccess)
        assertEquals(10, mapped.getOrNull())
    }

    /**
     * RepositoryResult - mapCatches handles exceptions
     */
    @Test
    fun `RepositoryResult - mapCatches handles exceptions`() {
        // Arrange
        val result: RepositoryResult<Int> =
            Result.failure(
                ApiError.NetworkError(Exception("Network error")),
            )

        // Act
        val mapped = result.mapCatching { it * 2 }

        // Assert
        assertTrue(mapped.isFailure)
        assertTrue(mapped.exceptionOrNull() is ApiError.NetworkError)
    }
}
