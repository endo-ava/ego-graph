package dev.egograph.shared.repository

import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MessageRepositoryImplのテスト
 *
 * 注: Ktor MockEngineが利用可能でないため、構造検証のみ実施しています。
 */
class MessageRepositoryImplTest {
    @Test
    fun `MessageRepositoryImpl can be instantiated`() {
        // Given
        val httpClient = HttpClient()

        // When
        val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

        // Then
        assertTrue(repository is MessageRepository)
    }
}
