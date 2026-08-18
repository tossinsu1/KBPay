package com.tossinsu.smsrelay

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * RECEIVER가 받은 메시지를 로컬에 보관하고 목록 화면에 노출한다.
 * 간단하게 SharedPreferences에 JSON으로 저장(최근 200건).
 */
object MessageStore {
    private const val FILE = "sms_relay_msgs"
    private const val KEY = "list"
    private const val MAX = 200

    private val items = mutableListOf<RelayMessage>()
    private var loaded = false
    var onChanged: (() -> Unit)? = null

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        items.clear()
        val raw = sp(ctx).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(RelayMessage(o.optString("from"), o.optString("body"), o.optLong("ts")))
        }
        loaded = true
    }

    @Synchronized
    fun add(ctx: Context, m: RelayMessage) {
        load(ctx)
        items.add(0, m)
        while (items.size > MAX) items.removeAt(items.size - 1)
        persist(ctx)
        onChanged?.invoke()
    }

    @Synchronized
    fun all(ctx: Context): List<RelayMessage> {
        load(ctx)
        return items.toList()
    }

    @Synchronized
    fun clear(ctx: Context) {
        items.clear()
        persist(ctx)
        onChanged?.invoke()
    }

    private fun persist(ctx: Context) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("from", it.from).put("body", it.body).put("ts", it.ts))
        }
        sp(ctx).edit().putString(KEY, arr.toString()).apply()
    }
}
