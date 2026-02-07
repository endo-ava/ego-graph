package dev.egograph.shared.repository

import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.ThreadMessage
import dev.egograph.shared.dto.ThreadMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageRepositoryImplTest {
    private val testJson =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val testMessages =
        listOf(
            ThreadMessage(
                messageId = "msg-1",
                threadId = "thread-123",
                userId = "user-456",
                role = MessageRole.USER,
                content = "Hello",
                createdAt = "2025-01-01T00:00:00Z",
                modelName = null,
            ),
            ThreadMessage(
                messageId = "msg-2",
                threadId = "thread-123",
                userId = "user-456",
                role = MessageRole.ASSISTANT,
                content = "Hi there!",
                createdAt = "2025-01-01T00:01:00Z",
                modelName = "gpt-4",
            ),
        )

    private val testThreadMessagesResponse =
        ThreadMessagesResponse(
            threadId = "thread-123",
            messages = testMessages,
        )

    @Test
    fun `getMessages - cache miss then hit`() =
        runTest {
            // Given - MockEngine that tracks request count
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    respond(
                        content =
                            testJson.encodeToString(
                                ThreadMessagesResponse.serializer(),
                                testThreadMessagesResponse,
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When - First call (cache miss)
            val result1 = repository.getMessages("thread-123")
            val response1 = result1.getOrThrow()

            // Then - Should make HTTP request
            assertEquals(1, requestCount)
            assertEquals("thread-123", response1.threadId)
            assertEquals(2, response1.messages.size)

            // When - Second call (cache hit)
            val result2 = repository.getMessages("thread-123")
            val response2 = result2.getOrThrow()

            // Then - Should NOT make another HTTP request (cached)
            assertEquals(1, requestCount, "Should not make another request on cache hit")
            assertEquals(2, response2.messages.size)
        }

    @Test
    fun `getMessages - different cache keys for different threadIds`() =
        runTest {
            // Given
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    val threadId =
                        it.url.pathSegments
                            .dropLast(1)
                            .last()
                    val response =
                        ThreadMessagesResponse(
                            threadId = threadId,
                            messages =
                                listOf(
                                    ThreadMessage(
                                        messageId = "msg-$threadId",
                                        threadId = threadId,
                                        userId = "user-456",
                                        role = MessageRole.USER,
                                        content = "Message in $threadId",
                                        createdAt = "2025-01-01T00:00:00Z",
                                    ),
                                ),
                        )

                    respond(
                        content = testJson.encodeToString(ThreadMessagesResponse.serializer(), response),
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When - Call two different threadIds
            repository.getMessages("thread-1")
            repository.getMessages("thread-2")

            // Then - Should make 2 requests
            assertEquals(2, requestCount, "Different threadIds should result in different cache keys")

            // When - Call again with same threadId (should hit cache)
            repository.getMessages("thread-1")

            // Then - Still 2 requests (third call hit cache)
            assertEquals(2, requestCount, "Third call should hit cache")
        }

    @Test
    fun `getMessages - handles HTTP 404 error`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    respondError(
                        status = HttpStatusCode.NotFound,
                        content = """{"error": "Thread not found"}""",
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getMessages("non-existent-thread")

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(404, error?.code)
        }

    @Test
    fun `getMessages - handles HTTP 500 error`() =
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getMessages("thread-123")

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(500, error?.code)
        }

    @Test
    fun `getMessages - handles network error`() =
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getMessages("thread-123")

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.NetworkError
            assertTrue(error != null, "Should return NetworkError")
        }

    @Test
    fun `getMessages - handles invalid JSON response`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"invalid": "json structure"}""",
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getMessages("thread-123")

            // Then
            assertTrue(result.isFailure, "Should fail on invalid JSON")
        }

    @Test
    fun `getMessages - handles HTTP 401 unauthorized error`() =
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "invalid-key")

            // When
            val result = repository.getMessages("thread-123")

            // Then
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(401, error?.code)
        }

    @Test
    fun `getMessages - cache invalidated on error`() =
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
                                    ThreadMessagesResponse.serializer(),
                                    testThreadMessagesResponse,
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When - First successful call
            val result1 = repository.getMessages("thread-123")
            assertTrue(result1.isSuccess)
            assertEquals(1, requestCount)

            // When - Second call fails (cache should be invalidated)
            shouldFail = true
            val result2 = repository.getMessages("thread-123")
            assertTrue(result2.isFailure)
            assertEquals(2, requestCount, "Should make new request even though cache was populated")

            // When - Third call succeeds again
            shouldFail = false
            val result3 = repository.getMessages("thread-123")
            assertTrue(result3.isSuccess)
            assertEquals(3, requestCount, "Should make new request after cache was invalidated by error")
        }

    @Test
    fun `getMessages - handles empty messages list`() =
        runTest {
            // Given
            val mockEngine =
                MockEngine {
                    val response =
                        ThreadMessagesResponse(
                            threadId = "thread-123",
                            messages = emptyList(),
                        )

                    respond(
                        content = testJson.encodeToString(ThreadMessagesResponse.serializer(), response),
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
            val repository = MessageRepositoryImpl(httpClient, "http://localhost:8000", "")

            // When
            val result = repository.getMessages("thread-123")

            // Then
            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertEquals("thread-123", response.threadId)
            assertTrue(response.messages.isEmpty(), "Messages list should be empty")
        }

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

private class IOException(
    message: String,
) : Exception(message)
