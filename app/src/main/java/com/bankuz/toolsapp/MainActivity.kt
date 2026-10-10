package com.bankuz.toolsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bankuz.toolsapp.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val ctx = LocalContext.current
                var authed by remember { mutableStateOf(false) }
                if (!authed) { LoginGate { authed = true }; return@AppTheme }
                var tab by remember { mutableIntStateOf(0) }
                var keys by remember { mutableStateOf(com.bankuz.toolsapp.util.SecureStore.getKeys(ctx)) }
                Scaffold(bottomBar = {
                    NavigationBar {
                        val items = listOf("Home" to Icons.Default.Dashboard, "Payments" to Icons.Default.Receipt,
                            "Refunds" to Icons.Default.History, "Links" to Icons.Default.Link, "Keys" to Icons.Default.Settings)
                        items.forEachIndexed { i, (l, ic) ->
                            NavigationBarItem(tab == i, { tab = i }, icon = { Icon(ic, null) }, label = { Text(l) })
                        }
                    }
                }) { p ->
                    val mod = Modifier.padding(p)
                    when (tab) {
                        0 -> DashboardScreen(keys, mod)
                        1 -> PaymentsScreen(keys, mod)
                        2 -> com.bankuz.toolsapp.ui.RefundHistoryScreen(mod)
                        3 -> LinksScreen(keys, mod)
                        else -> KeysScreen(keys, { keys = it; com.bankuz.toolsapp.util.SecureStore.saveKeys(ctx, it) }, mod)
                    }
                }
            }
        }
    }
}
