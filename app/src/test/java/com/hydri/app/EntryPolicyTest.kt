// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import com.hydri.app.data.WaterEntry
import com.hydri.app.domain.EntryPolicy
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class EntryPolicyTest {
    private fun entry(date: LocalDate) =
        WaterEntry(date.toString(), 250_000, 0, date.toString(), "Fixture", "UTC")

    @Test
    fun historyIsAnInclusiveSevenCalendarDayWindowAndDoesNotMutateRecords() {
        val today = LocalDate.of(2028, 3, 2)
        val entries = (-1L..40L).map { entry(today.minusDays(it)) }
        val snapshot = entries.toList()
        val dates = EntryPolicy.recentHistory(entries, today).keys.toList()
        assertEquals((0L..6L).map { today.minusDays(it).toString() }, dates)
        assertEquals(snapshot, entries)
        assertEquals(42, entries.size)
    }

    @Test
    fun emptyDaysAreNotInventedAndMidnightMovesTheWindow() {
        val today = LocalDate.of(2026, 12, 31)
        val entries = listOf(entry(today.minusDays(6)), entry(today), entry(today.minusDays(20)))
        assertEquals(2, EntryPolicy.recentHistory(entries, today).size)
        assertEquals(
            listOf(today.toString()),
            EntryPolicy.recentHistory(entries, today.plusDays(1)).keys.toList(),
        )
        assertTrue(EntryPolicy.canModify(today.toString(), today))
        assertFalse(EntryPolicy.canModify(today.toString(), today.plusDays(1)))
        assertFalse(EntryPolicy.canModify(today.plusDays(1).toString(), today))
    }

    @Test
    fun calendarDaysDoNotMeanThePrevious168Hours() {
        val before = ZonedDateTime.of(2026, 3, 8, 23, 59, 59, 0, ZoneId.of("America/New_York"))
        val entries = (0L..7L).map { entry(before.toLocalDate().minusDays(it)) }
        assertEquals(7, EntryPolicy.recentHistory(entries, before.toLocalDate()).size)
        assertEquals(
            6,
            EntryPolicy.recentHistory(entries, before.plusSeconds(2).toLocalDate()).size,
        )
    }
}
