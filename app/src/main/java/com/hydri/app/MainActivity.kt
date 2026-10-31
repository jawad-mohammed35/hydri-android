// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.hydri.app.ui.HydriApp
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var shortcut by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.hydri.app.i18n.AppText.refresh(this)
        com.hydri.app.background.Reminders.refreshLabels(this)
        LauncherShortcuts.publish(this)
        (application as HydriApplication).scope.launch {
            com.hydri.app.background.HydriWidgets.update(applicationContext)
        }
        shortcut =
            LauncherShortcuts.action(
                if (savedInstanceState != null) savedInstanceState.getString("pending-shortcut")
                else intent.action.takeUnless { intent.getBooleanExtra("shortcut-consumed", false) }
            )
        enableEdgeToEdge()
        setContent {
            HydriApp(
                shortcut = shortcut,
                onShortcutHandled = {
                    shortcut = null
                    intent.putExtra("shortcut-consumed", true)
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcut = LauncherShortcuts.action(intent.action)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending-shortcut", shortcut)
        super.onSaveInstanceState(outState)
    }
}
