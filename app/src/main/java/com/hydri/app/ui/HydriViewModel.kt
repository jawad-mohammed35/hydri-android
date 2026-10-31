// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hydri.app.HydriApplication
import com.hydri.app.R
import com.hydri.app.background.*
import com.hydri.app.data.*
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class UiState(
    val settings: Settings = Settings(),
    val entries: List<WaterEntry> = emptyList(),
    val bottles: List<Bottle> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val hour: Int = LocalTime.now().hour,
    val ready: Boolean = false,
) {
    fun entriesOn(date: LocalDate) = entries.filter { it.date == date.toString() }

    fun total(date: LocalDate) = entriesOn(date).sumOf { it.volume }

    fun goal(date: LocalDate) = Repository.goalFor(goals, date)

    val statistics
        get() = Hydration.statistics(Repository.totals(entries, goals), today)
}

class HydriViewModel(application: Application) : AndroidViewModel(application) {
    val repo = (application as HydriApplication).repository
    private val clock = MutableStateFlow(LocalDateTime.now())
    val state =
        combine(
                repo.preferences.flow,
                repo.dao.entries(),
                repo.dao.bottles(),
                repo.dao.goals(),
                clock,
            ) { s, e, b, g, t ->
                UiState(s, e, b, g, t.toLocalDate(), t.hour, true)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())
    val messages = MutableSharedFlow<Pair<String, (() -> Unit)?>>(extraBufferCapacity = 10)
    private val actions = Mutex()
    private val recentLogs = mutableListOf<String>()

    init {
        viewModelScope.launch {
            while (isActive) {
                clock.value = LocalDateTime.now()
                delay(1000)
            }
        }
    }

    fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                actions.withLock { block() }
                HydriWidgets.update(getApplication())
            } catch (e: Exception) {
                messages.emit(
                    (if (e is UserMessage) e.message!!
                    else tr(R.string.that_action_could_not_be_completed)) to null
                )
            }
        }
    }

    fun settings(change: (Settings) -> Settings) = action {
        // Apply each control's change to the latest stored preferences, even before recomposition.
        val s = change(repo.preferences.flow.first())
        repo.preferences.save(s)
        Reminders.schedule(getApplication(), s)
    }

    fun log(
        volume: Long,
        name: String,
        date: LocalDate = LocalDate.now(),
        timestamp: Long = System.currentTimeMillis(),
        bottle: Bottle? = null,
    ) = action {
        val before = repo.dao.allEntries().filter { it.date == date.toString() }.sumOf { it.volume }
        val entry = repo.log(volume, name, date, timestamp, bottle)
        recentLogs.add(entry.id)
        if (
            date == LocalDate.now() &&
                before < state.value.goal(date) &&
                before + volume >= state.value.goal(date)
        ) {
            if (state.value.settings.goalNotification)
                Reminders.notify(
                    getApplication(),
                    tr(R.string.you_ve_reached_today_s_hydration_goal),
                    2,
                    state.value.settings,
                )
        }
    }

    fun delete(entry: WaterEntry) = action {
        repo.dao.deleteEntry(entry.id)
        messages.emit(tr(R.string.entry_deleted) to { action { repo.dao.putEntry(entry) } })
    }

    fun undo(entry: WaterEntry) = action { repo.dao.deleteEntry(entry.id) }

    fun undoToday() = action {
        val today = LocalDate.now().toString()
        val entries = repo.dao.allEntries().filter { it.date == today }
        val latest =
            recentLogs.asReversed().firstNotNullOfOrNull { id -> entries.find { it.id == id } }
                ?: entries.firstOrNull()
        if (latest != null) {
            repo.dao.deleteEntry(latest.id)
            recentLogs.remove(latest.id)
        }
    }

    fun resetToday() = action { repo.dao.clearEntriesOn(LocalDate.now().toString()) }

    fun goal(volume: Long) = action {
        repo.goal(volume)
        messages.emit(tr(R.string.daily_goal_updated) to null)
    }

    fun bottle(bottle: Bottle) = action { repo.dao.putBottle(bottle) }

    fun deleteBottle(bottle: Bottle) = action { repo.dao.deleteBottle(bottle.id) }

    fun move(bottle: Bottle, delta: Int) = action {
        val list = state.value.bottles.toMutableList()
        val i = list.indexOfFirst { it.id == bottle.id }
        val j = i + delta
        if (i >= 0 && j in list.indices) {
            val other = list[j]
            list[j] = list[i]
            list[i] = other
            repo.reorder(list)
        }
    }
}
