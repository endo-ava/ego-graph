package dev.egograph.shared.features.chat

import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.ErrorAction
import dev.egograph.shared.core.domain.repository.ErrorSeverity
import dev.egograph.shared.core.domain.repository.TimeoutType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [ChatErrorState]のテスト
 *
 * [ApiError]から[ChatErrorState]への変換ロジックを検証します。
 */
class ChatErrorStateTest {
    @Test
    fun `NetworkError maps correctly`() {
        val networkError =
            ApiError.NetworkError(
                exception = RuntimeException("Connection failed"),
                isRetryable = true,
                suggestedAction = ErrorAction.RETRY,
                severity = ErrorSeverity.ERROR,
            )

        val errorState = networkError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.NETWORK, errorState.type)
        assertEquals("ネットワークエラーが発生しました", errorState.message)
        assertEquals("Connection failed", errorState.detail)
        assertEquals(ErrorAction.RETRY, errorState.action)
        assertTrue(errorState.canRetry)
        assertTrue(errorState.isRetryable)
        assertEquals(ErrorSeverity.ERROR, errorState.severity)
    }

    @Test
    fun `TimeoutError shows retry action`() {
        val timeoutError =
            ApiError.TimeoutError(
                timeoutType = TimeoutType.STREAMING,
                timeoutMillis = 300_000,
                suggestedAction = ErrorAction.RETRY,
                severity = ErrorSeverity.WARNING,
            )

        val errorState = timeoutError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.TIMEOUT, errorState.type)
        assertEquals("応答がタイムアウトしました", errorState.message)
        assertEquals(ErrorAction.RETRY, errorState.action)
        assertTrue(errorState.canRetry)
        assertTrue(errorState.isRetryable)
        assertEquals(ErrorSeverity.WARNING, errorState.severity)
    }

    @Test
    fun `AuthenticationError shows no retry`() {
        val authError =
            ApiError.AuthenticationError(
                errorMessage = "認証に失敗しました",
                detail = "APIキーが無効です",
                suggestedAction = ErrorAction.REAUTHENTICATE,
                severity = ErrorSeverity.ERROR,
            )

        val errorState = authError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.AUTHENTICATION, errorState.type)
        assertEquals("認証に失敗しました", errorState.message)
        assertEquals("APIキーが無効です", errorState.detail)
        assertEquals(ErrorAction.REAUTHENTICATE, errorState.action)
        assertFalse(errorState.canRetry)
        assertFalse(errorState.isRetryable)
        assertEquals(ErrorSeverity.ERROR, errorState.severity)
    }

    @Test
    fun `HttpError 401 shows authentication error`() {
        val httpError =
            ApiError.HttpError(
                code = 401,
                errorMessage = "Unauthorized",
                detail = "Invalid API key",
            )

        val errorState = httpError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.AUTHENTICATION, errorState.type)
        assertEquals("APIキーが無効です", errorState.message)
        assertEquals(ErrorAction.REAUTHENTICATE, errorState.action)
        assertFalse(errorState.canRetry)
        assertFalse(errorState.isRetryable)
    }

    @Test
    fun `HttpError 500 shows server error with retry`() {
        val httpError =
            ApiError.HttpError(
                code = 500,
                errorMessage = "Internal Server Error",
                detail = "Something went wrong",
            )

        val errorState = httpError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.SERVER, errorState.type)
        assertEquals("サーバーエラーが発生しました", errorState.message)
        assertEquals(ErrorAction.RETRY, errorState.action)
        assertTrue(errorState.canRetry)
        assertTrue(errorState.isRetryable)
        assertEquals(ErrorSeverity.CRITICAL, errorState.severity)
    }

    @Test
    fun `HttpError 404 shows unknown error with no retry`() {
        val httpError =
            ApiError.HttpError(
                code = 404,
                errorMessage = "Not Found",
            )

        val errorState = httpError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.UNKNOWN, errorState.type)
        assertTrue(errorState.message.startsWith("HTTP 404"))
        assertEquals(ErrorAction.DISMISS, errorState.action)
        assertFalse(errorState.canRetry)
        assertFalse(errorState.isRetryable)
    }

    @Test
    fun `UnknownError maps correctly`() {
        val unknownError =
            ApiError.UnknownError(
                exception = RuntimeException("Unknown issue"),
            )

        val errorState = unknownError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.UNKNOWN, errorState.type)
        assertTrue(errorState.message.contains("Unknown error"))
        assertEquals(ErrorAction.DISMISS, errorState.action)
        assertFalse(errorState.canRetry)
    }

    @Test
    fun `SerializationError maps correctly`() {
        val serializationError =
            ApiError.SerializationError(
                exception = RuntimeException("JSON parse error"),
            )

        val errorState = serializationError.toChatErrorState()

        assertEquals(dev.egograph.shared.features.chat.ErrorType.UNKNOWN, errorState.type)
        assertEquals("データの解析に失敗しました", errorState.message)
        assertEquals(ErrorAction.DISMISS, errorState.action)
        assertFalse(errorState.canRetry)
    }
}
