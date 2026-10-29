package com.hydri.app

import com.hydri.app.domain.EntryTime
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class EntryTimeTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun changedTimeStaysOnOriginalDate() {
        val date = LocalDate.of(2026, 10, 20)
        for (time in listOf(LocalTime.MIDNIGHT, LocalTime.of(23, 59))) {
            val result = EntryTime.resolve(date, time, zone)!!
            assertEquals(date, Instant.ofEpochMilli(result).atZone(zone).toLocalDate())
        }
    }

    @Test
    fun dstGapCannotSilentlyMoveTime() {
        assertNull(EntryTime.resolve(LocalDate.of(2026, 3, 8), LocalTime.of(2, 30), zone))
    }

    @Test
    fun dstOverlapAmountOnlyEditPreservesExactInstant() {
        val original = Instant.parse("2026-11-01T06:30:00Z").toEpochMilli()
        assertEquals(
            original,
            EntryTime.resolve(LocalDate.of(2026, 11, 1), LocalTime.of(1, 30), zone, original),
        )
    }

    @Test
    fun skippedLocalDayCannotBecomeAnotherDay() {
        assertNull(
            EntryTime.resolve(LocalDate.of(2011, 12, 30), LocalTime.NOON, ZoneId.of("Pacific/Apia"))
        )
    }
}
