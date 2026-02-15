package dev.egograph.shared.features.chat

import dev.egograph.shared.core.data.repository.ThreadRepositoryImpl
import dev.egograph.shared.core.data.repository.internal.RepositoryClient
import dev.egograph.shared.core.domain.model.Thread
import dev.egograph.shared.core.domain.model.ThreadListResponse
import dev.egograph.shared.core.domain.repository.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * ThreadRepositoryImplのテスト
 *
 * Ktor MockEngineを使用してHTTPリクエスト/レスポンスをテストします。
 */
class ThreadRepositoryImplTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val baseUrl = "http://test.example.com"
    private val apiKey = "test-api-key"

    /**
     * テスト用HttpClientを作成する
     */
    private fun createMockHttpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

    /**
     * テスト用RepositoryClientを作成する
     */
    private fun createMockRepositoryClient(engine: MockEngine): RepositoryClient {
        val httpClient = createMockHttpClient(engine)
        return RepositoryClient(httpClient, baseUrl, apiKey)
    }

    // ==================== getThreads() テスト ====================

    @Test
    fun `getThreads - success returns thread list`() =
        runTest {
            // Arrange: スレッド一覧レスポンスのモック設定
            val expectedResponse =
                ThreadListResponse(
                    threads =
                        listOf(
                            Thread(
                                threadId = "thread-1",
                                userId = "user-123",
                                title = "Thread 1",
                                preview = "Preview 1",
                                messageCount = 5,
                                createdAt = "2025-01-01T00:00:00Z",
                                lastMessageAt = "2025-01-01T01:00:00Z",
                            ),
                            Thread(
                                threadId = "thread-2",
                                userId = "user-123",
                                title = "Thread 2",
                                preview = null,
                                messageCount = 3,
                                createdAt = "2025-01-02T00:00:00Z",
                                lastMessageAt = "2025-01-02T00:30:00Z",
                            ),
                        ),
                    total = 2,
                    limit = 20,
                    offset = 0,
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("/v1/threads", it.url.encodedPath)
                    assertEquals("20", it.url.parameters["limit"])
                    assertEquals("0", it.url.parameters["offset"])
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: スレッド一覧を収集
            val results = mutableListOf<ThreadListResponse>()
            repository.getThreads(limit = 20, offset = 0).collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert: 結果の検証
            assertEquals(1, results.size)
            val actual = results[0]
            assertEquals(2, actual.threads.size)
            assertEquals("thread-1", actual.threads[0].threadId)
            assertEquals("Thread 1", actual.threads[0].title)
            assertEquals("thread-2", actual.threads[1].threadId)
            assertEquals(2, actual.total)
            assertEquals(20, actual.limit)
            assertEquals(0, actual.offset)
        }

    @Test
    fun `getThreads - pagination with limit and offset`() =
        runTest {
            // Arrange: ページネーションのモック設定
            val expectedResponse =
                ThreadListResponse(
                    threads =
                        listOf(
                            Thread(
                                threadId = "thread-3",
                                userId = "user-123",
                                title = "Thread 3",
                                preview = null,
                                messageCount = 1,
                                createdAt = "2025-01-03T00:00:00Z",
                                lastMessageAt = "2025-01-03T00:00:00Z",
                            ),
                        ),
                    total = 3,
                    limit = 10,
                    offset = 2,
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("10", it.url.parameters["limit"])
                    assertEquals("2", it.url.parameters["offset"])

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val results = mutableListOf<ThreadListResponse>()
            repository.getThreads(limit = 10, offset = 2).collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert
            assertEquals(1, results.size)
            val actual = results[0]
            assertEquals(1, actual.threads.size)
            assertEquals("thread-3", actual.threads[0].threadId)
            assertEquals(3, actual.total)
            assertEquals(10, actual.limit)
            assertEquals(2, actual.offset)
        }

    @Test
    fun `getThreads - empty list returns empty response`() =
        runTest {
            // Arrange: 空のスレッドリストのモック設定
            val expectedResponse =
                ThreadListResponse(
                    threads = emptyList(),
                    total = 0,
                    limit = 20,
                    offset = 0,
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val results = mutableListOf<ThreadListResponse>()
            repository.getThreads(limit = 20, offset = 0).collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert
            assertEquals(1, results.size)
            val actual = results[0]
            assertTrue(actual.threads.isEmpty())
            assertEquals(0, actual.total)
        }

    @Test
    fun `getThreads - HTTP 500 returns HttpError`() =
        runTest {
            // Arrange: 500 Internal Server Errorのモック設定
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"detail": "Internal server error"}""",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.getThreads(limit = 20, offset = 0).collect { result ->
                if (result.isFailure) {
                    result.exceptionOrNull()?.let { errors.add(assertIs<ApiError>(it)) }
                }
            }

            // Assert
            assertTrue(errors.isNotEmpty())
            val error = assertIs<ApiError.HttpError>(errors.first())
            assertEquals(500, error.code)
        }

    // ==================== getThread() テスト ====================

    @Test
    fun `getThread - success returns thread details`() =
        runTest {
            // Arrange: スレッド詳細レスポンスのモック設定
            val expectedResponse =
                Thread(
                    threadId = "thread-123",
                    userId = "user-456",
                    title = "Test Thread",
                    preview = "This is a test thread",
                    messageCount = 10,
                    createdAt = "2025-01-01T00:00:00Z",
                    lastMessageAt = "2025-01-01T02:00:00Z",
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("/v1/threads/thread-123", it.url.encodedPath)
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: スレッド詳細を収集
            val results = mutableListOf<Thread>()
            repository.getThread("thread-123").collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert: 結果の検証
            assertEquals(1, results.size)
            val actual = results[0]
            assertEquals("thread-123", actual.threadId)
            assertEquals("user-456", actual.userId)
            assertEquals("Test Thread", actual.title)
            assertEquals("This is a test thread", actual.preview)
            assertEquals(10, actual.messageCount)
        }

    @Test
    fun `getThread - HTTP 404 Not Found`() =
        runTest {
            // Arrange: 404 Not Foundのモック設定
            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("/v1/threads/nonexistent", it.url.encodedPath)

                    respond(
                        content = """{"detail": "Thread not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.getThread("nonexistent").collect { result ->
                if (result.isFailure) {
                    result.exceptionOrNull()?.let { errors.add(assertIs<ApiError>(it)) }
                }
            }

            // Assert
            assertTrue(errors.isNotEmpty())
            val error = assertIs<ApiError.HttpError>(errors.first())
            assertEquals(404, error.code)
            // detail にはレスポンスボディ全体が含まれる
            assertTrue(error.detail?.contains("Thread not found") == true)
        }

    @Test
    fun `getThread - cache returns same result on second call`() =
        runTest {
            // Arrange: キャッシュ動作の検証
            val expectedResponse =
                Thread(
                    threadId = "thread-cache",
                    userId = "user-789",
                    title = "Cache Test",
                    preview = null,
                    messageCount = 1,
                    createdAt = "2025-01-01T00:00:00Z",
                    lastMessageAt = "2025-01-01T00:00:00Z",
                )
            val responseBody = json.encodeToString(expectedResponse)

            var requestCount = 0
            val mockEngine =
                MockEngine {
                    requestCount++
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: 同じスレッドを2回取得
            val firstResults = mutableListOf<Thread>()
            repository.getThread("thread-cache").collect { result ->
                if (result.isSuccess) {
                    firstResults.add(result.getOrNull()!!)
                }
            }

            val secondResults = mutableListOf<Thread>()
            repository.getThread("thread-cache").collect { result ->
                if (result.isSuccess) {
                    secondResults.add(result.getOrNull()!!)
                }
            }

            // Assert: 2回目はキャッシュから返されるのでHTTPリクエストは1回のみ
            assertEquals(1, requestCount)
            assertEquals(1, firstResults.size)
            assertEquals(1, secondResults.size)
            assertEquals(firstResults[0].threadId, secondResults[0].threadId)
        }

    // ==================== createThread() テスト ====================

    @Test
    fun `createThread - returns 501 Not Implemented`() =
        runTest {
            // Arrange: createThreadはまだ実装されていないため501を返す

            val mockEngine =
                MockEngine {
                    // このテストではHTTPリクエストが送信されないことを検証
                    throw Exception("HTTP request should not be sent")
                }

            val repository = ThreadRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val result = repository.createThread("New Thread")

            // Assert: 501 Not Implemented エラーを返す
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(501, error.code)
            assertEquals("Not Implemented", error.errorMessage)
            assertEquals("Thread creation is not yet supported", error.detail)
        }
}
