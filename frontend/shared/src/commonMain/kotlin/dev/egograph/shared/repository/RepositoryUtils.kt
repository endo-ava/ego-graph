package dev.egograph.shared.repository

import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

internal const val DEFAULT_CACHE_DURATION_MS = 60000L

internal data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
)

internal fun generateContextHash(
    baseUrl: String,
    apiKey: String,
): String {
    val combined = "$baseUrl:$apiKey"
    // Use 64-bit FNV-1a hash for better collision resistance than 32-bit
    var hash: ULong = 0xcbf29ce484222325u // FNV offset basis
    val fnvPrime: ULong = 0x100000001b3u
    for (byte in combined.toByteArray(Charsets.UTF_8)) {
        hash = hash xor byte.toULong()
        hash = hash * fnvPrime
    }
    return hash.toString(16).padStart(16, '0')
}

internal fun HttpRequestBuilder.configureAuth(apiKey: String) {
    if (apiKey.isNotEmpty()) {
        headers {
            append("X-API-Key", apiKey)
        }
    }
}

internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(
    crossinline logError: (Exception) -> Unit = { e -> Logger.w(e) { "Failed to read error response body" } },
    fallbackDetail: String? = null,
): T {
    if (status == HttpStatusCode.OK) {
        return body()
    } else {
        val errorDetail =
            try {
                body<String>()
            } catch (e: Exception) {
                logError(e)
                fallbackDetail
            }
        throw ApiError.HttpError(
            code = status.value,
            errorMessage = status.description,
            detail = errorDetail,
        )
    }
}
