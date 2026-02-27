package dev.egograph.shared.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException
import co.touchlab.kermit.Logger as KermitLogger

/**
 * Android-specific HttpClient provider
 *
 * Creates a Ktor HttpClient configured with:
 * - OkHttp engine
 * - Timeout settings from HttpClientConfig
 * - Retry logic with exponential backoff
 * - JSON content negotiation with kotlinx.serialization
 * - Request/response logging with Kermit
 *
 * @param config HTTPクライアント設定
 * @return 設定適用済みのHttpClient
 */
actual fun provideHttpClient(config: HttpClientConfig): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(false)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }

        install(HttpRequestRetry) {
            maxRetries = config.maxRetries
            exponentialDelay(baseDelayMs = config.retryBaseDelayMs, maxDelayMs = config.retryMaxDelayMs)
            retryIf { request, response ->
                response.status.value >= 500
            }
            retryOnExceptionIf { _, cause ->
                cause is IOException || cause is HttpRequestTimeoutException
            }
            // 401/403は再認証が必要なためリトライしない
            retryIf { request, response ->
                response.status.value >= 500
            }
            modifyRequest {
                KermitLogger.withTag("HttpClient").w {
                    "Request failed (attempt $retryCount/${config.maxRetries}), retrying..."
                }
            }
        }

        install(ContentEncoding) {
            gzip()
            deflate()
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                },
            )
        }

        install(Logging) {
            level = LogLevel.INFO
            logger =
                object : Logger {
                    private val logger = KermitLogger.withTag("HttpClient")

                    override fun log(message: String) {
                        logger.i(message)
                    }
                }
        }
    }
