// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import android.app.Application
import android.content.res.Configuration
import androidx.room.Room
import com.hydri.app.background.HydriWidgets
import com.hydri.app.data.*
import kotlinx.coroutines.*

class HydriApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository by lazy {
        Repository(
            Room.databaseBuilder(this, HydriDatabase::class.java, "hydri.db")
                .addMigrations(HydriDatabase.MIGRATION_1_2)
                .build(),
            PreferenceStore(this),
        )
    }

    override fun onCreate() {
        super.onCreate()
        com.hydri.app.i18n.AppText.refresh(this)
        LauncherShortcuts.publish(this)
        com.hydri.app.background.Reminders.refreshLabels(this)
        scope.launch { repository.initialize() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        com.hydri.app.i18n.AppText.refresh(this)
        LauncherShortcuts.publish(this)
        scope.launch { HydriWidgets.update(this@HydriApplication) }
    }
}
