// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.ui.*
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import org.junit.*
import org.junit.Assert.*

class TrackerRefinementDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun isolated() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
    }

    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent(content = content) }
        rule.waitForIdle()
    }

    private fun capture(node: SemanticsNodeInteraction, name: String) {
        val bitmap = node.captureToImage().asAndroidBitmap()
        File(rule.activity.getExternalFilesDir(null), "v131-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun calendarKeepsOlderTotalsAndBadgesButOffersLoggingOnlyForToday() {
        val today = LocalDate.of(2026, 11, 14)
        val records =
            listOf(today.minusDays(1), today.minusDays(40)).map { date ->
                WaterEntry(
                    date.toString(),
                    250_000,
                    date
                        .atTime(12, 0)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                    date.toString(),
                    "Fixture",
                    java.time.ZoneId.systemDefault().id,
                )
            }
        val state =
            UiState(
                settings = Settings(dateFormat = "YYYY-MM-DD"),
                today = today,
                entries = records,
                goals = listOf(Goal(today.minusDays(40).toString(), 250_000)),
                ready = true,
            )
        val added = mutableListOf<LocalDate>()
        show {
            HydriTheme(state.settings) { Surface { TrackerScreen(state, {}, { added.add(it) }) } }
        }
        val logLabel = com.hydri.app.i18n.tr(R.string.log_water)
        rule.onNodeWithText(logLabel).performScrollTo().performClick()
        assertEquals(listOf(today), added)
        rule
            .onNode(
                hasContentDescription(today.minusDays(1).toString(), substring = true) and
                    SemanticsMatcher.keyIsDefined(
                        androidx.compose.ui.semantics.SemanticsProperties.Selected
                    )
            )
            .performScrollTo()
            .performClick()
        rule.onNodeWithText(logLabel).assertDoesNotExist()
        assertEquals(2, state.statistics.completed)
        assertNotNull(state.statistics.badges.first().unlocked)
        assertEquals(250_000L, state.total(today.minusDays(40)))
        assertEquals(records, state.entries)
    }

    @Test
    fun recentHistoryStaysExpandableButPastRowsAreReadOnlyAndExpireAtMidnight() {
        val today = LocalDate.of(2026, 12, 31)
        val records =
            listOf(0L, 1L, 6L, 7L, 30L).map { days ->
                val date = today.minusDays(days)
                WaterEntry(
                    "day-$days",
                    250_000,
                    date
                        .atTime(12, 0)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                    date.toString(),
                    "Record $days",
                    java.time.ZoneId.systemDefault().id,
                )
            }
        var state by mutableStateOf(UiState(entries = records, today = today, ready = true))
        var edits = 0
        show {
            HydriTheme(Settings()) {
                Surface {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        SipHistory(state) { edits++ }
                    }
                }
            }
        }
        listOf(0L, 1L, 6L).forEach { day ->
            rule.onNodeWithTag("history-${today.minusDays(day)}").assertExists()
        }
        listOf(7L, 30L).forEach { day ->
            rule.onNodeWithTag("history-${today.minusDays(day)}").assertDoesNotExist()
        }
        rule.onNodeWithTag("history-$today").performClick()
        rule.onNodeWithText("Record 0").performScrollTo().performClick()
        assertEquals(1, edits)
        rule.onNodeWithTag("history-${today.minusDays(1)}").performScrollTo().performClick()
        rule.onNodeWithText("Record 1").performScrollTo().assertHasNoClickAction()
        rule.onNodeWithText("Record 1").performTouchInput { click() }
        assertEquals(1, edits)
        rule.runOnIdle { state = state.copy(today = today.plusDays(1)) }
        rule.onNodeWithTag("history-${today.minusDays(6)}").assertDoesNotExist()
        rule.onNodeWithText("Record 0").performScrollTo().assertHasNoClickAction()
        assertEquals(records, state.entries)
        capture(rule.onRoot(), "history-read-only")
    }

    @Test
    fun exceededRingTransitionsAndReturnsAcrossEveryThemeAndAccent() {
        var settings by mutableStateOf(Settings(theme = "Light"))
        var total by mutableLongStateOf(2_500_000L)
        show {
            HydriTheme(settings) {
                Surface { WaterProgress(total, 2_500_000, "mL", compact = true) }
            }
        }
        val ring = rule.onNode(SemanticsMatcher.keyIsDefined(GoalExceededBlend))
        fun blend() = ring.fetchSemanticsNode().config[GoalExceededBlend]
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { total = 2_750_000 }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(160)
        assertTrue("The appearance should transition smoothly", blend() > 0f && blend() < 1f)
        rule.mainClock.advanceTimeBy(700)
        assertEquals(1f, blend(), .001f)
        rule.mainClock.autoAdvance = true
        val themes =
            listOf(
                "light" to Settings(theme = "Light"),
                "dark" to Settings(theme = "Dark"),
                "oled" to Settings(theme = "Dark", oled = true),
                "system" to Settings(theme = "System"),
            )
        for ((theme, base) in themes) for (accent in 0..6) {
            rule.runOnIdle {
                settings = base.copy(accent = accent.coerceAtMost(5), dynamicColors = accent == 6)
                total = 2_500_000
            }
            rule.waitForIdle()
            assertEquals(0f, blend(), .001f)
            val normal = ring.captureToImage().toPixelMap()
            val normalPixel = normal[normal.width / 2, (normal.height * .06f).toInt()]
            rule.runOnIdle { total = 2_750_000 }
            rule.waitForIdle()
            assertEquals(1f, blend(), .001f)
            ring.assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.ContentDescription,
                    listOf(
                        com.hydri.app.i18n.tr(
                            R.string.value_of_value_value_percent,
                            com.hydri.app.domain.Volume.format(2_750_000, "mL"),
                            com.hydri.app.domain.Volume.format(2_500_000, "mL"),
                            110,
                        )
                    ),
                )
            )
            val above = ring.captureToImage().toPixelMap()
            val abovePixel = above[above.width / 2, (above.height * .06f).toInt()]
            assertTrue(
                "Exceeded ring should change color: $theme/$accent",
                distance(normalPixel, abovePixel) > .025f,
            )
            if (accent == 0 || accent == 6) capture(ring, "ring-$theme-$accent-exceeded")
            rule.runOnIdle { total = 2_500_000 }
            rule.waitForIdle()
            assertEquals(0f, blend(), .001f)
            if (accent == 0 || accent == 6) capture(ring, "ring-$theme-$accent-complete")
            rule.runOnIdle { total = 1_250_000 }
            rule.waitForIdle()
            assertEquals(0f, blend(), .001f)
            ring.assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.ContentDescription,
                    listOf(
                        com.hydri.app.i18n.tr(
                            R.string.value_of_value_value_percent,
                            com.hydri.app.domain.Volume.format(1_250_000, "mL"),
                            com.hydri.app.domain.Volume.format(2_500_000, "mL"),
                            50,
                        )
                    ),
                )
            )
        }
        rule.runOnIdle {
            settings = settings.copy(reducedMotion = true)
            total = 3_000_000
        }
        rule.waitForIdle()
        assertEquals(1f, blend(), .001f)
    }

    @Test
    fun moreIconReallyFillsAllFourSquaresWhileOtherTabsKeepItOutlined() {
        var selected by mutableStateOf("Home")
        val settings = Settings(theme = "Light", accent = 0)
        show {
            HydriTheme(settings) {
                Surface { SlidingNavigation(selected, false) { selected = it } }
            }
        }
        val outline =
            rule
                .onNodeWithTag("tab-icon-More-outlined", useUnmergedTree = true)
                .captureToImage()
                .toPixelMap()
        rule.onNodeWithContentDescription("More").performClick()
        val filledNode = rule.onNodeWithTag("tab-icon-More-filled", useUnmergedTree = true)
        val filled = filledNode.captureToImage().toPixelMap()
        val primary = hydriColors(rule.activity, settings).primary
        for (x in listOf(.27f, .73f)) for (y in listOf(.27f, .73f)) {
            assertTrue(
                distance(filled[(filled.width * x).toInt(), (filled.height * y).toInt()], primary) <
                    .06f
            )
            assertTrue(
                distance(
                    outline[(outline.width * x).toInt(), (outline.height * y).toInt()],
                    primary,
                ) > .1f
            )
        }
        capture(filledNode, "more-solid-icon")
        rule.onNodeWithContentDescription("Home").performClick()
        rule.onNodeWithTag("tab-icon-More-outlined", useUnmergedTree = true).assertExists()
    }

    private fun distance(a: Color, b: Color) =
        abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)
}
