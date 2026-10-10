package com.bankuz.toolsapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Dark = darkColorScheme(
    primary = Color(0xFF7C9CFF), secondary = Color(0xFF4ADE80),
    background = Color(0xFF0B0E14), surface = Color(0xFF131926),
    surfaceVariant = Color(0xFF1B2333), error = Color(0xFFF87171),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}

@Composable
fun StatusChip(status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "captured", "paid", "processed", "success" ->
            Color(0xFF166534) to Color(0xFF4ADE80)
        "failed" -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        else -> Color(0xFF713F12) to Color(0xFFFACC15)
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(status, color = fg, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}
