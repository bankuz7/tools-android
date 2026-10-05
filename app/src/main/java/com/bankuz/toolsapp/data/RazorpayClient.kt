package com.bankuz.toolsapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RefundItem(
    val id: String,
    val paymentId: String,
    val amount: Double,
    val status: String,
    val speed: String,
    val createdAt: Long,
    val email: String,
    val contact: String,
    val method: String,
    val orderId: String,
    val arn: String,
)

data class PaymentItem(
    val id: String,
    val amount: Double,
    val status: String,
    val method: String,
    val email: String,
    val contact: String,
    val key: String,
    val createdAt: Long,
)

object RazorpayClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    private suspend fun get(path: String, kid: String, sec: String): JSONObject =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url("https://api.razorpay.com/v1$path")
                .header("Authorization", Credentials.basic(kid, sec)).get().build()
            http.newCall(req).execute().use {
                val b = it.body?.string() ?: "{}"
                if (!it.isSuccessful) throw Exception("Razorpay ${it.code}: ${b.take(200)}")
                JSONObject(b)
            }
        }

    suspend fun fetchRefunds(kid: String, sec: String, from: Long = 0, to: Long = 0): List<RefundItem> {
        val out = mutableListOf<RefundItem>()
        var skip = 0
        while (true) {
            var p = "/refunds?count=100&skip=$skip"
            if (from > 0) p += "&from=$from"
            if (to > 0) p += "&to=$to"
            val items = get(p, kid, sec).optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val r = items.getJSONObject(i)
                val pid = r.optString("payment_id")
                var email = ""; var contact = ""; var method = ""; var orderId = ""
                if (pid.isNotEmpty()) {
                    try {
                        val pay = get("/payments/$pid", kid, sec)
                        email = pay.optString("email"); contact = pay.optString("contact")
                        method = pay.optString("method"); orderId = pay.optString("order_id")
                    } catch (_: Exception) {}
                }
                val acq = r.optJSONObject("acquirer_data")
                out += RefundItem(
                    r.optString("id"), pid, r.optLong("amount") / 100.0,
                    r.optString("status"), r.optString("speed_processed", r.optString("speed_requested")),
                    r.optLong("created_at"), email, contact, method, orderId,
                    acq?.optString("arn", acq.optString("rrn")) ?: "",
                )
            }
            if (items.length() < 100 || out.size >= 2000) break
            skip += 100
        }
        return out
    }

    suspend fun fetchPayments(kid: String, sec: String, label: String, count: Int = 50): List<PaymentItem> =
        try {
            val items = get("/payments?count=$count", kid, sec).optJSONArray("items")
                ?: return emptyList()
            (0 until items.length()).map {
                val p = items.getJSONObject(it)
                PaymentItem(p.optString("id"), p.optLong("amount") / 100.0, p.optString("status"),
                    p.optString("method"), p.optString("email"), p.optString("contact"),
                    label, p.optLong("created_at"))
            }
        } catch (_: Exception) { emptyList() }

    suspend fun fetchBalance(kid: String, sec: String): Pair<Double, Double>? = try {
        val j = get("/balance", kid, sec)
        (j.optDouble("balance") / 100.0) to (j.optDouble("locked_balance") / 100.0)
    } catch (_: Exception) { null }

    suspend fun createRefund(kid: String, sec: String, paymentId: String, amountRs: String, speed: String = "optimum"): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("speed", speed)
            if (amountRs.isNotBlank()) body.put("amount", (amountRs.toDouble() * 100).toInt())
            val req = Request.Builder().url("https://api.razorpay.com/v1/payments/$paymentId/refund")
                .header("Authorization", Credentials.basic(kid, sec))
                .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), body.toString())).build()
            http.newCall(req).execute().use { "${it.code}: ${it.body?.string()?.take(300)}" }
        }
}
