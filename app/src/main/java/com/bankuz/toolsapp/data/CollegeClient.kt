package com.bankuz.toolsapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

val PORTALS = mapOf(
    "vanraj" to "https://payment.vaccdharampur.org",
    "jppacc" to "https://student.jppacc.org",
)

object CollegeClient {
    private fun client(): OkHttpClient {
        val cm = CookieManager(); cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        return OkHttpClient.Builder().cookieJar(
            object : okhttp3.CookieJar {
                private val h = okhttp3.JavaNetCookieJar(cm)
                override fun loadForRequest(u: okhttp3.HttpUrl) = h.loadForRequest(u)
                override fun saveFromResponse(u: okhttp3.HttpUrl, c: List<okhttp3.Cookie>) = h.saveFromResponse(u, c)
            }).connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
    }

    private fun csrf(html: String): String? =
        Regex("""name="_token"\s+value="([^"]+)"""").find(html)?.groupValues?.get(1)

    suspend fun findToken(email: String, portal: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base = PORTALS[portal] ?: PORTALS["vanraj"]!!
            val c = client()
            val ua = Request.Builder().url("$base/password/reset").header("User-Agent", "Mozilla/5.0").build()
            val html = c.newCall(ua).execute().use { it.body?.string() ?: "" }
            val t = csrf(html) ?: return@withContext Result.failure(Exception("CSRF nahi mila"))
            val post = Request.Builder().url("$base/password/email")
                .header("User-Agent", "Mozilla/5.0").header("Referer", "$base/password/reset")
                .post(FormBody.Builder().add("_token", t).add("email", email).build()).build()
            val resp = c.newCall(post).execute().use { it.body?.string() ?: "" }
            Regex("""password/reset/([a-f0-9]{40,})""", RegexOption.IGNORE_CASE).find(resp)?.groupValues?.get(1)?.let {
                return@withContext Result.success(it)
            }
            val low = resp.lowercase()
            when {
                "find a user with that e-mail" in low || "find a user with that email" in low ->
                    Result.failure(Exception("Is email se account nahi mila"))
                "e-mailed" in low || "password reset link" in low ->
                    Result.failure(Exception("Reset mail bhej di gayi, inbox check karo"))
                else -> Result.failure(Exception("Token nahi mila"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reset(email: String, token: String, pass: String, portal: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val base = PORTALS[portal] ?: PORTALS["vanraj"]!!
                val c = client()
                val url = "$base/password/reset/$token"
                val html = c.newCall(Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build())
                    .execute().use { it.body?.string() ?: "" }
                val t = csrf(html) ?: return@withContext Result.failure(Exception("CSRF nahi mila"))
                val post = Request.Builder().url("$base/password/reset")
                    .header("Referer", url).header("User-Agent", "Mozilla/5.0")
                    .post(FormBody.Builder().add("_token", t).add("token", token)
                        .add("email", email).add("password", pass)
                        .add("password_confirmation", pass).build()).build()
                val r = c.newCall(post).execute()
                val body = r.use { it.body?.string() ?: "" }.lowercase()
                val ok = "password has been reset" in body || "/home" in r.request.url.toString()
                if (ok) Result.success("Password reset successful!") else Result.failure(Exception("Reset fail"))
            } catch (e: Exception) { Result.failure(e) }
        }
}
