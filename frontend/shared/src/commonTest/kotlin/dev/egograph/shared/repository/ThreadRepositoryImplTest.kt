package dev.egograph.shared.repository

import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThreadRepositoryImplTest {
    private val testJson =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val testThread =
        Thread(
            threadId = "thread-123",
            userId = "user-456",
            title = "Test Thread",
            preview = "This is a test thread",
            messageCount = 5,
            createdAt = "2025-01-01T00:00:00Z",
            lastMessageAt = "2025-01-01T01:00:00Z",
        )

    private val testThreadListResponse =
        ThreadListResponse(
            threads = listOf(testThread),
            total = 1,
            limit = 10,
            offset = 0,
        )

    @Test
    fun `getThreads - cache miss then hit`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    respond(
                        content =
                            testJson.encodeToString(
                                ThreadListResponse.serializer(),
                                testThreadListResponse,
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result1 = repository.getThreads(limit = 10, offset = 0).first()
            val response1 = result1.getOrThrow()

            assertEquals(1, requestCount)
            assertEquals(1, response1.total)
            assertEquals(testThread.threadId, response1.threads[0].threadId)

            val result2 = repository.getThreads(limit = 10, offset = 0).first()
            val response2 = result2.getOrThrow()

            assertEquals(1, requestCount, "Should not make another request on cache hit")
            assertEquals(1, response2.total)
        }

    @Test
    fun `getThreads - different cache keys for different parameters`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    val limit = it.url.parameters["limit"]?.toIntOrNull() ?: 10
                    val offset = it.url.parameters["offset"]?.toIntOrNull() ?: 0

                    val response =
                        ThreadListResponse(
                            threads = listOf(testThread),
                            total = 100,
                            limit = limit,
                            offset = offset,
                        )

                    respond(
                        content = testJson.encodeToString(ThreadListResponse.serializer(), response),
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            repository.getThreads(limit = 10, offset = 0).first()
            repository.getThreads(limit = 20, offset = 0).first()

            assertEquals(2, requestCount, "Different parameters should result in different cache keys")

            repository.getThreads(limit = 10, offset = 0).first()

            assertEquals(2, requestCount, "Third call should hit cache")
        }

    @Test
    fun `getThread - cache miss then hit`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    respond(
                        content = testJson.encodeToString(Thread.serializer(), testThread),
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result1 = repository.getThread("thread-123").first()
            val thread1 = result1.getOrThrow()

            assertEquals(1, requestCount)
            assertEquals(testThread.threadId, thread1.threadId)

            val result2 = repository.getThread("thread-123").first()
            val thread2 = result2.getOrThrow()

            assertEquals(1, requestCount, "Should not make another request on cache hit")
            assertEquals(testThread.threadId, thread2.threadId)
        }

    @Test
    fun `getThread - different cache keys for different threadIds`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    val threadId = it.url.pathSegments.last()
                    val thread =
                        testThread.copy(
                            threadId = threadId,
                            title = "Thread $threadId",
                        )

                    respond(
                        content = testJson.encodeToString(Thread.serializer(), thread),
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            repository.getThread("thread-1").first()
            repository.getThread("thread-2").first()

            assertEquals(2, requestCount, "Different threadIds should result in different cache keys")

            repository.getThread("thread-1").first()

            assertEquals(2, requestCount, "Third call should hit cache")
        }

    @Test
    fun `getThreads - handles HTTP 404 error`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respondError(
                        status = HttpStatusCode.NotFound,
                        content = """{"error": "Threads not found"}""",
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(testJson)
                    }
                }
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result = repository.getThreads(limit = 10, offset = 0).first()

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(404, error?.code)
        }

    @Test
    fun `getThreads - handles HTTP 500 error`() =
        runTest {
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result = repository.getThreads(limit = 10, offset = 0).first()

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(500, error?.code)
        }

    @Test
    fun `getThreads - handles network error`() =
        runTest {
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result = repository.getThreads(limit = 10, offset = 0).first()

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.NetworkError
            assertTrue(error != null, "Should return NetworkError")
        }

    @Test
    fun `getThread - handles HTTP 404 error`() =
        runTest {
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result = repository.getThread("non-existent-thread").first()

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error != null, "Should return HttpError")
            assertEquals(404, error?.code)
        }

    @Test
    fun `getThread - handles invalid JSON response`() =
        runTest {
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result = repository.getThread("thread-123").first()

            assertTrue(result.isFailure, "Should fail on invalid JSON")
        }

    @Test
    fun `getThreads - cache invalidated on error`() =
        runTest {
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
                                    ThreadListResponse.serializer(),
                                    testThreadListResponse,
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
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result1 = repository.getThreads(limit = 10, offset = 0).first()
            assertTrue(result1.isSuccess)
            assertEquals(1, requestCount)

            shouldFail = true
            val result2 = repository.getThreads(limit = 10, offset = 0).first()
            assertTrue(result2.isFailure)
            assertEquals(2, requestCount, "Should make new request even though cache was populated")

            shouldFail = false
            val result3 = repository.getThreads(limit = 10, offset = 0).first()
            assertTrue(result3.isSuccess)
            assertEquals(3, requestCount, "Should make new request after cache was invalidated by error")
        }

    @Test
    fun `ThreadRepositoryImpl can be instantiated`() {
        val httpClient = HttpClient()
        val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")
        assertTrue(repository is ThreadRepository)
    }

    @Test
    fun `createThread returns not implemented error`() =
        runTest {
            val httpClient = HttpClient()
            val repository = ThreadRepositoryImpl(httpClient, "http://localhost:8000", "")

            val result = repository.createThread("New Thread")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as? ApiError.HttpError
            assertTrue(error?.code == 501)
            assertTrue(error?.errorMessage == "Not Implemented")
        }
}

private class IOException(
    message: String,
) : Exception(message)
