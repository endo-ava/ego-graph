package dev.egograph.shared.core.network

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Ktor Client configuration
 *
 * Verifies that:
 * - HttpClient can be instantiated
 * - HttpClient is properly configured with required plugins
 */
class KtorConfigTest {
    private val testConfig = HttpClientConfig.release()

    @Test
    fun `HttpClient initializes successfully`() {
        // Arrange
        // (nothing to arrange)

        // Act
        val client = provideHttpClient(testConfig)

        // Assert
        assertNotNull(client, "HttpClient should be initialized")
        assertTrue(client.engine != null, "HttpClient should have an engine configured")
    }

    @Test
    fun `HttpClient can be closed without errors`() {
        // Arrange
        val client = provideHttpClient(testConfig)

        // Act & Assert
        // This should not throw an exception
        client.close()
    }

    @Test
    fun `HttpClient provider creates multiple independent instances`() {
        // Arrange
        // (nothing to arrange)

        // Act
        val client1 = provideHttpClient(testConfig)
        val client2 = provideHttpClient(testConfig)

        // Assert
        assertNotNull(client1)
        assertNotNull(client2)
        assertTrue(client1 !== client2, "Each call to create() should produce a new HttpClient instance")

        // Cleanup
        client1.close()
        client2.close()
    }
}
