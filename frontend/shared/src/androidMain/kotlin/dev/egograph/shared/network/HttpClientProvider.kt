package dev.egograph.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.utils.unwrapCancellationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.SocketTimeoutException
import co.touchlab.kermit.Logger as KermitLogger

private val IDEMPOTENT_METHODS = setOf(
    HttpMethod.Get,
    HttpMethod.Head,
    HttpMethod.Options,
    HttpMethod.Put,
    HttpMethod.Delete
)

/**
 * Android-specific HttpClient provider
 *
 * Creates a Ktor HttpClient configured with:
 * - Android engine
 * - Timeout settings (30s request, 10s connect)
 * - Retry logic (3 retries on server errors for idempotent methods, transient network exceptions only)
 * - JSON content negotiation with kotlinx.serialization
 * - Request/response logging with Kermit
 */
actual fun provideHttpClient(): HttpClient = HttpClient(Android) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }

    install(HttpRequestRetry) {
        maxRetries = 3
        retryOnServerErrors(maxRetries = 3)
        retryIf { request, _ -> request.method in IDEMPOTENT_METHODS }
        retryOnExceptionIf { request, cause ->
            val unwrapped = cause.unwrapCancellationException()
            request.method in IDEMPOTENT_METHODS &&
                (unwrapped is IOException || unwrapped is SocketTimeoutException)
        }
        exponentialDelay()
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        })
    }

    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            private val logger = KermitLogger.withTag("HttpClient")
            override fun log(message: String) {
                logger.i(message)
            }
        }
    }
}
