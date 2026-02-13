package dev.egograph.shared.core.data.repository.internal

import co.touchlab.kermit.Logger
import dev.egograph.shared.core.domain.repository.ApiError
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

internal const val DEFAULT_CACHE_DURATION_MS = 60000L

internal data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
)

internal class InMemoryCache<K, V>(
    private val expirationMs: Long = DEFAULT_CACHE_DURATION_MS,
) {
    private val mutex = Mutex()
    private var cache: Map<K, CacheEntry<V>> = emptyMap()

    suspend fun get(key: K): V? =
        mutex.withLock {
            val entry = cache[key]
            if (entry != null && Clock.System.now().toEpochMilliseconds() - entry.timestamp < expirationMs) {
                entry.data
            } else {
                null
            }
        }

    suspend fun put(
        key: K,
        value: V,
    ) = mutex.withLock {
        cache = cache + (key to CacheEntry(value))
    }

    suspend fun remove(key: K) =
        mutex.withLock {
            cache = cache - key
        }

    suspend fun clear() =
        mutex.withLock {
            cache = emptyMap()
        }
}

internal fun generateContextHash(
    baseUrl: String,
    apiKey: String,
): String {
    val combined = "$baseUrl:$apiKey"
    var hash: ULong = 0xcbf29ce484222325u
    val fnvPrime: ULong = 0x100000001b3u
    for (byte in combined.toByteArray(Charsets.UTF_8)) {
        hash = hash xor byte.toUByte().toULong()
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
