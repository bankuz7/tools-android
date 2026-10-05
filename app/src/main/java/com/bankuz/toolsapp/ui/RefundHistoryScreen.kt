package com.bankuz.toolsapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bankuz.toolsapp.data.RazorpayClient
import com.bankuz.toolsapp.util.Prefs
import kotlinx.coroutines.launch

@Composable
fun RefundHistoryScreen(mod: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var kid by remember { mutableStateOf(Prefs.get(ctx, "rr_kid")) }
    var sec by remember { mutableStateOf(Prefs.get(ctx, "rr_sec")) }
    var q by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(emptyList<com.bankuz.toolsapp.data.RefundItem>()) }
    var receipt by remember { mutableStateOf<com.bankuz.toolsapp.data.RefundItem?>(null) }
    var rname by remember { mutableStateOf("") }
    var upi by remember { mutableStateOf("") }

    Column(mod.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(kid, { kid = it }, { Text("Key ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(sec, { sec = it }, { Text("Key Secret") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({ Prefs.set(ctx, "rr_kid", kid); Prefs.set(ctx, "rr_sec", sec)
            loading = true; err = ""
            scope.launch {
                try { list = RazorpayClient.fetchRefunds(kid.trim(), sec.trim()) }
                catch (e: Exception) { err = e.message ?: "fail" }
                loading = false
            }
        }, enabled = !loading) { Text(if (loading) "Loading..." else "Fetch (${list.size})") }
        if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
        OutlinedTextField(q, { q = it }, { Text("Search id / email / contact") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val total = remember(list) { list.sumOf { it.amount } }
        Text("Total ₹$total · ${list.size} refunds", style = MaterialTheme.typography.titleSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(list.filter {
                q.isBlank() || it.id.contains(q, true) || it.email.contains(q, true) || it.contact.contains(q)
            }.take(300)) { r ->
                ElevatedCard {
                    Column(Modifier.padding(10.dp)) {
                        Text("₹${r.amount} · ${r.status}", style = MaterialTheme.typography.titleSmall)
                        Text("${r.id}\n${r.email} ${r.contact} · ${r.method}", style = MaterialTheme.typography.bodySmall)
                        TextButton({ receipt = r; rname = ""; upi = "" }) { Text("Success Receipt") }
                    }
                }
            }
        }
    }
    receipt?.let { r ->
        AlertDialog({ receipt = null }, confirmButton = {
            TextButton({ receipt = null }) { Text("Close") }
        }, title = { Text("Paytm-style Receipt") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("₹${r.amount} SUCCESS", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(rname, { rname = it }, { Text("Receiver Name") })
                OutlinedTextField(upi, { upi = it }, { Text("UPI ID") })
                Text("Name: $rname\nUPI: $upi\nTxn: ${r.id}\nStatus: ${r.status}")
            }
        })
    }
}
