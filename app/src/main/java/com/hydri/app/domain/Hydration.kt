// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.domain

import com.hydri.app.R
import com.hydri.app.i18n.*
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
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

    fun parse(text: String, unit: String, locale: Locale = AppText.locale): Long? {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        var normalized =
            text
                .trim()
                .map { if (it.isDigit()) Character.digit(it, 10).toString() else it.toString() }
                .joinToString("")
        val grouping = symbols.groupingSeparator
        // Only strip well-formed thousands groups; never silently concatenate arbitrary input.
        if (grouping != symbols.decimalSeparator && normalized.contains(grouping)) {
            val whole = normalized.substringBefore(symbols.decimalSeparator)
            val groupPattern = "\\d{1,3}(" + Regex.escape(grouping.toString()) + "\\d{3})+"
            if (whole.matches(Regex(groupPattern)))
                normalized = normalized.replace(grouping.toString(), "")
        }
        normalized = normalized.replace(symbols.decimalSeparator, '.').replace(',', '.')
        if (!normalized.matches(Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))) return null
        val number = normalized.toDoubleOrNull() ?: return null
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

    fun input(value: Long, unit: String, locale: Locale = AppText.locale): String =
        NumberFormat.getNumberInstance(locale)
            .apply {
                isGroupingUsed = false
                minimumFractionDigits = if (unit == "mL") 0 else 2
                maximumFractionDigits = minimumFractionDigits
            }
            .format(value / factor(unit))

    fun factor(unit: String) =
        when (unit) {
            "oz" -> OZ
            "L" -> 1_000_000.0
            else -> 1000.0
        }

    fun format(value: Long, unit: String): String =
        String.format(
            AppText.locale,
            when (unit) {
                "mL" -> "%,.0f"
                "L" -> "%.2f"
                else -> "%.1f"
            },
            value / factor(unit),
        ) + " " + unitLabel(unit)
}

data class DayTotal(val date: LocalDate, val volume: Long, val goal: Long) {
    val complete
        get() = volume >= goal

    val percent
        get() = (volume.toDouble() / goal * 100).toInt()
}

data class Badge(val name: Int, val days: Int, val unlocked: LocalDate?)

data class Statistics(
    val current: Int,
    val longest: Int,
    val completed: Int,
    val badges: List<Badge>,
)

object Hydration {
    val milestones =
        listOf(
            R.string.first_drop to 1,
            R.string.getting_started to 3,
            R.string.one_week_strong to 7,
            R.string.hydration_champion to 14,
            R.string.monthly_master to 30,
            R.string.consistency_legend to 100,
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
        else ->
            date.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(AppText.locale)
            )
    }

fun formattedTime(
    timestamp: Long,
    format: String,
    system24: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern(timePattern(format, system24), AppText.locale))

fun timePattern(format: String, system24: Boolean): String {
    val is24 = format == "24-hour" || (format == "System" && system24)
    return if (AppText.context == null) {
        if (is24) "HH:mm" else "h:mm a"
    } else
        android.text.format.DateFormat.getBestDateTimePattern(
            AppText.locale,
            if (is24) "Hm" else "hm",
        )
}

fun formattedMonth(month: YearMonth): String =
    month.format(
        DateTimeFormatter.ofPattern(
            if (AppText.context == null) "MMMM yyyy"
            else android.text.format.DateFormat.getBestDateTimePattern(AppText.locale, "yMMMM"),
            AppText.locale,
        )
    )
