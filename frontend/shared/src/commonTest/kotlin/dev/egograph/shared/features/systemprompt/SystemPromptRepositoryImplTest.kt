package dev.egograph.shared.features.systemprompt

import dev.egograph.shared.core.data.repository.SystemPromptRepositoryImpl
import dev.egograph.shared.core.data.repository.internal.RepositoryClient
import dev.egograph.shared.core.domain.model.SystemPromptName
import dev.egograph.shared.core.domain.model.SystemPromptResponse
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
 * SystemPromptRepositoryImplのテスト
 *
 * Ktor MockEngineを使用してHTTPリクエスト/レスポンスをテストします。
 */
class SystemPromptRepositoryImplTest {
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

    // ==================== getSystemPrompt() テスト ====================

    @Test
    fun `getSystemPrompt - success returns system prompt`() =
        runTest {
            // Arrange: SystemPromptレスポンスのモック設定
            val expectedResponse =
                SystemPromptResponse(
                    name = "user",
                    content = "You are a helpful assistant.",
                )
            val responseBody = json.encodeToString(expectedResponse)

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("$baseUrl/v1/system-prompts/user", it.url.toString())
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: テスト対象メソッドの実行
            val result = repository.getSystemPrompt(SystemPromptName.USER)

            // Assert: 結果の検証
            assertTrue(result.isSuccess)
            val actual = result.getOrNull()!!
            assertEquals("user", actual.name)
            assertEquals("You are a helpful assistant.", actual.content)
        }

    @Test
    fun `getSystemPrompt - HTTP 404 Not Found`() =
        runTest {
            // Arrange: 404 Not Foundのモック設定
            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("/v1/system-prompts/identity", it.url.encodedPath)

                    respond(
                        content = """{"detail": "System prompt not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val result = repository.getSystemPrompt(SystemPromptName.IDENTITY)

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(404, error.code)
            // detail にはレスポンスボディ全体が含まれる
            assertTrue(error.detail?.contains("System prompt not found") == true)
        }

    @Test
    fun `getSystemPrompt - HTTP 500 Internal Server Error`() =
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

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val result = repository.getSystemPrompt(SystemPromptName.SOUL)

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(500, error.code)
        }

    @Test
    fun `getSystemPrompt - cache returns same result on second call`() =
        runTest {
            // Arrange: キャッシュ動作の検証
            val expectedResponse =
                SystemPromptResponse(
                    name = "tools",
                    content = "Available tools: calculator, weather.",
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

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: 同じSystemPromptを2回取得
            val firstResult = repository.getSystemPrompt(SystemPromptName.TOOLS)
            val secondResult = repository.getSystemPrompt(SystemPromptName.TOOLS)

            // Assert: 2回目はキャッシュから返されるのでHTTPリクエストは1回のみ
            assertEquals(1, requestCount)
            assertTrue(firstResult.isSuccess)
            assertTrue(secondResult.isSuccess)
            assertEquals(
                firstResult.getOrNull()!!.content,
                secondResult.getOrNull()!!.content,
            )
        }

    // ==================== updateSystemPrompt() テスト ====================

    @Test
    fun `updateSystemPrompt - success returns updated prompt`() =
        runTest {
            // Arrange: PUTリクエストのモック設定
            val expectedResponse =
                SystemPromptResponse(
                    name = "user",
                    content = "Updated content",
                )
            val responseBody = json.encodeToString(expectedResponse)

            var requestBody: String? = null

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Put, it.method)
                    assertEquals("$baseUrl/v1/system-prompts/user", it.url.toString())
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    // リクエストボディをキャプチャして検証
                    val bodyBytes =
                        (it.body as io.ktor.http.content.OutgoingContent.ByteArrayContent)
                            .bytes()
                    requestBody = String(bodyBytes)

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act: テスト対象メソッドの実行
            val result = repository.updateSystemPrompt(SystemPromptName.USER, "Updated content")

            // Assert: 結果の検証
            assertTrue(result.isSuccess)
            val actual = result.getOrNull()!!
            assertEquals("user", actual.name)
            assertEquals("Updated content", actual.content)

            // Assert: リクエストボディに更新内容が含まれることを検証
            assertTrue(requestBody?.contains("Updated content") == true)
        }

    @Test
    fun `updateSystemPrompt - PUT request includes content`() =
        runTest {
            // Arrange: PUTリクエストボディの検証
            val expectedResponse =
                SystemPromptResponse(
                    name = "identity",
                    content = "You are Claude.",
                )
            val responseBody = json.encodeToString(expectedResponse)

            var capturedRequestBody: String? = null

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Put, it.method)
                    assertEquals("$baseUrl/v1/system-prompts/identity", it.url.toString())
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    // リクエストボディをキャプチャして検証
                    val bodyBytes =
                        (it.body as io.ktor.http.content.OutgoingContent.ByteArrayContent)
                            .bytes()
                    capturedRequestBody = String(bodyBytes)

                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val result = repository.updateSystemPrompt(SystemPromptName.IDENTITY, "You are Claude.")

            // Assert: レスポンスの検証
            assertTrue(result.isSuccess)
            assertEquals("identity", result.getOrNull()!!.name)
            assertEquals("You are Claude.", result.getOrNull()!!.content)

            // Assert: リクエストボディにコンテンツが含まれることを検証
            assertTrue(capturedRequestBody?.contains("You are Claude.") == true)
        }

    @Test
    fun `updateSystemPrompt - HTTP 404 Not Found`() =
        runTest {
            // Arrange: 404 Not Foundのモック設定
            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Put, it.method)

                    respond(
                        content = """{"detail": "System prompt not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val result = repository.updateSystemPrompt(SystemPromptName.SOUL, "New content")

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(404, error.code)
            // detail にはレスポンスボディ全体が含まれる
            assertTrue(error.detail?.contains("System prompt not found") == true)
        }

    @Test
    fun `updateSystemPrompt - HTTP 500 Internal Server Error`() =
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

            val repository = SystemPromptRepositoryImpl(createMockRepositoryClient(mockEngine))

            // Act
            val result = repository.updateSystemPrompt(SystemPromptName.TOOLS, "New tools")

            // Assert
            assertTrue(result.isFailure)
            val error = assertIs<ApiError.HttpError>(result.exceptionOrNull())
            assertEquals(500, error.code)
        }
}
