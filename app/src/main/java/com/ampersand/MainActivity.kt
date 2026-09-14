package com.ampersand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ampersand.ui.AmpersandApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.ampersand.auth.InviteCode.fromLink(intent?.dataString)?.let { com.ampersand.auth.PendingJoin.code = it }
        setContent { AmpersandApp() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        com.ampersand.auth.InviteCode.fromLink(intent.dataString)?.let { com.ampersand.auth.PendingJoin.code = it }
    }
}
