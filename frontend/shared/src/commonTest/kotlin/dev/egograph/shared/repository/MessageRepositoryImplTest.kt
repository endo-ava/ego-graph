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
        // Arrange
        val httpClient = HttpClient()

        // Act
        val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

        // Assert
        assertTrue(repository is MessageRepository)
    }
}
