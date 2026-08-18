package dev.clxud.pawtap

import android.content.Context
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject

data class Mapping(val keyCode: Int, val displayId: Int, val x: Float, val y: Float) {
    fun keyName(): String = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
}

object Store {
    private const val PREFS = "pawtap"
    private const val KEY = "mappings"
    private const val ENABLED = "enabled"

    fun load(ctx: Context): MutableList<Mapping> {
        val raw = ctx.getSharedPreferences(PREFS, 0).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Mapping(o.getInt("key"), o.getInt("display"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat())
        }
    }

    fun save(ctx: Context, list: List<Mapping>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().put("key", m.keyCode).put("display", m.displayId).put("x", m.x).put("y", m.y))
        }
        ctx.getSharedPreferences(PREFS, 0).edit().putString(KEY, arr.toString()).apply()
        PawtapService.instance?.reload()
    }

    fun enabled(ctx: Context) = ctx.getSharedPreferences(PREFS, 0).getBoolean(ENABLED, true)
    fun setEnabled(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean(ENABLED, on).apply()
        PawtapService.instance?.reload()
    }
}
