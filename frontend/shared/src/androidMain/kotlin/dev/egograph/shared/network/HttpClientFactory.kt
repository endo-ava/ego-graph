package dev.egograph.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.utils.unwrapCancellationException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Protocol
import java.util.concurrent.TimeUnit
import co.touchlab.kermit.Logger as KermitLogger

/**
 * Android-specific HTTP Client Factory implementation
 *
 * Creates a Ktor HttpClient configured with:
 * - OkHttp engine
 * - Timeout settings (30s request, 10s connect)
 * - Retry logic (3 retries on server errors or exceptions)
 * - JSON content negotiation with kotlinx.serialization
 * - Request/response logging with Kermit
 */
actual class HttpClientFactory {
    actual fun create(): HttpClient =
        HttpClient(OkHttp) {
            engine {
                config {
                    connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
                    protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    retryOnConnectionFailure(true)
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }

            install(HttpRequestRetry) {
                maxRetries = 1
                retryOnServerErrors(maxRetries = 1)
                retryOnExceptionIf { _, cause ->
                    val unwrapped = cause.unwrapCancellationException()
                    unwrapped is Exception
                }
                constantDelay(100)
            }

            install(ContentEncoding) {
                gzip()
                deflate()
                // brotli() // Only if backend supports Brotli
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
}
