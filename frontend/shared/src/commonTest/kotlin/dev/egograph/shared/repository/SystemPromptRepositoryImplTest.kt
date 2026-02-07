package dev.egograph.shared.repository

import dev.egograph.shared.dto.SystemPromptName
import dev.egograph.shared.dto.SystemPromptResponse
import dev.egograph.shared.network.provideHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemPromptRepositoryImplTest {
    private val testJson =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val testSystemPromptResponse =
        SystemPromptResponse(
            name = "user",
            content = "You are a helpful assistant.",
        )

    @Test
    fun `getSystemPrompt - cache miss then hit`() =
        runTest {
            // Given - MockEngine that tracks request count
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    respond(
                        content =
                            testJson.encodeToString(
                                SystemPromptResponse.serializer(),
                                testSystemPromptResponse,
                            ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When - First call (cache miss)
            val result1 = repository.getSystemPrompt(SystemPromptName.USER)
            val response1 = result1.getOrThrow()

            // Then - Should make HTTP request
            assertEquals(1, requestCount)
            assertEquals("user", response1.name)

            // When - Second call (cache hit)
            val result2 = repository.getSystemPrompt(SystemPromptName.USER)
            val response2 = result2.getOrThrow()

            // Then - Should NOT make another HTTP request (cached)
            assertEquals(1, requestCount, "Should not make another request on cache hit")
            assertEquals("user", response2.name)
        }

    @Test
    fun `getSystemPrompt - different cache keys for different prompt names`() =
        runTest {
            // Given
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    val promptName = it.url.pathSegments.last()
                    val response =
                        SystemPromptResponse(
                            name = promptName,
                            content = "Content for $promptName",
                        )

                    respond(
                        content = testJson.encodeToString(SystemPromptResponse.serializer(), response),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When - Call two different prompt names
            repository.getSystemPrompt(SystemPromptName.USER)
            repository.getSystemPrompt(SystemPromptName.IDENTITY)

            // Then - Should make 2 requests
            assertEquals(2, requestCount, "Different prompt names should result in different cache keys")

            // When - Call again with same prompt name (should hit cache)
            repository.getSystemPrompt(SystemPromptName.USER)

            // Then - Still 2 requests (third call hit cache)
            assertEquals(2, requestCount, "Third call should hit cache")
        }

    @Test
    fun `updateSystemPrompt - invalidates cache on successful update`() =
        runTest {
            // Given
            var requestCount = 0
            var promptContent = "Original content"
            val mockEngine =
                MockEngine {
                    requestCount++
                    when (it.method) {
                        HttpMethod.Get -> {
                            respond(
                                content =
                                    testJson.encodeToString(
                                        SystemPromptResponse.serializer(),
                                        SystemPromptResponse(name = "user", content = promptContent),
                                    ),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        HttpMethod.Put -> {
                            promptContent = "Updated content"
                            respond(
                                content =
                                    testJson.encodeToString(
                                        SystemPromptResponse.serializer(),
                                        SystemPromptResponse(name = "user", content = promptContent),
                                    ),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            respondError(HttpStatusCode.MethodNotAllowed, "Method not allowed")
                        }
                    }
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When - First get (cache miss)
            val result1 = repository.getSystemPrompt(SystemPromptName.USER)
            assertEquals("Original content", result1.getOrThrow().content)
            assertEquals(1, requestCount)

            // When - Update system prompt (should invalidate cache)
            val updateResult = repository.updateSystemPrompt(SystemPromptName.USER, "Updated content")
            assertTrue(updateResult.isSuccess)
            assertEquals(2, requestCount)

            // When - Second get (cache miss after invalidation)
            val result2 = repository.getSystemPrompt(SystemPromptName.USER)
            assertEquals("Updated content", result2.getOrThrow().content)
            assertEquals(3, requestCount, "Should make new request after cache was invalidated")
        }

    @Test
    fun `updateSystemPrompt - handles HTTP 404 error`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    respondError(
                        status = HttpStatusCode.NotFound,
                        content = """{"error": "System prompt not found"}""",
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.updateSystemPrompt(SystemPromptName.USER, "New content")

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(404, error?.code)
        }

    @Test
    fun `updateSystemPrompt - handles HTTP 401 unauthorized error`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    respondError(
                        status = HttpStatusCode.Unauthorized,
                        content = """{"error": "Invalid API key"}""",
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "invalid-key")

            // When
            val result = repository.updateSystemPrompt(SystemPromptName.USER, "New content")

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(401, error?.code)
        }

    @Test
    fun `updateSystemPrompt - handles network error`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    throw IOException("Network connection failed")
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.updateSystemPrompt(SystemPromptName.USER, "New content")

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.NetworkError
            assertTrue(error != null, "Should return NetworkError")
        }

    @Test
    fun `getSystemPrompt - handles HTTP 404 error`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    respondError(
                        status = HttpStatusCode.NotFound,
                        content = """{"error": "System prompt not found"}""",
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getSystemPrompt(SystemPromptName.USER)

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(404, error?.code)
        }

    @Test
    fun `getSystemPrompt - handles HTTP 500 error`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    respondError(
                        status = HttpStatusCode.InternalServerError,
                        content = """{"error": "Internal server error"}""",
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getSystemPrompt(SystemPromptName.USER)

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(500, error?.code)
        }

    @Test
    fun `getSystemPrompt - handles network error`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    throw IOException("Network connection failed")
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getSystemPrompt(SystemPromptName.USER)

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.NetworkError
            assertTrue(error != null, "Should return NetworkError")
        }

    @Test
    fun `getSystemPrompt - cache invalidated on error`() =
        runTest {
            // Given
            var requestCount = 0
            var shouldFail = false
            val mockEngine =
                MockEngine {
                    requestCount++
                    if (shouldFail) {
                        respondError(
                            status = HttpStatusCode.InternalServerError,
                            content = """{"error": "Server error"}""",
                        )
                    } else {
                        respond(
                            content =
                                testJson.encodeToString(
                                    SystemPromptResponse.serializer(),
                                    testSystemPromptResponse,
                                ),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When - First successful call
            val result1 = repository.getSystemPrompt(SystemPromptName.USER)
            assertTrue(result1.isSuccess)
            assertEquals(1, requestCount)

            // When - Second call fails (cache should be invalidated)
            shouldFail = true
            val result2 = repository.getSystemPrompt(SystemPromptName.USER)
            assertTrue(result2.isFailure)
            assertEquals(2, requestCount, "Should make new request even though cache was populated")

            // When - Third call succeeds again
            shouldFail = false
            val result3 = repository.getSystemPrompt(SystemPromptName.USER)
            assertTrue(result3.isSuccess)
            assertEquals(3, requestCount, "Should make new request after cache was invalidated by error")
        }

    @Test
    fun `updateSystemPrompt - does not invalidate cache on failed update`() =
        runTest {
            // Given
            var getCallCount = 0
            val mockEngine =
                MockEngine {
                    when (it.method) {
                        HttpMethod.Get -> {
                            getCallCount++
                            respond(
                                content =
                                    testJson.encodeToString(
                                        SystemPromptResponse.serializer(),
                                        testSystemPromptResponse,
                                    ),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        HttpMethod.Put -> {
                            respondError(
                                status = HttpStatusCode.Unauthorized,
                                content = """{"error": "Invalid API key"}""",
                            )
                        }
                        else -> {
                            respondError(HttpStatusCode.MethodNotAllowed, "Method not allowed")
                        }
                    }
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "invalid-key")

            // When - First get (cache miss)
            val result1 = repository.getSystemPrompt(SystemPromptName.USER)
            assertTrue(result1.isSuccess)
            assertEquals(1, getCallCount)

            // When - Failed update attempt (should NOT invalidate cache since update failed)
            val updateResult = repository.updateSystemPrompt(SystemPromptName.USER, "New content")
            assertTrue(updateResult.isFailure)

            // When - Second get (should still hit cache since update failed)
            val result2 = repository.getSystemPrompt(SystemPromptName.USER)
            assertTrue(result2.isSuccess)
            assertEquals(1, getCallCount, "Should not make new request since cache should still be valid after failed update")
        }

    @Test
    fun `SystemPromptRepositoryImpl can be instantiated`() {
        // Given
        val httpClient = provideHttpClient()

        try {
            // When
            val repository = SystemPromptRepositoryImpl(httpClient, "http://localhost:8000", "")

            // Then
            assertTrue(repository is SystemPromptRepository)
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun `network error type can be created`() =
        runTest {
            // This test documents expected behavior without actual HTTP calls
            // When MockEngine is available, add actual network error tests

            val expectedError = ApiError.NetworkError(Exception("Network error"))
            assertTrue(expectedError is ApiError.NetworkError)
            assertTrue(expectedError.cause != null)
        }
}

private class IOException(
    message: String,
) : Exception(message)
