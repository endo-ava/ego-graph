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
 * - Timeout settings (30s request, 10s connect)
 * - Retry logic with exponential backoff (3 retries)
 * - JSON content negotiation with kotlinx.serialization
 * - Request/response logging with Kermit
 */
actual fun provideHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(false)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }

        install(HttpRequestRetry) {
            maxRetries = 3
            exponentialDelay(baseDelayMs = 1000, maxDelayMs = 4000)
            retryOnServerErrors(maxRetries = 3)
            retryOnExceptionIf { _, cause ->
                cause is IOException || cause is HttpRequestTimeoutException
            }
            modifyRequest {
                KermitLogger.withTag("HttpClient").w {
                    "Request failed (attempt $retryCount/3), retrying..."
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
