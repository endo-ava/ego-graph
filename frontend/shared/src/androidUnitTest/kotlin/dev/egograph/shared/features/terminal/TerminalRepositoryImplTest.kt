package dev.egograph.shared.features.terminal

import dev.egograph.shared.core.data.repository.TerminalRepositoryImpl
import dev.egograph.shared.core.domain.model.terminal.Session
import dev.egograph.shared.core.domain.model.terminal.SessionStatus
import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.platform.PlatformPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * TerminalRepositoryImplのテスト
 *
 * Ktor MockEngineを使用してHTTPリクエスト/レスポンスをテストします。
 */
class TerminalRepositoryImplTest {
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
     * テスト用PlatformPreferencesのMockを作成する
     */
    private fun createMockPreferences(
        gatewayUrl: String = baseUrl,
        gatewayApiKey: String = apiKey,
    ): PlatformPreferences {
        val mockPrefs = mockk<PlatformPreferences>()
        every { mockPrefs.getString("gateway_api_url", any()) } returns gatewayUrl
        every { mockPrefs.getString("gateway_api_key", any()) } returns gatewayApiKey
        return mockPrefs
    }

    // ==================== getSessions() テスト ====================

    @Test
    fun `getSessions - success returns session list`() =
        runTest {
            // Arrange: セッション一覧レスポンスのモック設定
            val expectedResponse =
                """
                {
                    "sessions": [
                        {
                            "session_id": "session-1",
                            "name": "Test Session 1",
                            "status": "connected",
                            "last_activity": "2025-01-01T00:00:00Z",
                            "created_at": "2025-01-01T00:00:00Z"
                        },
                        {
                            "session_id": "session-2",
                            "name": "Test Session 2",
                            "status": "disconnected",
                            "last_activity": "2025-01-01T01:00:00Z",
                            "created_at": "2025-01-01T00:00:00Z"
                        }
                    ],
                    "count": 2
                }
                """.trimIndent()

            val mockEngine =
                MockEngine {
                    // HTTPリクエストのアサーション
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("$baseUrl/api/v1/terminal/sessions", it.url.toString())
                    assertEquals(apiKey, it.headers["X-API-Key"])

                    respond(
                        content = expectedResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val preferences = createMockPreferences()
            val repository = TerminalRepositoryImpl(httpClient, preferences)

            // Act: セッション一覧を収集
            val results = mutableListOf<List<Session>>()
            repository.getSessions().collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert: 結果の検証
            assertEquals(1, results.size)
            val actual = results[0]
            assertEquals(2, actual.size)
            assertEquals("session-1", actual[0].sessionId)
            assertEquals("Test Session 1", actual[0].name)
            assertEquals(SessionStatus.CONNECTED, actual[0].status)
            assertEquals("session-2", actual[1].sessionId)
            assertEquals("Test Session 2", actual[1].name)
            assertEquals(SessionStatus.DISCONNECTED, actual[1].status)
        }

    @Test
    fun `getSessions - HTTP 404 Not Found`() =
        runTest {
            // Arrange: 404 Not Foundのモック設定
            val mockEngine =
                MockEngine {
                    assertEquals(HttpMethod.Get, it.method)
                    assertEquals("$baseUrl/api/v1/terminal/sessions", it.url.toString())

                    respond(
                        content = """{"detail": "Sessions not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val preferences = createMockPreferences()
            val repository = TerminalRepositoryImpl(httpClient, preferences)

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.getSessions().collect { result ->
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
        }

    @Test
    fun `getSessions - HTTP 500 Internal Server Error`() =
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
            val preferences = createMockPreferences()
            val repository = TerminalRepositoryImpl(httpClient, preferences)

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.getSessions().collect { result ->
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
    fun `getSessions - empty session list`() =
        runTest {
            // Arrange: 空のセッションリストのモック設定
            val expectedResponse =
                """
                {
                    "sessions": [],
                    "count": 0
                }
                """.trimIndent()

            val mockEngine =
                MockEngine {
                    respond(
                        content = expectedResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val httpClient = createMockHttpClient(mockEngine)
            val preferences = createMockPreferences()
            val repository = TerminalRepositoryImpl(httpClient, preferences)

            // Act
            val results = mutableListOf<List<Session>>()
            repository.getSessions().collect { result ->
                if (result.isSuccess) {
                    results.add(result.getOrNull()!!)
                }
            }

            // Assert
            assertEquals(1, results.size)
            val actual = results[0]
            assertTrue(actual.isEmpty())
        }

    @Test
    fun `resolveGatewayConfig - invalid baseUrl throws ValidationError`() =
        runTest {
            // Arrange: 不正なURL（http://またはhttps://で始まらない）でpreferencesを作成
            val httpClient =
                createMockHttpClient(
                    MockEngine {
                        throw IllegalStateException("Should not be called")
                    },
                )
            val preferences = createMockPreferences(gatewayUrl = "invalid-url")
            val repository = TerminalRepositoryImpl(httpClient, preferences)

            // Act: エラー結果を収集
            val errors = mutableListOf<ApiError>()
            repository.getSessions().collect { result ->
                if (result.isFailure) {
                    result.exceptionOrNull()?.let {
                        assertIs<ApiError>(it)
                        errors.add(it)
                    }
                }
            }

            // Assert: ValidationErrorがスローされることを検証
            assertTrue(errors.isNotEmpty())
            val error = assertIs<ApiError.ValidationError>(errors.first())
            assertTrue(error.errorMessage.contains("Gateway API URL") || error.errorMessage.contains("invalid"))
        }
}
