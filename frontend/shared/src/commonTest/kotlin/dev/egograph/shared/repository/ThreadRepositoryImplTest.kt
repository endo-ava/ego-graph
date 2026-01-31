package dev.egograph.shared.repository

import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * ThreadRepositoryImplのテスト
 *
 * 注: Ktor MockEngineが利用可能でないため、構造検証のみ実施しています。
 * HTTP通信のテストは統合テスト層で実施することを推奨します。
 */
class ThreadRepositoryImplTest {

    @Test
    fun `ThreadRepositoryImpl can be instantiated`() {
        // Given
        val httpClient = HttpClient()

        // When
        val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000")

        // Then
        assertTrue(repository is ThreadRepository)
    }

    @Test
    fun `createThread returns not implemented error`() = runTest {
        // Given
        val httpClient = HttpClient()
        val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000")

        // When
        val result = repository.createThread("New Thread")

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as? ApiError.HttpError
        assert(error?.code == 501)
        assert(error?.errorMessage == "Not Implemented")
    }
}
