package com.hydri.app

import com.hydri.app.data.*
import com.hydri.app.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class HydrationTest {
    private val today = LocalDate.of(2026, 10, 27)

    @Test
    fun ounceConversionsPreservePrecision() {
        assertEquals(29574L, Volume.parse("1", "oz"))
        assertEquals(500_000L, Volume.parse("0.5", "L"))
        assertEquals(250_500L, Volume.parse("250.5", "mL"))
        val stored = Volume.parse("16", "oz")!!
        assertEquals(473176L, stored)
        assertTrue(kotlin.math.abs(stored / Volume.OZ - 16) < 0.00002)
    }

    @Test
    fun invalidAmountsRejected() {
        listOf("", "NaN", "Infinity", "-1", "0", "20001", "1e20").forEach {
            assertNull(it, Volume.parse(it, "mL"))
        }
    }

    @Test
    fun historicalGoalsAreStable() {
        val goals = listOf(Goal("2026-10-01", 2_000_000), Goal("2026-10-27", 2_500_000))
        assertEquals(2_000_000L, Repository.goalFor(goals, today.minusDays(1)))
        assertEquals(2_500_000L, Repository.goalFor(goals, today))
    }

    @Test
    fun currentStreakAllowsUnfinishedToday() {
        val days = (1..7).map { DayTotal(today.minusDays(it.toLong()), 2_000_000, 2_000_000) }
        val stats = Hydration.statistics(days, today)
        assertEquals(7, stats.current)
        assertEquals(7, stats.longest)
        assertEquals(today.minusDays(1), stats.badges[2].unlocked)
        assertNull(stats.badges[3].unlocked)
    }

    @Test
    fun editsRevokeInvalidAchievements() {
        val days =
            (0..6).map {
                DayTotal(
                    today.minusDays(it.toLong()),
                    if (it == 3) 500_000 else 2_000_000,
                    2_000_000,
                )
            }
        val stats = Hydration.statistics(days, today)
        assertEquals(3, stats.current)
        assertEquals(3, stats.longest)
        assertNull(stats.badges[2].unlocked)
    }

    @Test
    fun missingCalendarDayBreaksStreak() {
        val days =
            listOf(0L, 1L, 3L, 4L).map { DayTotal(today.minusDays(it), 2_000_000, 2_000_000) }
        assertEquals(2, Hydration.statistics(days, today).longest)
    }

    @Test
    fun exceedingGoalDoesNotEarnExtraStreakCredit() {
        val stats = Hydration.statistics(listOf(DayTotal(today, 20_000_000, 2_000_000)), today)
        assertEquals(1, stats.current)
        assertEquals(1, stats.completed)
        assertNull(stats.badges[1].unlocked)
    }

    @Test
    fun reminderWindowsCrossMidnight() {
        assertTrue(Hydration.inWindow(30, 1320, 420))
        assertFalse(Hydration.inWindow(720, 1320, 420))
        assertTrue(Hydration.inWindow(600, 480, 1260))
        assertFalse(Hydration.inWindow(1260, 480, 1260))
        assertTrue(Hydration.inWindow(600, 0, 0))
    }

    @Test
    fun quietHoursAndWeekdaysTakePriority() {
        val now = ZonedDateTime.of(2026, 10, 27, 23, 0, 0, 0, ZoneId.of("America/New_York"))
        assertFalse(Hydration.reminderAllowed(now, (1..7).toSet(), 0, 0, true, 1320, 420))
        assertFalse(Hydration.reminderAllowed(now, setOf(1), 0, 0, false, 1320, 420))
        assertTrue(Hydration.reminderAllowed(now, setOf(2), 0, 0, false, 1320, 420))
    }

    @Test
    fun dstDoesNotCreatePhantomStreakDays() {
        val zone = ZoneId.of("America/New_York")
        val before = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, zone)
        val after = before.plusDays(1)
        assertEquals(23, Duration.between(before, after).toHours())
        val stats =
            Hydration.statistics(
                listOf(before, after).map { DayTotal(it.toLocalDate(), 2_000_000, 2_000_000) },
                after.toLocalDate(),
            )
        assertEquals(2, stats.current)
    }

    @Test
    fun futureRecordsNeverUnlockBadges() {
        assertEquals(
            0,
            Hydration.statistics(listOf(DayTotal(today.plusDays(1), 2_000_000, 2_000_000)), today)
                .completed,
        )
    }
}
