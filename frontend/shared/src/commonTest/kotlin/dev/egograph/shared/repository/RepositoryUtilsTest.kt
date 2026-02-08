package dev.egograph.shared.repository

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryUtilsTest {
    @Test
    fun `InMemoryCache - put and get returns stored value`() =
        runTest {
            // Arrange
            val cache = InMemoryCache<String, String>()

            // Act
            cache.put("test-key", "test-value")

            // Assert
            assertEquals("test-value", cache.get("test-key"))
        }

    @Test
    fun `InMemoryCache - get returns null for non-existent key`() =
        runTest {
            // Arrange
            val cache = InMemoryCache<String, String>()

            // Act
            val result = cache.get("non-existent")

            // Assert
            assertNull(result)
        }

    @Test
    fun `InMemoryCache - remove deletes entry`() =
        runTest {
            // Arrange
            val cache = InMemoryCache<String, String>()
            cache.put("key", "value")

            // Act
            cache.remove("key")

            // Assert
            assertNull(cache.get("key"))
        }

    @Test
    fun `InMemoryCache - clear removes all entries`() =
        runTest {
            // Arrange
            val cache = InMemoryCache<String, String>()
            cache.put("key1", "value1")
            cache.put("key2", "value2")

            // Act
            cache.clear()

            // Assert
            assertNull(cache.get("key1"))
            assertNull(cache.get("key2"))
        }

    @Test
    fun `InMemoryCache - overwriting existing key replaces value`() =
        runTest {
            // Arrange
            val cache = InMemoryCache<String, String>()
            cache.put("key", "old-value")

            // Act
            cache.put("key", "new-value")

            // Assert
            assertEquals("new-value", cache.get("key"))
        }

    @Test
    fun `InMemoryCache - expired entry returns null`() =
        runTest {
            // Arrange
            val shortExpiration = 100L
            val cache = InMemoryCache<String, String>(expirationMs = shortExpiration)
            cache.put("key", "value")

            // Act
            Thread.sleep(shortExpiration + 150)

            // Assert
            assertNull(cache.get("key"))
        }

    @Test
    fun `InMemoryCache - non-expired entry returns value`() =
        runTest {
            // Arrange
            val longExpiration = 1000L
            val cache = InMemoryCache<String, String>(expirationMs = longExpiration)
            cache.put("key", "value")

            // Act
            Thread.sleep(100)

            // Assert
            assertEquals("value", cache.get("key"))
        }

    @Test
    fun `InMemoryCache - concurrent access does not crash`() =
        runTest {
            // Arrange
            val cache = InMemoryCache<String, Int>()

            // Act
            val jobs =
                List(10) {
                    launch {
                        repeat(100) { i ->
                            cache.put("counter", i)
                        }
                    }
                }
            jobs.forEach { it.join() }

            // Assert
            val result = cache.get("counter")
            assertNotNull(result)
            assertTrue(result >= 0)
            assertTrue(result < 100)
        }

    @Test
    fun `InMemoryCache - concurrent reads return consistent values`() =
        runTest {
            // Arrange
            val cache = InMemoryCache<String, String>()
            cache.put("key", "value")

            // Act
            val results = mutableListOf<String?>()
            val jobs =
                List(10) {
                    launch {
                        results.add(cache.get("key"))
                    }
                }
            jobs.forEach { it.join() }

            // Assert
            assertEquals(10, results.size)
            assertTrue(results.all { it == "value" })
        }

    @Test
    fun `generateContextHash - produces consistent hash for same input`() {
        // Arrange
        val baseUrl = "http://localhost:8000"
        val apiKey = "test-key"

        // Act
        val hash1 = generateContextHash(baseUrl, apiKey)
        val hash2 = generateContextHash(baseUrl, apiKey)

        // Assert
        assertEquals(hash1, hash2)
    }

    @Test
    fun `generateContextHash - produces different hashes for different inputs`() {
        // Arrange
        val baseUrl1 = "http://localhost:8000"
        val apiKey1 = "key1"
        val apiKey2 = "key2"

        // Act
        val hash1 = generateContextHash(baseUrl1, apiKey1)
        val hash2 = generateContextHash(baseUrl1, apiKey2)

        // Assert
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `generateContextHash - produces fixed-length hexadecimal string`() {
        // Arrange
        val baseUrl = "http://localhost:8000"
        val apiKey = "test-key"

        // Act
        val hash = generateContextHash(baseUrl, apiKey)

        // Assert
        assertEquals(16, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `generateContextHash - collision resistance with similar inputs`() {
        // Arrange
        val inputs =
            listOf(
                Pair("http://localhost:8000", "key1"),
                Pair("http://localhost:8000", "key2"),
                Pair("http://localhost:8000", "key3"),
                Pair("http://localhost:8001", "key1"),
                Pair("http://localhost:8002", "key1"),
                Pair("https://localhost:8000", "key1"),
            )

        // Act
        val hashes = inputs.map { (baseUrl, apiKey) -> generateContextHash(baseUrl, apiKey) }
        val uniqueHashes = hashes.toSet()

        // Assert
        assertEquals(inputs.size, uniqueHashes.size)
    }

    @Test
    fun `configureAuth - adds X-API-Key header when apiKey is non-empty`() {
        // Arrange
        val builder = HttpRequestBuilder()
        val apiKey = "test-api-key"

        // Act
        builder.configureAuth(apiKey)

        // Assert
        assertEquals(apiKey, builder.headers["X-API-Key"])
    }

    @Test
    fun `configureAuth - does not add header when apiKey is empty`() {
        // Arrange
        val builder = HttpRequestBuilder()
        val apiKey = ""

        // Act
        builder.configureAuth(apiKey)

        // Assert
        assertNull(builder.headers["X-API-Key"])
    }

    @Test
    fun `ApiError_HttpError - HTTP error properties are set correctly`() {
        // Arrange
        val code = 404
        val errorMessage = "Not Found"
        val detail = "Resource not found"

        // Act
        val httpError = ApiError.HttpError(code, errorMessage, detail)

        // Assert
        assertEquals(code, httpError.code)
        assertEquals(errorMessage, httpError.errorMessage)
        assertEquals(detail, httpError.detail)
    }

    @Test
    fun `ApiError_HttpError - error message formatting`() {
        // Arrange
        val httpError1 = ApiError.HttpError(500, "Internal Server Error", "Database connection failed")
        val httpError2 = ApiError.HttpError(401, "Unauthorized", null)

        // Act
        val message1 = httpError1.message
        val message2 = httpError2.message

        // Assert
        assertEquals("HTTP 500: Internal Server Error - Database connection failed", message1)
        assertEquals("HTTP 401: Unauthorized", message2)
    }
}
