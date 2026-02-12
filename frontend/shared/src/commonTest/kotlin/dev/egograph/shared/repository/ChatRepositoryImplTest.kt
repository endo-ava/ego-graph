package dev.egograph.shared.repository

import dev.egograph.shared.core.data.repository.ChatRepositoryImpl
import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.ChatResponse
import dev.egograph.shared.core.domain.model.LLMModel
import dev.egograph.shared.core.domain.model.Message
import dev.egograph.shared.core.domain.model.MessageRole
import dev.egograph.shared.core.domain.model.ModelsResponse
import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.core.domain.model.StreamChunkType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeText
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * ChatRepositoryImplのテスト
 *
 * Ktor MockEngineを使用してHTTPリクエスト/レスポンスをテストします。
 */
class ChatRepositoryImplTest {
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

    // ==================== sendMessageSync() テスト ====================

    @Test
    fun `sendMessageSync - success returns ChatResponse`() =
        runTest {
            // Arrange: モックHTTPレスポンスの設定
            val expectedResponse =
                ChatResponse(
                    id = "resp-123",
                    message =
                        Message(
                            role = MessageRole.ASSISTANT,
                            content = "Hello!",
                        ),
                    threadId = "thread-456",
                    modelName = "gpt-4",
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Post, it.method)
                    assertEquals("$baseUrl/v1/chat", it.url.toString())
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = listOf(Message(role = MessageRole.USER, content = "Hi")),
                    stream = false,
                )

            // Act: テスト対象メソッドの実行
            val result = repository.sendMessageSync(request)

