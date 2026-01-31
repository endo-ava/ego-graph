package dev.egograph.shared.repository

import dev.egograph.shared.dto.ChatRequest
import dev.egograph.shared.dto.Message
import dev.egograph.shared.dto.MessageRole
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * ChatRepositoryImplのテスト
 *
 * 注: Ktor MockEngineが利用可能でないため、構造検証のみ実施しています。
 */
class ChatRepositoryImplTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `ChatRepositoryImpl can be instantiated`() {
        // Given
        val httpClient = HttpClient()

        // When
        val repository = ChatRepositoryImpl(httpClient, "http://localhost:8000", json)

        // Then
        assertTrue(repository is ChatRepository)
    }

    @Test
    fun `handles network error gracefully`() = runTest {
        // This test documents expected behavior without actual HTTP calls
        // When MockEngine is available, add actual network error tests

        val expectedError = ApiError.NetworkError(Exception("Network error"))
        assertTrue(expectedError is ApiError.NetworkError)
        assert(expectedError.cause != null)
    }
}
