package dev.egograph.android.fcm

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** FCMトークン管理マネージャー。

Gatewayへのトークン登録と定期的な更新を行います。
*/
class FcmTokenManager(
    private val gatewayUrl: String,
    private val apiKey: String,
) {
    companion object {
        private const val TAG = "FcmTokenManager"
        private const val TOKEN_PREVIEW_LENGTH = 10
        private const val TIMEOUT_SECONDS = 10L
        private const val MAX_RETRY_COUNT = 5
        private const val RETRY_DELAY_MS = 60_000L // 1分
        private const val INITIAL_RETRY_DELAY_MS = 5_000L // 5秒
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Volatile
    private var currentToken: String? = null

    /** FCMトークンをGatewayに登録します。

     * @param token FCMトークン
     * @param deviceName デバイス名（オプション）
     */
    fun registerToken(
        token: String,
        deviceName: String? = null,
    ) {
        if (token == currentToken) {
            Log.d(TAG, "Token already registered")
            return
        }

        Log.d(TAG, "Registering FCM token: ${token.take(TOKEN_PREVIEW_LENGTH)}...")

        scope.launch {
            registerTokenWithRetry(token, deviceName)
        }
    }

    /** リトライ付きでトークンを登録します。 */
    private suspend fun registerTokenWithRetry(
        token: String,
        deviceName: String?,
        retryDelay: Long = INITIAL_RETRY_DELAY_MS,
    ) {
        var attempt = 0
        var currentDelay = retryDelay

        while (attempt < MAX_RETRY_COUNT) {
            try {
                sendTokenToGateway(token, deviceName)
                currentToken = token
                Log.i(TAG, "FCM token registered successfully")
                return
            } catch (e: IOException) {
                attempt++
                Log.w(TAG, "Failed to register token (attempt $attempt/$MAX_RETRY_COUNT): ${e.message}")

                if (attempt < MAX_RETRY_COUNT) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(RETRY_DELAY_MS)
                }
            } catch (e: CancellationException) {
                Log.w(TAG, "Token registration cancelled")
                throw e
            }
        }

        Log.e(TAG, "Failed to register FCM token after $MAX_RETRY_COUNT attempts")
    }

    /** Gatewayにトークンを送信します。 */
    private fun sendTokenToGateway(
        token: String,
        deviceName: String?,
    ) {
        val jsonObject = JSONObject()
        jsonObject.put("device_token", token)
        jsonObject.put("platform", "android")
        if (!deviceName.isNullOrBlank()) {
            jsonObject.put("device_name", deviceName)
        }

        val json = jsonObject.toString()
        val body = json.toRequestBody("application/json".toMediaType())

        val req =
            Request
                .Builder()
                .url("$gatewayUrl/v1/push/token")
                .addHeader("X-API-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .put(body)
                .build()

        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }
        }
    }

    /** リソースを解放します。 */
    fun cleanup() {
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
