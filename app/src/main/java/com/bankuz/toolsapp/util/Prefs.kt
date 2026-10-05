package com.bankuz.toolsapp.util

import android.content.Context

// ponytail: SharedPreferences over DataStore, single small helper is enough
object Prefs {
    private const val F = "toolsapp"
    fun get(c: Context, k: String, d: String = "") =
        c.getSharedPreferences(F, Context.MODE_PRIVATE).getString(k, d) ?: d
    fun set(c: Context, k: String, v: String) =
        c.getSharedPreferences(F, Context.MODE_PRIVATE).edit().putString(k, v).apply()
}
