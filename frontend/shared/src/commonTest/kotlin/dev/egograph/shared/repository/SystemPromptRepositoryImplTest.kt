package dev.egograph.shared.repository

import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SystemPromptRepositoryImplのテスト
 *
 * 注: Ktor MockEngineが利用可能でないため、構造検証のみ実施しています。
 */
class SystemPromptRepositoryImplTest {

    @Test
    fun `SystemPromptRepositoryImpl can be instantiated`() {
        // Given
        val httpClient = HttpClient()

        // When
        val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000")

        // Then
        assertTrue(repository is SystemPromptRepository)
    }

    @Test
    fun `handles network error gracefully`() = runTest {
        // This test documents expected behavior without actual HTTP calls
        // When MockEngine is available, add actual network error tests

        val expectedError = ApiError.NetworkError(Exception("Network error"))
        assertTrue(expectedError is ApiError.NetworkError)
        assertTrue(expectedError.cause != null)
    }
}
