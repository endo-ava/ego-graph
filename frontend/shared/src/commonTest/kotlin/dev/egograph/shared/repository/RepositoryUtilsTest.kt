package dev.egograph.shared.repository

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.delay
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
            val cache = InMemoryCache<String, String>()

            cache.put("test-key", "test-value")

            assertEquals("test-value", cache.get("test-key"))
        }

    @Test
    fun `InMemoryCache - get returns null for non-existent key`() =
        runTest {
            val cache = InMemoryCache<String, String>()

            assertNull(cache.get("non-existent"))
        }

    @Test
    fun `InMemoryCache - remove deletes entry`() =
        runTest {
            val cache = InMemoryCache<String, String>()
            cache.put("key", "value")

            cache.remove("key")

            assertNull(cache.get("key"))
        }

    @Test
    fun `InMemoryCache - clear removes all entries`() =
        runTest {
            val cache = InMemoryCache<String, String>()
            cache.put("key1", "value1")
            cache.put("key2", "value2")

            cache.clear()

            assertNull(cache.get("key1"))
            assertNull(cache.get("key2"))
        }

    @Test
    fun `InMemoryCache - overwriting existing key replaces value`() =
        runTest {
            val cache = InMemoryCache<String, String>()
            cache.put("key", "old-value")

            cache.put("key", "new-value")

            assertEquals("new-value", cache.get("key"))
        }

    @Test
    fun `InMemoryCache - expired entry returns null`() =
        runTest {
            val shortExpiration = 100L
            val cache = InMemoryCache<String, String>(expirationMs = shortExpiration)
            cache.put("key", "value")

            Thread.sleep(shortExpiration + 150)

            assertNull(cache.get("key"))
        }

    @Test
    fun `InMemoryCache - non-expired entry returns value`() =
        runTest {
            val longExpiration = 1000L
            val cache = InMemoryCache<String, String>(expirationMs = longExpiration)
            cache.put("key", "value")

            delay(100)

            assertEquals("value", cache.get("key"))
        }

    @Test
    fun `InMemoryCache - concurrent access does not crash`() =
        runTest {
            val cache = InMemoryCache<String, Int>()

            val jobs =
                List(10) {
                    launch {
                        repeat(100) { i ->
                            cache.put("counter", i)
                        }
                    }
                }
            jobs.forEach { it.join() }

            val result = cache.get("counter")
            assertNotNull(result)
            assertTrue(result >= 0)
            assertTrue(result < 100)
        }

    @Test
    fun `InMemoryCache - concurrent reads return consistent values`() =
        runTest {
            val cache = InMemoryCache<String, String>()
            cache.put("key", "value")

            val results = mutableListOf<String?>()
            val jobs =
                List(10) {
                    launch {
                        results.add(cache.get("key"))
                    }
                }
            jobs.forEach { it.join() }

            assertEquals(10, results.size)
            assertTrue(results.all { it == "value" })
        }

    @Test
    fun `generateContextHash - produces consistent hash for same input`() {
        val hash1 = generateContextHash("http://localhost:8000", "test-key")
        val hash2 = generateContextHash("http://localhost:8000", "test-key")

        assertEquals(hash1, hash2)
    }

    @Test
    fun `generateContextHash - produces different hashes for different inputs`() {
        val hash1 = generateContextHash("http://localhost:8000", "key1")
        val hash2 = generateContextHash("http://localhost:8000", "key2")

        assertTrue(hash1 != hash2)
    }

    @Test
    fun `generateContextHash - produces fixed-length hexadecimal string`() {
        val hash = generateContextHash("http://localhost:8000", "test-key")

        assertEquals(16, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `generateContextHash - collision resistance with similar inputs`() {
        val inputs =
            listOf(
                Pair("http://localhost:8000", "key1"),
                Pair("http://localhost:8000", "key2"),
                Pair("http://localhost:8000", "key3"),
                Pair("http://localhost:8001", "key1"),
                Pair("http://localhost:8002", "key1"),
                Pair("https://localhost:8000", "key1"),
            )

        val hashes = inputs.map { (baseUrl, apiKey) -> generateContextHash(baseUrl, apiKey) }
        val uniqueHashes = hashes.toSet()

        assertEquals(inputs.size, uniqueHashes.size)
    }

    @Test
    fun `configureAuth - adds X-API-Key header when apiKey is non-empty`() {
        val builder = HttpRequestBuilder()

        builder.configureAuth("test-api-key")

        assertEquals("test-api-key", builder.headers["X-API-Key"])
    }

    @Test
    fun `configureAuth - does not add header when apiKey is empty`() {
        val builder = HttpRequestBuilder()

        builder.configureAuth("")

        assertNull(builder.headers["X-API-Key"])
    }

    @Test
    fun `ApiError_HttpError - HTTP error properties are set correctly`() {
        val httpError = ApiError.HttpError(404, "Not Found", "Resource not found")

        assertEquals(404, httpError.code)
        assertEquals("Not Found", httpError.errorMessage)
        assertEquals("Resource not found", httpError.detail)
    }

    @Test
    fun `ApiError_HttpError - error message formatting`() {
        val httpError1 = ApiError.HttpError(500, "Internal Server Error", "Database connection failed")
        val httpError2 = ApiError.HttpError(401, "Unauthorized", null)

        assertEquals("HTTP 500: Internal Server Error - Database connection failed", httpError1.message)
        assertEquals("HTTP 401: Unauthorized", httpError2.message)
    }
}
