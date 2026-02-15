package dev.egograph.shared.features.chat

import dev.egograph.shared.core.data.repository.MessageRepositoryImpl
import dev.egograph.shared.core.data.repository.internal.RepositoryClient
import dev.egograph.shared.core.domain.model.MessageRole
import dev.egograph.shared.core.domain.model.ThreadMessage
import dev.egograph.shared.core.domain.model.ThreadMessagesResponse
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
 * MessageRepositoryImplのテスト
 *
 * Ktor MockEngineを使用してHTTPリクエスト/レスポンスをテストします。
 */
class MessageRepositoryImplTest {
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

    // ==================== getMessages() テスト ====================

    @Test
    fun `getMessages - success returns thread messages`() =
        runTest {
            // Arrange: スレッドメッセージ一覧レスポンスのモック設定
            val expectedResponse =
                ThreadMessagesResponse(
                    threadId = "thread-123",
                    messages =
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
                        ),
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("$baseUrl/v1/threads/thread-123/messages", it.url.toString())
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = MessageRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: メッセージ一覧を収集
            val results = mutableListOf<ThreadMessagesResponse>()
            repository.getMessages("thread-123").collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert: 結果の検証
            assertEquals(1, results.size)
            val actual = results[0]
            assertEquals("thread-123", actual.threadId)
            assertEquals(2, actual.messages.size)
            assertEquals("msg-1", actual.messages[0].messageId)
            assertEquals(MessageRole.USER, actual.messages[0].role)
            assertEquals("Hello", actual.messages[0].content)
            assertEquals("msg-2", actual.messages[1].messageId)
            assertEquals(MessageRole.ASSISTANT, actual.messages[1].role)
            assertEquals("Hi there!", actual.messages[1].content)
            assertEquals("gpt-4", actual.messages[1].modelName)
        }

    @Test
    fun `getMessages - HTTP 404 Not Found`() =
        runTest {
            // Arrange: 404 Not Foundのモック設定
            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("$baseUrl/v1/threads/nonexistent/messages", it.url.toString())

                    respond(
                        content = """{"detail": "Thread not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = MessageRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.getMessages("nonexistent").collect { result ->
                if (result.isFailure) {
                    result.exceptionOrNull()?.let {
                        assertIs<ApiError>(it)
                        errors.add(it)
                    }
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
    fun `getMessages - empty message list returns empty response`() =
        runTest {
            // Arrange: 空のメッセージリストのモック設定
            val expectedResponse =
                ThreadMessagesResponse(
                    threadId = "thread-empty",
                    messages = emptyList(),
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

            val repository = MessageRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val results = mutableListOf<ThreadMessagesResponse>()
            repository.getMessages("thread-empty").collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert
            assertEquals(1, results.size)
            val actual = results[0]
            assertEquals("thread-empty", actual.threadId)
            assertTrue(actual.messages.isEmpty())
        }

    @Test
    fun `getMessages - HTTP 500 Internal Server Error`() =
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

            val repository = MessageRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.getMessages("thread-123").collect { result ->
                if (result.isFailure) {
                    result.exceptionOrNull()?.let {
                        assertIs<ApiError>(it)
                        errors.add(it)
                    }
                }
            }

            // Assert
            assertTrue(errors.isNotEmpty())
            val error = assertIs<ApiError.HttpError>(errors.first())
            assertEquals(500, error.code)
        }

    @Test
    fun `getMessages - cache returns same result on second call`() =
        runTest {
            // Arrange: キャッシュ動作の検証
            val expectedResponse =
                ThreadMessagesResponse(
                    threadId = "thread-cache",
                    messages =
                        listOf(
                            ThreadMessage(
                                messageId = "msg-cache",
                                threadId = "thread-cache",
                                userId = "user-789",
                                role = MessageRole.USER,
                                content = "Cached message",
                                createdAt = "2025-01-01T00:00:00Z",
                                modelName = null,
                            ),
                        ),
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

            val repository = MessageRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: 同じスレッドのメッセージを2回取得
            val firstResults = mutableListOf<ThreadMessagesResponse>()
            repository.getMessages("thread-cache").collect { result ->
                if (result.isSuccess) {
                    firstResults.add(result.getOrNull()!!)
                }
            }

            val secondResults = mutableListOf<ThreadMessagesResponse>()
            repository.getMessages("thread-cache").collect { result ->
                if (result.isSuccess) {
                    secondResults.add(result.getOrNull()!!)
                }
            }

            // Assert: 2回目はキャッシュから返されるのでHTTPリクエストは1回のみ
            assertEquals(1, requestCount)
            assertEquals(1, firstResults.size)
            assertEquals(1, secondResults.size)
            assertEquals(firstResults[0].threadId, secondResults[0].threadId)
            assertEquals(1, firstResults[0].messages.size)
        }
}
