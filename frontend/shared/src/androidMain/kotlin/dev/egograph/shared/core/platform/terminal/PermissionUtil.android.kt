package dev.egograph.shared.core.platform.terminal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Androidパーミッションユーティリティ実装
 *
 * RECORD_AUDIOパーミッションのリクエストを処理する。
 */
actual fun createPermissionUtil(): PermissionUtil =
    object : PermissionUtil {
        private var permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>? = null

        override suspend fun requestRecordAudioPermission(): PermissionResult =
            suspendCancellableCoroutine { continuation ->
                val activity =
                    ActivityRecorder.currentActivity as? ComponentActivity
                        ?: run {
                            continuation.resume(PermissionResult(granted = false))
                            return@suspendCancellableCoroutine
                        }

                // パーミッションランチャーの設定
                val launcher =
                    activity.activityResultRegistry.register(
                        "record_audio_permission",
                        ActivityResultContracts.RequestPermission(),
                    ) { isGranted ->
                        if (continuation.isActive) {
                            continuation.resume(PermissionResult(granted = isGranted))
                        }
                    }

                permissionLauncher = launcher

                // パーミッションをリクエスト
                launcher.launch(Manifest.permission.RECORD_AUDIO)

                continuation.invokeOnCancellation {
                    // キャンセル時にランチャーを解除
                    permissionLauncher?.unregister()
                }
            }

        override fun hasRecordAudioPermission(): Boolean {
            val context = ActivityRecorder.currentActivity ?: return false
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        }

        private var notificationLauncher: androidx.activity.result.ActivityResultLauncher<String>? = null

        override suspend fun requestPostNotificationsPermission(): PermissionResult =
            suspendCancellableCoroutine { continuation ->
                val activity =
                    ActivityRecorder.currentActivity as? ComponentActivity
                        ?: run {
                            continuation.resume(PermissionResult(granted = false))
                            return@suspendCancellableCoroutine
                        }

                // パーミッションランチャーの設定
                val launcher =
                    activity.activityResultRegistry.register(
                        "post_notifications_permission",
                        ActivityResultContracts.RequestPermission(),
                    ) { isGranted ->
                        if (continuation.isActive) {
                            continuation.resume(PermissionResult(granted = isGranted))
                        }
                    }

                notificationLauncher = launcher

                // パーミッションをリクエスト
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)

                continuation.invokeOnCancellation {
                    // キャンセル時にランチャーを解除
                    notificationLauncher?.unregister()
                }
            }

        override fun hasPostNotificationsPermission(): Boolean {
            val context = ActivityRecorder.currentActivity ?: return false
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
