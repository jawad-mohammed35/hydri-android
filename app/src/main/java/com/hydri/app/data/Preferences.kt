package com.hydri.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.hydriPreferences by preferencesDataStore("hydri_preferences")

data class Settings(
    val onboarded: Boolean = false,
    val name: String = "",
    val unit: String = "mL",
    val theme: String = "System",
    val oled: Boolean = false,
    val accent: Int = 0,
    val reducedMotion: Boolean = false,
    val haptics: Boolean = true,
    val dateFormat: String = "System",
    val timeFormat: String = "System",
    val reminders: Boolean = false,
    val frequency: Int = 120,
    val start: Int = 480,
    val end: Int = 1260,
    val quiet: Boolean = true,
    val quietStart: Int = 1320,
    val quietEnd: Int = 420,
    val days: Set<Int> = (1..7).toSet(),
    val stopAtGoal: Boolean = true,
    val goalNotification: Boolean = true,
    val evening: Boolean = false,
    val sound: Boolean = true,
    val vibration: Boolean = true,
) {
    fun json(): JSONObject =
        JSONObject().apply {
            put("onboarded", onboarded)
            put("name", name)
            put("unit", unit)
            put("theme", theme)
            put("oled", oled)
            put("accent", accent)
            put("reducedMotion", reducedMotion)
            put("haptics", haptics)
            put("dateFormat", dateFormat)
            put("timeFormat", timeFormat)
            put("reminders", reminders)
            put("frequency", frequency)
            put("start", start)
            put("end", end)
            put("quiet", quiet)
            put("quietStart", quietStart)
            put("quietEnd", quietEnd)
            put("days", days.sorted().joinToString(","))
            put("stopAtGoal", stopAtGoal)
            put("goalNotification", goalNotification)
            put("evening", evening)
            put("sound", sound)
            put("vibration", vibration)
        }

    companion object {
        fun from(j: JSONObject) =
            Settings(
                    j.optBoolean("onboarded"),
                    j.optString("name", "").take(60),
                    j.optString("unit", "mL"),
                    j.optString("theme", "System"),
                    j.optBoolean("oled"),
                    j.optInt("accent"),
                    j.optBoolean("reducedMotion"),
                    j.optBoolean("haptics", true),
                    j.optString("dateFormat", "System"),
                    j.optString("timeFormat", "System"),
                    j.optBoolean("reminders"),
                    j.optInt("frequency", 120),
                    j.optInt("start", 480),
                    j.optInt("end", 1260),
                    j.optBoolean("quiet", true),
                    j.optInt("quietStart", 1320),
                    j.optInt("quietEnd", 420),
                    j.optString("days", "1,2,3,4,5,6,7")
                        .split(",")
                        .filter { it.isNotBlank() }
                        .map { it.toInt() }
                        .toSet(),
                    j.optBoolean("stopAtGoal", true),
                    j.optBoolean("goalNotification", true),
                    j.optBoolean("evening"),
                    j.optBoolean("sound", true),
                    j.optBoolean("vibration", true),
                )
                .also { s ->
                    require(
                        s.unit in listOf("mL", "L", "oz") &&
                            s.theme in listOf("System", "Light", "Dark") &&
                            s.accent in 0..5
                    )
                    require(
                        s.frequency in listOf(30, 60, 90, 120, 180, 240) &&
                            listOf(s.start, s.end, s.quietStart, s.quietEnd).all { it in 0..1439 }
                    )
                    require(
                        s.days.all { it in 1..7 } &&
                            s.dateFormat in
                                listOf("System", "MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD") &&
                            s.timeFormat in listOf("System", "12-hour", "24-hour")
                    )
                }
    }
}

class PreferenceStore(private val context: Context) {
    private val key = stringPreferencesKey("settings")
    val flow =
        context.hydriPreferences.data.map { p ->
            p[key]?.let { Settings.from(JSONObject(it)) } ?: Settings()
        }

    suspend fun save(settings: Settings) {
        context.hydriPreferences.edit { it[key] = settings.json().toString() }
    }
}
