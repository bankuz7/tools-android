package com.bankuz.toolsapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bankuz.toolsapp.data.CollegeClient
import kotlinx.coroutines.launch

@Composable
fun PasswordResetScreen(mod: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var portal by remember { mutableStateOf("vanraj") }
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(mod.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Password Reset — Vanraj / JPPACC", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(portal == "vanraj", { portal = "vanraj" }, { Text("Vanraj") })
            FilterChip(portal == "jppacc", { portal = "jppacc" }, { Text("JPPACC") })
        }
        OutlinedTextField(email, { email = it }, { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass, { pass = it }, { Text("New password (min 6)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({
            busy = true; msg = ""
            scope.launch {
                val f = CollegeClient.findToken(email.trim(), portal)
                if (f.isSuccess) { token = f.getOrNull() ?: ""; msg = "Token mil gaya, resetting..."
                    val r = CollegeClient.reset(email.trim(), token, pass.trim(), portal)
                    ok = r.isSuccess; msg = r.getOrNull() ?: r.exceptionOrNull()?.message ?: "fail"
                } else { ok = false; msg = f.exceptionOrNull()?.message ?: "fail" }
                busy = false
            }
        }, enabled = !busy && email.isNotBlank() && pass.length >= 6, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "..." else "One-Click Reset")
        }
        OutlinedTextField(token, { token = it }, { Text("Token (advanced)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (msg.isNotEmpty()) Text(msg, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
}
