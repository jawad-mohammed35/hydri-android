// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.domain

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Integer microlitres keep unit changes lossless. Logged dates stay in their original local day.
 */
object Volume {
    const val OZ = 29573.5295625

    fun parse(text: String, unit: String): Long? {
        val number = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (!number.isFinite() || number <= 0) return null
        val value =
            number *
                when (unit) {
                    "oz" -> OZ
                    "L" -> 1_000_000.0
                    else -> 1000.0
                }
        return if (value in 1_000.0..20_000_000.0) value.roundToLong() else null
    }

    fun input(value: Long, unit: String) =
        String.format(Locale.US, if (unit == "mL") "%.0f" else "%.2f", value / factor(unit))

    fun factor(unit: String) =
        when (unit) {
            "oz" -> OZ
            "L" -> 1_000_000.0
            else -> 1000.0
        }

    fun format(value: Long, unit: String): String =
        String.format(
            Locale.getDefault(),
            when (unit) {
                "mL" -> "%,.0f"
                "L" -> "%.2f"
                else -> "%.1f"
            },
            value / factor(unit),
        ) + " " + unit
}

data class DayTotal(val date: LocalDate, val volume: Long, val goal: Long) {
    val complete
        get() = volume >= goal

    val percent
        get() = (volume.toDouble() / goal * 100).toInt()
}

data class Badge(val name: String, val days: Int, val unlocked: LocalDate?)

data class Statistics(
    val current: Int,
    val longest: Int,
    val completed: Int,
    val badges: List<Badge>,
)

object Hydration {
    val milestones =
        listOf(
            "First Drop" to 1,
            "Getting Started" to 3,
            "One Week Strong" to 7,
            "Hydration Champion" to 14,
            "Monthly Master" to 30,
            "Consistency Legend" to 100,
        )

    fun statistics(days: List<DayTotal>, today: LocalDate): Statistics {
        val completed = days.filter { it.date <= today && it.complete }.map { it.date }.toSet()
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        val unlocks = mutableMapOf<Int, LocalDate>()
        for (date in completed.sorted()) {
            run = if (previous == date.minusDays(1)) run + 1 else 1
            longest = maxOf(longest, run)
            previous = date
            milestones.forEach { (_, count) -> if (run >= count) unlocks.putIfAbsent(count, date) }
        }
        var cursor = if (today in completed) today else today.minusDays(1)
        var current = 0
        while (cursor in completed) {
            current++
            cursor = cursor.minusDays(1)
        }
        return Statistics(
            current,
            longest,
            completed.size,
            milestones.map { Badge(it.first, it.second, unlocks[it.second]) },
        )
    }

    fun inWindow(minute: Int, start: Int, end: Int): Boolean =
        when {
            start == end -> true
            start < end -> minute >= start && minute < end
            else -> minute >= start || minute < end
        }

    fun reminderAllowed(
        now: ZonedDateTime,
        days: Set<Int>,
        start: Int,
        end: Int,
        quiet: Boolean,
        quietStart: Int,
        quietEnd: Int,
    ): Boolean {
        val minute = now.hour * 60 + now.minute
        return now.dayOfWeek.value in days &&
            inWindow(minute, start, end) &&
            (!quiet || !inWindow(minute, quietStart, quietEnd))
    }
}

fun formattedDate(date: LocalDate, format: String): String =
    when (format) {
        "MM/DD/YYYY" -> date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        "DD/MM/YYYY" -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        "YYYY-MM-DD" -> date.toString()
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

fun formattedTime(
    timestamp: Long,
    format: String,
    system24: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(zone)
        .format(
            DateTimeFormatter.ofPattern(
                if (format == "24-hour" || (format == "System" && system24)) "HH:mm" else "h:mm a"
            )
        )
