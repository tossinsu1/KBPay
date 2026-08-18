package com.tossinsu.smsrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tossinsu.smsrelay.Prefs.role

/**
 * 재부팅 후 저장된 역할에 따라 서비스를 자동으로 다시 시작한다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        when (context.role) {
            Prefs.ROLE_SENDER -> SenderService.start(context)
            Prefs.ROLE_RECEIVER -> ReceiverService.start(context)
        }
    }
}
