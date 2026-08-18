package com.tossinsu.smsrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.tossinsu.smsrelay.Prefs.matchesWhitelist
import com.tossinsu.smsrelay.Prefs.pairCode
import com.tossinsu.smsrelay.Prefs.role

/**
 * SMS 수신 시 호출. SENDER 역할일 때만 동작하며,
 * 화이트리스트 번호와 일치하면 RTDB로 전달한다.
 * 여러 파트로 쪼개진 긴 문자는 발신자별로 합쳐서 처리한다.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (context.role != Prefs.ROLE_SENDER) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // 멀티파트 문자 합치기 (발신자별)
        val grouped = messages.groupBy { it.originatingAddress ?: "" }
        for ((sender, parts) in grouped) {
            if (sender.isBlank()) continue
            if (!context.matchesWhitelist(sender)) {
                Log.d(TAG, "화이트리스트 미포함, 무시: $sender")
                continue
            }
            val body = parts.joinToString("") { it.messageBody ?: "" }
            val ts = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
            push(context, sender, body, ts)
        }
    }

    private fun push(context: Context, from: String, body: String, ts: Long) {
        val code = context.pairCode
        if (code.isBlank()) {
            Log.w(TAG, "페어 코드 없음, 전달 불가")
            return
        }
        val msg = RelayMessage(from = from, body = body, ts = ts)
        FirebaseRefs.messages(code).push().setValue(msg)
            .addOnSuccessListener { Log.d(TAG, "전달 성공: $from") }
            .addOnFailureListener { e -> Log.e(TAG, "전달 실패", e) }
    }

    companion object { private const val TAG = "SmsReceiver" }
}
