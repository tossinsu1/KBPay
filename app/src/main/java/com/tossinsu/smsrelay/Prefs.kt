package com.tossinsu.smsrelay

import android.content.Context

/**
 * 앱 설정 저장소. 역할, 페어 코드, 화이트리스트 번호를 보관한다.
 */
object Prefs {
    private const val FILE = "sms_relay_prefs"
    private const val KEY_ROLE = "role"                 // "SENDER" | "RECEIVER"
    private const val KEY_PAIR = "pair_code"            // 두 폰이 공유하는 코드
    private const val KEY_WHITELIST = "whitelist"       // 쉼표로 구분된 번호들
    private const val KEY_LAST_TS = "last_ts"           // RECEIVER가 마지막 처리한 시각

    const val ROLE_SENDER = "SENDER"
    const val ROLE_RECEIVER = "RECEIVER"

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var Context.role: String?
        get() = sp(this).getString(KEY_ROLE, null)
        set(v) { sp(this).edit().putString(KEY_ROLE, v).apply() }

    var Context.pairCode: String
        get() = sp(this).getString(KEY_PAIR, "") ?: ""
        set(v) { sp(this).edit().putString(KEY_PAIR, v.trim()).apply() }

    /** 원본 그대로 저장된 화이트리스트 문자열 */
    var Context.whitelistRaw: String
        get() = sp(this).getString(KEY_WHITELIST, "") ?: ""
        set(v) { sp(this).edit().putString(KEY_WHITELIST, v).apply() }

    var Context.lastTs: Long
        get() = sp(this).getLong(KEY_LAST_TS, 0L)
        set(v) { sp(this).edit().putLong(KEY_LAST_TS, v).apply() }

    /** 숫자만 남긴 화이트리스트 번호 목록 */
    fun Context.whitelistNumbers(): List<String> =
        whitelistRaw.split(",", "\n", ";")
            .map { it.filter { c -> c.isDigit() } }
            .filter { it.isNotEmpty() }

    /**
     * 발신번호가 화이트리스트에 포함되는지 확인.
     * 뒤 8자리를 비교해 국가코드/하이픈 표기 차이를 흡수한다.
     */
    fun Context.matchesWhitelist(sender: String): Boolean {
        val s = sender.filter { it.isDigit() }
        if (s.isEmpty()) return false
        val list = whitelistNumbers()
        if (list.isEmpty()) return false   // 등록 번호 없으면 아무것도 전달하지 않음
        return list.any { w ->
            val a = s.takeLast(8)
            val b = w.takeLast(8)
            a == b || s.endsWith(w) || w.endsWith(s)
        }
    }
}
