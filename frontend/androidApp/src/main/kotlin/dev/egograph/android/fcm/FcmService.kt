package dev.egograph.android.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Firebase Cloud Messaging サービス。

FCMトークンの更新とメッセージ受信を処理します。
*/
class FcmService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FcmService"
        private const val PREFS_NAME = "egograph_prefs"
        private const val KEY_GATEWAY_API_URL = "gateway_api_url"
        private const val KEY_GATEWAY_API_KEY = "gateway_api_key"
        private const val TOKEN_PREVIEW_LENGTH = 10
    }

    private var tokenManager: FcmTokenManager? = null

    /** FCMトークンが更新されたときに呼ばれます。

     * @param token 新しいFCMトークン
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: ${token.take(TOKEN_PREVIEW_LENGTH)}...")

        getTokenManager()?.registerToken(
            token = token,
            deviceName = android.os.Build.MODEL,
        )
    }

    override fun onCreate() {
        super.onCreate()

        // 起動時に既存トークンの登録も試行
        FirebaseMessaging
            .getInstance()
            .token
            .addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    getTokenManager()?.registerToken(
                        token = token,
                        deviceName = android.os.Build.MODEL,
                    )
                }
            }.addOnFailureListener { error ->
                Log.w(TAG, "Failed to fetch initial FCM token: ${error.message}")
            }
    }

    private fun getTokenManager(): FcmTokenManager? {
        tokenManager?.let { return it }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gatewayUrl = prefs.getString(KEY_GATEWAY_API_URL, "")?.trim().orEmpty()
        val apiKey =
            prefs
                .getString(KEY_GATEWAY_API_KEY, "")
                ?.trim()
                .orEmpty()

        val manager: FcmTokenManager? =
            if (gatewayUrl.isBlank() || apiKey.isBlank()) {
                Log.w(TAG, "Skip FCM token registration: api_url or api_key is empty")
                null
            } else {
                FcmTokenManager(gatewayUrl = gatewayUrl, apiKey = apiKey)
            }

        manager?.let { tokenManager = it }
        return manager
    }

    override fun onDestroy() {
        tokenManager?.cleanup()
        tokenManager = null
        super.onDestroy()
    }

    /** FCMメッセージを受信したときに呼ばれます。

     * @param message 受信したRemoteMessage
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // 通知ペイロードの処理
        message.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            // システム通知は自動的に表示されます
        }

        // データペイロードの処理
        message.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${message.data}")

            val type = message.data["type"]
            val sessionId = message.data["session_id"]

            when (type) {
                "task_completed" -> {
                    Log.d(TAG, "Task completed: $sessionId")
                    // TODO: タスク完了時の処理
                }
            }
        }
    }
}
