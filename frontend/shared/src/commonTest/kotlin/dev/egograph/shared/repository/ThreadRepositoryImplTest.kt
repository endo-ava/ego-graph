package dev.egograph.shared.repository

import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThreadRepositoryImplTest {
    @Test
    fun `ThreadRepositoryImpl can be instantiated`() {
        // Arrange
        val httpClient = HttpClient()

        // Act
        val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

        // Assert
        assertTrue(repository is ThreadRepository)
    }

    @Test
    fun `createThread returns not implemented error`() =
        runTest {
            // Arrange
            val httpClient = HttpClient()
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            // Act
            val result = repository.createThread("New Thread")

            // Assert
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertEquals(501, error?.code)
            assertEquals("Not Implemented", error?.errorMessage)
        }
}
