package dev.egograph.shared.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

expect class DiskCache {
    constructor(context: DiskCacheContext)

    suspend fun <T> getOrFetch(
        key: String,
        serializer: KSerializer<T>,
        maxAgeMs: Long = 300000L,
        fetch: suspend () -> T,
    ): T

    suspend fun remove(key: String)
}

expect class DiskCacheContext {
    val cacheDirPath: String

    constructor(cacheDirPath: String)
}

suspend inline fun <reified T> DiskCache.getOrFetch(
    key: String,
    maxAgeMs: Long = 300000L,
    noinline fetch: suspend () -> T,
): T where T : Serializable =
    getOrFetch(
        key = key,
        serializer = serializer(),
        maxAgeMs = maxAgeMs,
        fetch = fetch,
    )
