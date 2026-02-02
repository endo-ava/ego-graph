package dev.egograph.shared.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

actual class DiskCacheContext actual constructor(
    actual val cacheDirPath: String,
)

actual class DiskCache actual constructor(
    private val context: DiskCacheContext,
) {
    private val cacheDir = File(context.cacheDirPath, "api_cache")
    private val ioMutex = Mutex()
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    @Serializable
    private data class DiskCacheEntry<T>(
        val timestamp: Long,
        val data: T,
    )

    actual suspend fun <T> getOrFetch(
        key: String,
        serializer: KSerializer<T>,
        maxAgeMs: Long,
        fetch: suspend () -> T,
    ): T {
        val cached = readFromDisk(key, serializer, maxAgeMs)
        if (cached != null) {
            return cached
        }

        val result = fetch()
        writeToDisk(key, serializer, result)
        return result
    }

    actual suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            ioMutex.withLock {
                getCacheFile(key).delete()
            }
        }
    }

    private suspend fun <T> readFromDisk(
        key: String,
        serializer: KSerializer<T>,
        maxAgeMs: Long,
    ): T? =
        withContext(Dispatchers.IO) {
            ioMutex.withLock {
                val cacheFile = getCacheFile(key)
                if (!cacheFile.exists()) {
                    return@withLock null
                }
                try {
                    val content = cacheFile.readText()
                    val entrySerializer = DiskCacheEntry.serializer(serializer)
                    val entry = json.decodeFromString(entrySerializer, content)
                    val ageMs = System.currentTimeMillis() - entry.timestamp
                    if (ageMs > maxAgeMs) {
                        cacheFile.delete()
                        return@withLock null
                    }
                    entry.data
                } catch (_: Exception) {
                    cacheFile.delete()
                    null
                }
            }
        }

    private suspend fun <T> writeToDisk(
        key: String,
        serializer: KSerializer<T>,
        value: T,
    ) {
        withContext(Dispatchers.IO) {
            ioMutex.withLock {
                val cacheFile = getCacheFile(key)
                val entry = DiskCacheEntry(timestamp = System.currentTimeMillis(), data = value)
                val entrySerializer = DiskCacheEntry.serializer(serializer)
                val payload = json.encodeToString(entrySerializer, entry)
                val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                tempFile.writeText(payload)
                if (!tempFile.renameTo(cacheFile)) {
                    cacheFile.writeText(payload)
                    tempFile.delete()
                }
            }
        }
    }

    private fun getCacheFile(key: String): File {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, sha256(key))
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        val builder = StringBuilder(digest.size * 2)
        for (byte in digest) {
            builder.append("%02x".format(byte))
        }
        return builder.toString()
    }
}
