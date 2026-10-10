package com.bankuz.toolsapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

data class RazorpayKey(val label: String, val keyId: String, val keySecret: String)

// ponytail: EncryptedSharedPreferences direct, no Hilt/KeyStore abstraction needed
object SecureStore {
    private const val F = "razorpay_keys_secure"

    private fun prefs(c: Context): SharedPreferences = try {
        EncryptedSharedPreferences.create(
            c, F,
            MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        c.getSharedPreferences(F, Context.MODE_PRIVATE)
    }

    fun getKeys(c: Context): List<RazorpayKey> = try {
        val s = prefs(c).getString("keys", null) ?: return migrateOld(c)
        JSONArray(s).let { a -> (0 until a.length()).map {
            val o = a.getJSONObject(it)
            RazorpayKey(o.optString("label"), o.optString("keyId"), o.optString("keySecret"))
        } }
    } catch (_: Exception) { emptyList() }

    fun saveKeys(c: Context, keys: List<RazorpayKey>) {
        val a = JSONArray()
        keys.forEach { a.put(JSONObject().put("label", it.label).put("keyId", it.keyId).put("keySecret", it.keySecret)) }
        prefs(c).edit().putString("keys", a.toString()).apply()
    }

    // old plain Prefs ug/pg keys -> migrate once into encrypted store
    private fun migrateOld(c: Context): List<RazorpayKey> {
        val p = c.getSharedPreferences("toolsapp", Context.MODE_PRIVATE)
        val out = listOf("UG" to ("ug_id" to "ug_sec"), "PG" to ("pg_id" to "pg_sec"))
            .mapNotNull { (l, k) ->
                val id = p.getString(k.first, "")?.trim().orEmpty()
                val sec = p.getString(k.second, "")?.trim().orEmpty()
                if (id.isNotBlank()) RazorpayKey(l, id, sec) else null
            }
        if (out.isNotEmpty()) saveKeys(c, out)
        return out
    }

    fun appPassword(c: Context): String = prefs(c).getString("app_pass", "") ?: ""
    fun setAppPassword(c: Context, v: String) = prefs(c).edit().putString("app_pass", v).apply()
    fun unlocked(c: Context): Boolean = prefs(c).getBoolean("unlocked", false)
    fun setUnlocked(c: Context, v: Boolean) = prefs(c).edit().putBoolean("unlocked", v).apply()
}
