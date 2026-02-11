package dev.egograph.android.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.egograph.app.MainActivity

/**
 * 通知表示ユーティリティ
 *
 * NotificationCompat.Builderを使用して通知を作成・表示する。
 */
object NotificationDisplayer {
    private const val NOTIFICATION_ID = 1001

    /**
     * 通知を表示する
     *
     * @param context アプリケーションコンテキスト
     * @param title 通知タイトル
     * @param message 通知メッセージ本文
     * @param channelId 通知チャンネルID
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = NotificationChannelManager.getChannelId(),
    ) {
        // 通知をタップした時にMainActivityを開くインテントを作成
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        // 通知ビルダーで通知を作成
        val notification =
            NotificationCompat
                .Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        // 通知マネージャーで通知を表示
        val notificationManager = context.getSystemService<NotificationManager>()
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 通知をキャンセルする
     *
     * @param context アプリケーションコンテキスト
     */
    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>()
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
