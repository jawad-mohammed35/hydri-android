// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import com.hydri.app.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TrackerRangesTest {
    @Test
    fun removedDayRangeFallsBackToWeek() {
        assertEquals("Week", TrackerRanges.period("Day"))
        assertEquals("Week", TrackerRanges.period("invalid"))
        assertEquals("Week", TrackerRanges.period("Week"))
        assertEquals("Month", TrackerRanges.period("Month"))
    }

    @Test
    fun weekUsesSevenCalendarDaysAcrossYearsAndIgnoresBrowsedMonth() {
        val today = LocalDate.of(2027, 1, 2)
        val expected = (6L downTo 0).map(today::minusDays)
        assertEquals(expected, TrackerRanges.dates("Week", YearMonth.of(2025, 8), today))
        assertEquals(expected, TrackerRanges.dates("Day", YearMonth.from(today), today))
    }

    @Test
    fun monthIncludesLeapDayButOmitsFutureDays() {
        val month = YearMonth.of(2028, 2)
        assertEquals(29, TrackerRanges.dates("Month", month, LocalDate.of(2028, 3, 1)).size)
        assertEquals(
            (1..14).map(month::atDay),
            TrackerRanges.dates("Month", month, month.atDay(14)),
        )
        assertTrue(TrackerRanges.dates("Month", YearMonth.of(2028, 4), month.atDay(14)).isEmpty())
    }

    @Test
    fun calendarAdvancesOnlyOneMonthAndRespectsCurrentMonthLimit() {
        val today = LocalDate.of(2027, 1, 2)
        val current = YearMonth.from(today)
        assertEquals(current, TrackerRanges.stepMonth(current, 1, today))
        assertEquals(YearMonth.of(2026, 12), TrackerRanges.stepMonth(current, -1, today))
        assertEquals(current, TrackerRanges.stepMonth(YearMonth.of(2026, 12), 1, today))
    }

    @Test
    fun shapeIdsKeepTheirLegacyMeaningAndAppendWithoutCollisions() {
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6),
            listOf(
                    BottleShape.GLASS,
                    BottleShape.BOTTLE,
                    BottleShape.LARGE_BOTTLE,
                    BottleShape.TUMBLER,
                    BottleShape.SPORTS_BOTTLE,
                    BottleShape.MUG,
                    BottleShape.CUSTOM,
                )
                .map { it.id },
        )
        assertEquals((0..13).toList(), BottleShape.entries.map { it.id })
        (0..13).forEach { assertTrue(BottleShape.isValid(it)) }
        assertFalse(BottleShape.isValid(-1))
        assertFalse(BottleShape.isValid(14))
    }
}
