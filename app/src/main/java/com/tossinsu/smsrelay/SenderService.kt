package com.tossinsu.smsrelay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.tossinsu.smsrelay.Prefs.whitelistNumbers

/**
 * SENDER(폰A)용 포그라운드 서비스.
 * 실제 전달은 SmsReceiver가 처리하지만, 이 서비스가 떠 있어야
 * 프로세스가 잘 안 죽고 SMS 브로드캐스트를 안정적으로 받는다.
 */
class SenderService : Service() {

    override fun onCreate() {
        super.onCreate()
        val count = whitelistNumbers().size
        startForeground(
            ID,
            NotificationHelper.serviceNotification(this, "감시 번호 ${count}개 · 문자 수신 대기 중")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ID = 1001
        fun start(ctx: Context) {
            val i = Intent(ctx, SenderService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(ctx, i)
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, SenderService::class.java))
        }
    }
}
