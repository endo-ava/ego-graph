package dev.egograph.shared.core.domain.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * ApiErrorの単体テスト
 *
 * 各エラー型の生成とメッセージ形式を検証します。
 */
class ApiErrorTest {
    /**
     * ValidationError - creates_with_message
     *
     * バリデーションエラーが指定したメッセージで正しく生成されることを検証します。
     */
    @Test
    fun `ValidationError - creates_with_message`() {
        // Arrange
        val errorMessage = "Invalid input parameter"

        // Act
        val validationError = ApiError.ValidationError(errorMessage)
        val message = validationError.message

        // Assert
        assertEquals(errorMessage, message)
        assertEquals(errorMessage, validationError.errorMessage)
    }

    /**
     * ValidationError - empty_message
     *
     * 空文字列のメッセージでも正しく動作することを検証します。
     */
    @Test
    fun `ValidationError - empty_message`() {
        // Arrange
        val errorMessage = ""

        // Act
        val validationError = ApiError.ValidationError(errorMessage)
        val message = validationError.message

        // Assert
        assertEquals(errorMessage, message)
    }

    /**
     * NetworkError - creates_with_cause
     *
     * ネットワークエラーが指定した原因例外で正しく生成されることを検証します。
     */
    @Test
    fun `NetworkError - creates_with_cause`() {
        // Arrange
        val causeException = RuntimeException("Connection timeout")

        // Act
        val networkError = ApiError.NetworkError(causeException)
        val message = networkError.message
        val cause = networkError.cause
        val exception = networkError.exception

        // Assert
        assertEquals("Network error: Connection timeout", message)
        assertEquals(causeException, cause)
        assertEquals(causeException, exception)
    }

    /**
     * NetworkError - null_cause_message
     *
     * 原因例外のメッセージがnullの場合でも正しく動作することを検証します。
     */
    @Test
    fun `NetworkError - null_cause_message`() {
        // Arrange
        val causeException = RuntimeException(null as String?)

        // Act
        val networkError = ApiError.NetworkError(causeException)
        val message = networkError.message

        // Assert
        assertEquals("Network error: null", message)
    }

    /**
     * HttpError - creates_with_code_and_message
     *
     * HTTPエラーが指定したステータスコードとメッセージで正しく生成されることを検証します。
     */
    @Test
    fun `HttpError - creates_with_code_and_message`() {
        // Arrange
        val code = 404
        val errorMessage = "Not Found"
        val detail = "Resource not found"

        // Act
        val httpError = ApiError.HttpError(code, errorMessage, detail)
        val message = httpError.message

        // Assert
        assertEquals(code, httpError.code)
        assertEquals(errorMessage, httpError.errorMessage)
        assertEquals(detail, httpError.detail)
        assertEquals("HTTP 404: Not Found - Resource not found", message)
    }

    /**
     * HttpError - creates_without_detail
     *
     * 詳細情報なしでも正しく動作することを検証します。
     */
    @Test
    fun `HttpError - creates_without_detail`() {
        // Arrange
        val code = 401
        val errorMessage = "Unauthorized"

        // Act
        val httpError = ApiError.HttpError(code, errorMessage, null)
        val message = httpError.message

        // Assert
        assertEquals(code, httpError.code)
        assertEquals(errorMessage, httpError.errorMessage)
        assertEquals(null, httpError.detail)
        assertEquals("HTTP 401: Unauthorized", message)
    }

    /**
     * SerializationError - creates_with_cause
     *
     * シリアライゼーションエラーが指定した原因例外で正しく生成されることを検証します。
     */
    @Test
    fun `SerializationError - creates_with_cause`() {
        // Arrange
        val causeException = RuntimeException("JSON parse failed")

        // Act
        val serializationError = ApiError.SerializationError(causeException)
        val message = serializationError.message
        val cause = serializationError.cause
        val exception = serializationError.exception

        // Assert
        assertEquals("Serialization error: JSON parse failed", message)
        assertEquals(causeException, cause)
        assertEquals(causeException, exception)
    }

    /**
     * UnknownError - creates_with_cause
     *
     * 不明なエラーが指定した原因例外で正しく生成されることを検証します。
     */
    @Test
    fun `UnknownError - creates_with_cause`() {
        // Arrange
        val causeException = RuntimeException("Unexpected error occurred")

        // Act
        val unknownError = ApiError.UnknownError(causeException)
        val message = unknownError.message
        val cause = unknownError.cause
        val exception = unknownError.exception

        // Assert
        assertEquals("Unknown error: Unexpected error occurred", message)
        assertEquals(causeException, cause)
        assertEquals(causeException, exception)
    }

    /**
     * ApiError - sealed_class_hierarchy
     *
     * ApiErrorがsealed classとして正しく機能することを検証します。
     */
    @Test
    fun `ApiError - sealed_class_hierarchy`() {
        // Arrange
        val networkError: ApiError = ApiError.NetworkError(Exception("Network failed"))
        val httpError: ApiError = ApiError.HttpError(500, "Internal Server Error", "DB failed")
        val serializationError: ApiError = ApiError.SerializationError(Exception("Parse failed"))
        val unknownError: ApiError = ApiError.UnknownError(Exception("Unknown"))
        val validationError: ApiError = ApiError.ValidationError("Invalid input")

        // Act
        val errors = listOf(networkError, httpError, serializationError, unknownError, validationError)
        val allAreApiError = errors.all { it is ApiError }

        // Assert
        assertEquals(5, errors.size)
        assertEquals(true, allAreApiError)
    }

    /**
     * ApiError - exception_type_check
     *
     * 各エラー型の型チェックが正しく機能することを検証します。
     */
    @Test
    fun `ApiError - exception_type_check`() {
        // Arrange
        val errors: List<ApiError> =
            listOf(
                ApiError.NetworkError(Exception("Network failed")),
                ApiError.HttpError(404, "Not Found", null),
                ApiError.SerializationError(Exception("Parse failed")),
                ApiError.UnknownError(Exception("Unknown")),
                ApiError.ValidationError("Invalid input"),
            )

        // Act
        val networkError = errors[0] as? ApiError.NetworkError
        val httpError = errors[1] as? ApiError.HttpError
        val serializationError = errors[2] as? ApiError.SerializationError
        val unknownError = errors[3] as? ApiError.UnknownError
        val validationError = errors[4] as? ApiError.ValidationError

        // Assert
        assertNotNull(networkError)
        assertNotNull(httpError)
        assertNotNull(serializationError)
        assertNotNull(unknownError)
        assertNotNull(validationError)
    }
}
