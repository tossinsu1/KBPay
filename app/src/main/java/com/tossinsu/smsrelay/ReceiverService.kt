package com.tossinsu.smsrelay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.tossinsu.smsrelay.Prefs.lastTs
import com.tossinsu.smsrelay.Prefs.pairCode

/**
 * RECEIVER(폰B)용 포그라운드 서비스.
 * RTDB의 새 메시지를 실시간으로 받아 원 발신번호 제목으로 알림을 띄우고
 * 로컬 목록에 저장한다. 이미 처리한 메시지는 lastTs로 걸러 중복을 막는다.
 */
class ReceiverService : Service() {

    private var query: Query? = null
    private var listener: ChildEventListener? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(ID, NotificationHelper.serviceNotification(this, "문자 수신 대기 중"))
        attachListener()
    }

    private fun attachListener() {
        val code = pairCode
        if (code.isBlank()) {
            Log.w(TAG, "페어 코드 없음")
            return
        }
        // 마지막 처리 시각 이후의 메시지만 구독
        val q = FirebaseRefs.messages(code).orderByChild("ts").startAt((lastTs + 1).toDouble())
        val l = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val m = snapshot.getValue(RelayMessage::class.java) ?: return
                if (m.ts <= lastTs) return
                lastTs = m.ts
                MessageStore.add(this@ReceiverService, m)
                NotificationHelper.showMessage(this@ReceiverService, m.from, m.body, m.ts)
                Log.d(TAG, "수신: ${m.from}")
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "구독 취소: ${error.message}")
            }
        }
        q.addChildEventListener(l)
        query = q
        listener = l
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        listener?.let { query?.removeEventListener(it) }
        super.onDestroy()
    }

    companion object {
        private const val ID = 1002
        private const val TAG = "ReceiverService"
        fun start(ctx: Context) {
            androidx.core.content.ContextCompat.startForegroundService(
                ctx, Intent(ctx, ReceiverService::class.java)
            )
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ReceiverService::class.java))
        }
    }
}
