// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import com.hydri.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class LoggingAndWidgetTest {
    @Test
    fun customNamesPreserveUnicodeAndOnlyTrimEdges() {
        assertEquals("朝の水 💧  물", CustomEntryName.stored("  朝の水 💧  물  ", "Custom water"))
        assertEquals("x".repeat(150), CustomEntryName.normalize("x".repeat(150)))
    }

    @Test
    fun defaultsStayLocalizableAndBlankNamesFallBack() {
        listOf("Custom water", "Agua personalizada", "自定义饮水", "물", "水", "Air").forEach {
            assertEquals(CustomEntryName.DEFAULT, CustomEntryName.stored(it, it))
            assertEquals(CustomEntryName.DEFAULT, CustomEntryName.stored(" \t ", it))
        }
    }

    @Test
    fun superchargeIsContinuousMonotonicAndBounded() {
        val goal = 2_000_000L
        assertEquals(0f, Supercharge.intensity(0, goal), 0f)
        assertEquals(0f, Supercharge.intensity(goal, goal), 0f)
        var before = 0f
        for (percent in 101..200) {
            val value = Supercharge.intensity(goal * percent / 100, goal)
            assertTrue(value > before)
            before = value
        }
        assertEquals(.25f, Supercharge.intensity(goal * 125 / 100, goal), .0001f)
        assertEquals(1f, Supercharge.intensity(Long.MAX_VALUE, goal), 0f)
        assertEquals(0f, Supercharge.intensity(goal, 0), 0f)
    }

    @Test
    fun widgetTypesRetainDistinctPurposeAndRespondToBothAxes() {
        for (w in listOf(100f, 160f, 180f, 240f, 275f, 320f, 360f)) for (h in
            listOf(80f, 120f, 200f, 340f)) {
            val progress = WidgetLayout.forSize(w, h, false)
            val quick = WidgetLayout.forSize(w, h, true)
            assertTrue(progress.buttonCount in 0..2)
            assertTrue(quick.buttonCount in 0..4)
            assertTrue(progress.ringSize <= w * if (progress.verticalRing) .72f else .38f)
            if (w >= 150 && h >= 170) assertTrue(quick.buttonCount > 0)
        }
        assertNotEquals(
            WidgetLayout.forSize(180f, 120f, true),
            WidgetLayout.forSize(180f, 340f, true),
        )
        assertNotEquals(
            WidgetLayout.forSize(180f, 200f, true),
            WidgetLayout.forSize(320f, 200f, true),
        )
    }
}
