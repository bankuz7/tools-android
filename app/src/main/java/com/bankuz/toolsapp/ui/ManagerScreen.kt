package com.bankuz.toolsapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bankuz.toolsapp.data.PaymentItem
import com.bankuz.toolsapp.data.RazorpayClient
import com.bankuz.toolsapp.util.Prefs
import kotlinx.coroutines.launch

@Composable
fun ManagerScreen(mod: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ugId by remember { mutableStateOf(Prefs.get(ctx, "ug_id")) }
    var ugSec by remember { mutableStateOf(Prefs.get(ctx, "ug_sec")) }
    var pgId by remember { mutableStateOf(Prefs.get(ctx, "pg_id")) }
    var pgSec by remember { mutableStateOf(Prefs.get(ctx, "pg_sec")) }
    var msg by remember { mutableStateOf("") }
    var pays by remember { mutableStateOf(emptyList<PaymentItem>()) }
    var busy by remember { mutableStateOf(false) }
    var pid by remember { mutableStateOf("") }
    var amt by remember { mutableStateOf("") }

    fun save() {
        Prefs.set(ctx, "ug_id", ugId); Prefs.set(ctx, "ug_sec", ugSec)
        Prefs.set(ctx, "pg_id", pgId); Prefs.set(ctx, "pg_sec", pgSec)
    }
    Column(mod.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("UG / PG keys (TAX optional — same fields reuse)", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(ugId, { ugId = it }, { Text("UG Key ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(ugSec, { ugSec = it }, { Text("UG Secret") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pgId, { pgId = it }, { Text("PG Key ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pgSec, { pgSec = it }, { Text("PG Secret") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ save(); busy = true; msg = ""
                scope.launch {
                    val a = if (ugId.isNotBlank()) RazorpayClient.fetchPayments(ugId.trim(), ugSec.trim(), "UG") else emptyList()
                    val b = if (pgId.isNotBlank()) RazorpayClient.fetchPayments(pgId.trim(), pgSec.trim(), "PG") else emptyList()
                    pays = (a + b).sortedByDescending { it.createdAt }
                    val bal = if (ugId.isNotBlank()) RazorpayClient.fetchBalance(ugId.trim(), ugSec.trim()) else null
                    msg = if (bal != null) "UG balance ₹${bal.first} (locked ₹${bal.second}) · ${pays.size} payments" else "${pays.size} payments"
                    busy = false
                }
            }, enabled = !busy) { Text("Load") }
        }
        if (msg.isNotEmpty()) Text(msg, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(pid, { pid = it }, { Text("Payment ID for refund (pay_...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amt, { amt = it }, { Text("Amount ₹ (blank = full)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({
            busy = true
            scope.launch {
                msg = try { RazorpayClient.createRefund(ugId.trim(), ugSec.trim(), pid.trim(), amt.trim()) }
                catch (e: Exception) { e.message ?: "fail" }
                busy = false
            }
        }, enabled = !busy && pid.isNotBlank()) { Text("Refund via UG") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(pays.take(200)) { p ->
                ElevatedCard {
                    Column(Modifier.padding(8.dp)) {
                        Text("[${p.key}] ₹${p.amount} · ${p.status}", style = MaterialTheme.typography.titleSmall)
                        Text("${p.id}\n${p.email} ${p.contact}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
