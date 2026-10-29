package com.hydri.app

import android.app.Application
import androidx.room.Room
import com.hydri.app.data.*
import kotlinx.coroutines.*

class HydriApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository by lazy {
        Repository(
            Room.databaseBuilder(this, HydriDatabase::class.java, "hydri.db").build(),
            PreferenceStore(this),
        )
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch { repository.initialize() }
    }
}