            // Assert: 結果の検証
            assertTrue(result.isSuccess)
            val actual = result.getOrNull()!!
            assertEquals("resp-123", actual.id)
            assertEquals("Hello!", actual.message.content)
            assertEquals("thread-456", actual.threadId)
            assertEquals("gpt-4", actual.modelName)
        }

    @Test
    fun `sendMessageSync - HTTP 400 returns HttpError`() =
        runTest {
            // Arrange: 400 Bad Errorのモック設定
            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Post, it.method)
                    respond(
                        content = """{"detail": "Invalid request"}""",
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = emptyList(),
                    stream = false,
                )

            // Act
            val result = repository.sendMessageSync(request)

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(400, error.code)
            assertEquals("Bad Request", error.errorMessage)
            assertEquals("""{"detail": "Invalid request"}""", error.detail)
        }

    @Test
    fun `sendMessageSync - HTTP 401 Unauthorized`() =
        runTest {
            // Arrange: 401 Unauthorizedのモック設定
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"detail": "Invalid API key"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = emptyList(),
                    stream = false,
                )

            // Act
            val result = repository.sendMessageSync(request)

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(401, error.code)
            assertEquals("Unauthorized", error.errorMessage)
        }

    @Test
    fun `sendMessageSync - HTTP 500 Internal Server Error`() =
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

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = emptyList(),
                    stream = false,
                )

            // Act
            val result = repository.sendMessageSync(request)

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(500, error.code)
        }

    @Test
    fun `sendMessageSync - network error returns NetworkError`() =
        runTest {
            // Arrange: ネットワークエラーのモック設定
            val mockEngine =
                MockEngine {
                    throw Exception("Connection timeout")
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = emptyList(),
                    stream = false,
                )

            // Act
            val result = repository.sendMessageSync(request)

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.NetworkError>(result.exceptionOrNull())
            assertTrue(error.cause is Exception)
        }

    // ==================== streamChatResponse() テスト ====================

    @Test
    fun `streamChatResponse - SSE streaming returns chunks`() =
        runTest {
            // Arrange: SSEストリーミングレスポンスの設定
            val sseData =
                """
                data: {"type":"delta","delta":"Hello"}

                data: {"type":"delta","delta":" World"}

                data: {"type":"done","finish_reason":"stop"}

                """.trimIndent()

            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Post, it.method)
                    assertEquals("$baseUrl/v1/chat", it.url.toString())

                    val responseContent =
                        buildPacket {
                            writeText(sseData)
                        }

                    respond(
                        content = ByteReadChannel(responseContent),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "text/event-stream"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = listOf(Message(role = MessageRole.USER, content = "Hi")),
                    stream = true,
                )

            // Act: ストリームからチャンクを収集
            val chunks = mutableListOf<StreamChunk>()
            repository.streamChatResponse(request).collect { result ->
                if (result.isSuccess) {
                    chunks.add(result.getOrNull()!!)
                }
            }

            // Assert: チャンクの検証
            assertTrue(chunks.size >= 2)
            assertEquals(StreamChunkType.DELTA, chunks[0].type)
            assertEquals("Hello", chunks[0].delta)
            assertEquals(StreamChunkType.DELTA, chunks[1].type)
            assertEquals(" World", chunks[1].delta)
        }

    @Test
    fun `streamChatResponse - error chunk returns HttpError`() =
        runTest {
            // Arrange: エラーチャンクを含むSSEレスポンス
            val sseData =
                """
                data: {"type":"error","error":"Tool execution failed"}

                """.trimIndent()

            val mockEngine =
                MockEngine {
                    val responseContent =
                        buildPacket {
                            writeText(sseData)
                        }

                    respond(
                        content = ByteReadChannel(responseContent),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "text/event-stream"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = emptyList(),
                    stream = true,
                )

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.streamChatResponse(request).collect { result ->
                if (result.isFailure) {
                    result.exceptionOrNull()?.let {
                        assertIs<ApiError>(it)
                        errors.add(it)
                    }
                }
            }

            // Assert: エラーの検証
            assertTrue(errors.isNotEmpty())
            val error = assertIs<ApiError.HttpError>(errors.first())
            assertEquals(500, error.code)
            assertEquals("Stream error", error.errorMessage)
            assertEquals("Tool execution failed", error.detail)
        }

    @Test
    fun `streamChatResponse - includes API key header`() =
        runTest {
            // Arrange: APIキーヘッダーを検証するモック
            val sseData = "data: {\"type\":\"done\"}\n"
            var receivedApiKey: String? = null

            val mockEngine =
                MockEngine {
                    receivedApiKey = it.headers["X-API-Key"]

                    val responseContent =
                        buildPacket {
                            writeText(sseData)
                        }

                    respond(
                        content = ByteReadChannel(responseContent),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "text/event-stream"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            val request =
                ChatRequest(
                    messages = emptyList(),
                    stream = true,
                )

            // Act
            repository.streamChatResponse(request).collect {}

            // Assert: APIキーヘッダーが送信されたことを検証
            assertEquals(apiKey, receivedApiKey)
        }

    // ==================== getModels() テスト ====================

    @Test
    fun `getModels - success returns ModelsResponse`() =
        runTest {
            // Arrange: モデル一覧レスポンスのモック設定
            val expectedResponse =
                ModelsResponse(
                    models =
                        listOf(
                            LLMModel(
                                id = "gpt-4",
                                name = "GPT-4",
                                provider = "openai",
                                inputCostPer1m = 30.0,
                                outputCostPer1m = 60.0,
                                isFree = false,
                            ),
                            LLMModel(
                                id = "claude-3",
                                name = "Claude 3",
                                provider = "anthropic",
                                inputCostPer1m = 15.0,
                                outputCostPer1m = 75.0,
                                isFree = false,
                            ),
                        ),
                    defaultModel = "gpt-4",
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("$baseUrl/v1/chat/models", it.url.toString())
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            // Act
            val result = repository.getModels()

            // Assert
            assertTrue(result.isSuccess)
            val actual = result.getOrNull()!!
            assertEquals(2, actual.models.size)
            assertEquals("gpt-4", actual.models[0].id)
            assertEquals("GPT-4", actual.models[0].name)
            assertEquals("claude-3", actual.models[1].id)
            assertEquals("gpt-4", actual.defaultModel)
        }

    @Test
    fun `getModels - HTTP 404 Not Found`() =
        runTest {
            // Arrange: 404 Not Foundのモック設定
            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Get, it.method)
                    respond(
                        content = """{"detail": "Models endpoint not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            // Act
            val result = repository.getModels()

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(404, error.code)
            // detail にはレスポンスボディ全体が含まれる
            assertTrue(error.detail?.contains("Models endpoint not found") == true)
        }

    @Test
    fun `getModels - network error returns NetworkError`() =
        runTest {
            // Arrange: ネットワークエラーのモック
            val mockEngine =
                MockEngine {
                    throw Exception("Network unreachable")
                }

            val httpClient = createMockHttpClient(mockEngine)
            val repository = ChatRepositoryImpl(httpClient, baseUrl, apiKey, json)

            // Act
            val result = repository.getModels()

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.NetworkError>(result.exceptionOrNull())
            assertTrue(error.cause is Exception)
        }
}
