package com.tossinsu.smsrelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_SERVICE = "relay_service"   // 상시 실행 알림 (조용함)
    const val CHANNEL_MESSAGE = "relay_message"   // 전달된 문자 알림

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return

        val service = NotificationChannel(
            CHANNEL_SERVICE, "상시 실행",
            NotificationManager.IMPORTANCE_MIN
        ).apply { description = "SMS 전달 앱이 백그라운드에서 동작 중임을 표시" }

        val message = NotificationChannel(
            CHANNEL_MESSAGE, "전달된 문자",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "다른 폰에서 전달된 문자 알림"
            enableVibration(true)
        }
        nm.createNotificationChannel(service)
        nm.createNotificationChannel(message)
    }

    /** 포그라운드 서비스용 상시 알림 */
    fun serviceNotification(ctx: Context, text: String): Notification {
        ensureChannels(ctx)
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(ctx, CHANNEL_SERVICE)
            .setContentTitle("SMS 전달 실행 중")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    /** 전달된 문자를 원 발신번호 제목으로 표시 */
    fun showMessage(ctx: Context, from: String, body: String, ts: Long) {
        ensureChannels(ctx)
        val pi = PendingIntent.getActivity(
            ctx, from.hashCode(), Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_MESSAGE)
            .setContentTitle(from)                       // 원 발신번호
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setWhen(ts)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        // 발신번호별로 다른 알림 ID → 번호별로 쌓임
        ctx.getSystemService(NotificationManager::class.java)
            ?.notify(("$from$ts").hashCode(), n)
    }
}
