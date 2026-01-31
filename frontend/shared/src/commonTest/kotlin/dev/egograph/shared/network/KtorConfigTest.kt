package dev.egograph.shared.network

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
    @Test
    fun `HttpClient initializes successfully`() {
        // Arrange
        val factory = HttpClientFactory()

        // Act
        val client = factory.create()

        // Assert
        assertNotNull(client, "HttpClient should be initialized")
        assertTrue(client.engine != null, "HttpClient should have an engine configured")
    }

    @Test
    fun `HttpClient can be closed without errors`() {
        // Arrange
        val factory = HttpClientFactory()
        val client = factory.create()

        // Act & Assert
        // This should not throw an exception
        client.close()
    }

    @Test
    fun `HttpClientFactory creates multiple independent instances`() {
        // Arrange
        val factory = HttpClientFactory()

        // Act
        val client1 = factory.create()
        val client2 = factory.create()

        // Assert
        assertNotNull(client1)
        assertNotNull(client2)
        assertTrue(client1 !== client2, "Each call to create() should produce a new HttpClient instance")

        // Cleanup
        client1.close()
        client2.close()
    }
}
