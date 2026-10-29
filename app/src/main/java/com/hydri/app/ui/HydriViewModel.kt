package com.hydri.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hydri.app.HydriApplication
import com.hydri.app.background.*
import com.hydri.app.data.*
import com.hydri.app.domain.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
    val messages = MutableSharedFlow<Pair<String, WaterEntry?>>(extraBufferCapacity = 10)

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
                block()
                HydriWidgets.update(getApplication())
            } catch (e: Exception) {
                messages.emit((e.message ?: "That action could not be completed.") to null)
            }
        }
    }

    fun settings(s: Settings) = action {
        repo.preferences.save(s)
        Reminders.schedule(getApplication(), s)
    }

    fun log(
        volume: Long,
        name: String,
        date: LocalDate = state.value.today,
        timestamp: Long = System.currentTimeMillis(),
    ) = action {
        val before = repo.dao.allEntries().filter { it.date == date.toString() }.sumOf { it.volume }
        val entry = repo.log(volume, name, date, timestamp)
        messages.emit("Added ${Volume.format(volume,state.value.settings.unit)}" to entry)
        if (
            date == LocalDate.now() &&
                before < state.value.goal(date) &&
                before + volume >= state.value.goal(date)
        ) {
            if (state.value.settings.goalNotification)
                Reminders.notify(
                    getApplication(),
                    "You've reached today's hydration goal!",
                    2,
                    state.value.settings,
                )
        }
    }

    fun delete(entry: WaterEntry) = action { repo.dao.deleteEntry(entry.id) }

    fun undo(entry: WaterEntry) = delete(entry)

    fun goal(volume: Long) = action {
        repo.goal(volume)
        messages.emit("Daily goal updated" to null)
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
