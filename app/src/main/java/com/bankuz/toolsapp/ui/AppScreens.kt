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
import com.bankuz.toolsapp.util.RazorpayKey
import com.bankuz.toolsapp.util.SecureStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginGate(ok: () -> Unit) {
    val ctx = LocalContext.current
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    val saved = remember { SecureStore.appPassword(ctx) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ToolsApp", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text(if (saved.isEmpty()) "Set app password" else "App password") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button({
            if (saved.isEmpty()) { SecureStore.setAppPassword(ctx, pass); ok() }
            else if (pass == saved) ok() else err = "Galat password"
        }, enabled = pass.length >= 4, modifier = Modifier.fillMaxWidth()) {
            Text(if (saved.isEmpty()) "Set & Unlock" else "Unlock")
        }
        if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun DashboardScreen(keys: List<RazorpayKey>, mod: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var pays by remember { mutableStateOf(emptyList<PaymentItem>()) }
    var bals by remember { mutableStateOf(mapOf<String, Pair<Double, Double>>()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    fun load() {
        if (keys.isEmpty()) { msg = "Settings me key add karo"; return }
        busy = true
        scope.launch {
            val fetched = coroutineScope {
                keys.map { k -> async { RazorpayClient.fetchPayments(k.keyId, k.keySecret, k.label) } }.awaitAll()
            }
            pays = fetched.flatten().sortedByDescending { it.createdAt }
            bals = coroutineScope {
                keys.map { k -> async { k.label to RazorpayClient.fetchBalance(k.keyId, k.keySecret) } }
                    .awaitAll().mapNotNull { (l, b) -> b?.let { l to it } }.toMap()
            }
            val today = pays.filter { System.currentTimeMillis() / 1000 - it.createdAt < 86400 }
            msg = "Today: ₹${today.filter { it.status == "captured" }.sumOf { it.amount }} (${today.size}) · Total ${pays.size}"
            busy = false
        }
    }
    LaunchedEffect(keys) { if (pays.isEmpty()) load() }
    Column(mod.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            keys.forEach { k ->
                val b = bals[k.label]
                ElevatedCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(k.label, style = MaterialTheme.typography.labelMedium)
                        Text(if (b == null) "₹—" else "₹${b.first}",
                            style = MaterialTheme.typography.headlineSmall)
                        if (b != null) Text("locked ₹${b.second}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ load() }, enabled = !busy) { Text(if (busy) "..." else "Refresh") }
            if (msg.isNotEmpty()) Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
        }
        Text("Recent", style = MaterialTheme.typography.titleSmall)
        var sel by remember { mutableStateOf<PaymentItem?>(null) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(pays.take(30)) { p ->
                ElevatedCard(onClick = { sel = p }) {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("[${p.key}] ₹${p.amount}", style = MaterialTheme.typography.titleSmall)
                            Text("${p.id} · ${p.email}", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusChip(p.status)
                    }
                }
            }
        }
        sel?.let { p ->
            AlertDialog({ sel = null }, confirmButton = { TextButton({ sel = null }) { Text("Close") } },
                title = { Text("₹${p.amount} · ${p.status}") },
                text = { Text("${p.id}\n${p.email} ${p.contact}\n${p.method} · ${p.key}") })
        }
    }
}

@Composable
fun PaymentsScreen(keys: List<RazorpayKey>, mod: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var pays by remember { mutableStateOf(emptyList<PaymentItem>()) }
    var busy by remember { mutableStateOf(false) }
    var pid by remember { mutableStateOf("") }
    var amt by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val first = keys.firstOrNull()

    fun load() {
        busy = true
        scope.launch {
            pays = coroutineScope {
                keys.map { k -> async { RazorpayClient.fetchPayments(k.keyId, k.keySecret, k.label, 50) } }
                    .awaitAll().flatten().sortedByDescending { it.createdAt }
            }
            busy = false
        }
    }
    LaunchedEffect(keys) { if (pays.isEmpty() && keys.isNotEmpty()) load() }
    Column(mod.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(q, { q = it }, label = { Text("Search id / email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "UG", "PG", "TAX").forEach { f ->
                FilterChip(f == filter, { filter = f }, label = { Text(f) })
            }
            Spacer(Modifier.weight(1f))
            TextButton({ load() }, enabled = !busy) { Text(if (busy) "..." else "Reload") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("captured", "failed", "authorized").forEach { s ->
                FilterChip(filter == s, { filter = if (filter == s) "All" else s }, label = { Text(s) })
            }
        }
        var sel by remember { mutableStateOf<PaymentItem?>(null) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(pays.filter {
                (filter == "All" || it.key.equals(filter, true) || it.status.equals(filter, true)) &&
                    (q.isBlank() || it.id.contains(q, true) || it.email.contains(q, true) || it.contact.contains(q))
            }.take(200)) { p ->
                ElevatedCard(onClick = { sel = p; pid = p.id }) {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("[${p.key}] ₹${p.amount}", style = MaterialTheme.typography.titleSmall)
                            Text("${p.id}\n${p.email} ${p.contact}", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusChip(p.status)
                    }
                }
            }
        }
        sel?.let { p ->
            AlertDialog({ sel = null }, confirmButton = { TextButton({ sel = null }) { Text("Close") } },
                dismissButton = {
                    TextButton({
                        scope.launch {
                            msg = try { RazorpayClient.createRefund(first!!.keyId, first.keySecret, p.id, amt) }
                            catch (e: Exception) { e.message ?: "fail" }
                        }
                    }, enabled = first != null) { Text("Refund") }
                },
                title = { Text("₹${p.amount} · ${p.status}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${p.id}\n${p.email} ${p.contact}\n${p.method}")
                        OutlinedTextField(amt, { amt = it }, label = { Text("Refund ₹ (blank=full)") }, singleLine = true)
                        if (msg.isNotEmpty()) Text(msg, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(pid, { pid = it }, label = { Text("Payment ID") }, singleLine = true)
                    }
                })
        }
    }
}

@Composable
fun LinksScreen(keys: List<RazorpayKey>, mod: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val first = keys.firstOrNull()
    var links by remember { mutableStateOf(emptyList<RazorpayClient.PayLink>()) }
    var amt by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    fun load() {
        val k = first ?: return
        busy = true
        scope.launch { links = RazorpayClient.fetchLinks(k.keyId, k.keySecret); busy = false }
    }
    LaunchedEffect(first) { if (links.isEmpty()) load() }
    Column(mod.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Payment Links (${first?.label ?: "no key"})", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(amt, { amt = it }, label = { Text("Amount ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({
                busy = true
                scope.launch {
                    msg = try { RazorpayClient.createLink(first!!.keyId, first.keySecret, amt.toDoubleOrNull() ?: 0.0, desc) }
                    catch (e: Exception) { e.message ?: "fail" }
                    busy = false; load()
                }
            }, enabled = !busy && first != null && amt.toDoubleOrNull() != null) { Text("Create") }
            TextButton({ load() }, enabled = !busy) { Text("Reload") }
        }
        if (msg.isNotEmpty()) Text(msg.take(300), style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(links) { l ->
                ElevatedCard {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("₹${l.amount} · ${l.id}", style = MaterialTheme.typography.titleSmall)
                            Text("${l.desc}\n${l.shortUrl}", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusChip(l.status)
                    }
                }
            }
        }
    }
}

@Composable
fun KeysScreen(keys: List<RazorpayKey>, onSave: (List<RazorpayKey>) -> Unit, mod: Modifier = Modifier) {
    val ctx = LocalContext.current
    var label by remember { mutableStateOf("UG") }
    var kid by remember { mutableStateOf("") }
    var sec by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf(SecureStore.appPassword(ctx)) }
    Column(mod.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Razorpay Keys (encrypted on device)", style = MaterialTheme.typography.titleSmall)
        keys.forEach { k ->
            ElevatedCard {
                Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("[${k.label}] ${k.keyId.take(12)}…")
                    TextButton({ onSave(keys - k) }) { Text("Delete") }
                }
            }
        }
        OutlinedTextField(label, { label = it }, label = { Text("Label (UG/PG/TAX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(kid, { kid = it }, label = { Text("Key ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(sec, { sec = it }, label = { Text("Key Secret") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({
            onSave(keys + RazorpayKey(label.trim().ifEmpty { "KEY" }, kid.trim(), sec.trim()))
            label = ""; kid = ""; sec = ""
        }, enabled = kid.isNotBlank() && sec.isNotBlank()) { Text("Add Key") }
        OutlinedTextField(pass, { pass = it }, label = { Text("App password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({ SecureStore.setAppPassword(ctx, pass) }, enabled = pass.length >= 4) { Text("Save Password") }
    }
}
