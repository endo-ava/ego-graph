package dev.egograph.shared.core.data.repository

import dev.egograph.shared.core.domain.model.terminal.Session
import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.RepositoryResult
import dev.egograph.shared.core.domain.repository.TerminalRepository
import dev.egograph.shared.core.platform.PlatformPreferences
import dev.egograph.shared.core.platform.PlatformPrefsDefaults
import dev.egograph.shared.core.platform.PlatformPrefsKeys
import dev.egograph.shared.core.platform.getDefaultGatewayBaseUrl
import dev.egograph.shared.core.platform.normalizeBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable

/**
 * TerminalRepositoryの実装
 *
 * HTTPクライアントを使用してバックエンドAPIと通信します。
 */
class TerminalRepositoryImpl(
    private val httpClient: HttpClient,
    private val preferences: PlatformPreferences,
) : TerminalRepository {
    private val sessionsCache = InMemoryCache<String, List<Session>>()
    private val sessionCache = InMemoryCache<String, Session>()

    override fun getSessions(): Flow<RepositoryResult<List<Session>>> =
        flow {
            val cacheKey = "terminal:sessions:list"
            val cached = sessionsCache.get(cacheKey)
            if (cached != null) {
                emit(Result.success(cached))
                return@flow
            }
            try {
                val sessions = fetchSessionList()
                sessionsCache.put(cacheKey, sessions)
                emit(Result.success(sessions))
            } catch (e: ApiError) {
                emit(Result.failure(e))
            } catch (e: Exception) {
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    override fun getSession(sessionId: String): Flow<RepositoryResult<Session>> =
        flow {
            val cacheKey = "terminal:session:$sessionId"
            val cached = sessionCache.get(cacheKey)
            if (cached != null) {
                emit(Result.success(cached))
                return@flow
            }
            try {
                val session = fetchSession(sessionId)
                sessionCache.put(cacheKey, session)
                emit(Result.success(session))
            } catch (e: ApiError) {
                emit(Result.failure(e))
            } catch (e: Exception) {
                emit(Result.failure(ApiError.NetworkError(e)))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun fetchSessionList(): List<Session> {
        val config = resolveGatewayConfig()
        val response =
            httpClient.get("${config.baseUrl}/api/v1/terminal/sessions") {
                configureGatewayAuth(config.apiKey)
            }

        if (response.status == HttpStatusCode.OK) {
            return response.body<SessionListResponse>().sessions
        } else {
            throw ApiError.HttpError(
                code = response.status.value,
                errorMessage = response.status.description,
                detail = null,
            )
        }
    }

    private suspend fun fetchSession(sessionId: String): Session {
        val config = resolveGatewayConfig()
        val encodedSessionId = sessionId.encodeURLPathPart()
        val response =
            httpClient.get("${config.baseUrl}/api/v1/terminal/sessions/$encodedSessionId") {
                configureGatewayAuth(config.apiKey)
            }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            throw ApiError.HttpError(
                code = response.status.value,
                errorMessage = response.status.description,
                detail = null,
            )
        }
    }

    private fun resolveGatewayConfig(): GatewayConfig {
        val rawBaseUrl =
            preferences
                .getString(
                    PlatformPrefsKeys.KEY_GATEWAY_API_URL,
                    PlatformPrefsDefaults.DEFAULT_GATEWAY_API_URL,
                ).ifBlank { getDefaultGatewayBaseUrl() }
                .trim()

        if (rawBaseUrl.isBlank()) {
            throw ApiError.ValidationError("Gateway API URL is not configured")
        }

        val normalizedBaseUrl =
            try {
                normalizeBaseUrl(rawBaseUrl)
            } catch (e: IllegalArgumentException) {
                throw ApiError.ValidationError("Gateway API URL is invalid")
            }

        val apiKey =
            preferences
                .getString(
                    PlatformPrefsKeys.KEY_GATEWAY_API_KEY,
                    PlatformPrefsDefaults.DEFAULT_GATEWAY_API_KEY,
                ).trim()

        if (apiKey.isBlank()) {
            throw ApiError.ValidationError("Gateway API key is not configured")
        }

        return GatewayConfig(
            baseUrl = normalizedBaseUrl,
            apiKey = apiKey,
        )
    }

    private fun io.ktor.client.request.HttpRequestBuilder.configureGatewayAuth(apiKey: String) {
        headers {
            append("X-API-Key", apiKey)
        }
    }

    @Serializable
    private data class SessionListResponse(
        val sessions: List<Session>,
        val count: Int,
    )

    private data class GatewayConfig(
        val baseUrl: String,
        val apiKey: String,
    )
}
