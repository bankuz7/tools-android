package com.bankuz.toolsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bankuz.toolsapp.ui.ManagerScreen
import com.bankuz.toolsapp.ui.PasswordResetScreen
import com.bankuz.toolsapp.ui.RefundHistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var tab by remember { mutableIntStateOf(0) }
                Scaffold(bottomBar = {
                    NavigationBar {
                        NavigationBarItem(tab == 0, { tab = 0 }, { Text("Refunds") }, icon = { Icon(Icons.Default.History, null) })
                        NavigationBarItem(tab == 1, { tab = 1 }, { Text("Manager") }, icon = { Icon(Icons.Default.Dashboard, null) })
                        NavigationBarItem(tab == 2, { tab = 2 }, { Text("Reset") }, icon = { Icon(Icons.Default.LockReset, null) })
                    }
                }) { p ->
                    when (tab) {
                        0 -> RefundHistoryScreen(Modifier.padding(p))
                        1 -> ManagerScreen(Modifier.padding(p))
                        else -> PasswordResetScreen(Modifier.padding(p))
                    }
                }
            }
        }
    }
}
