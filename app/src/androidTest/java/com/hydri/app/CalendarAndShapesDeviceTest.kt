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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
import com.hydri.app.ui.*
import java.io.File
import java.time.*
import kotlin.math.abs
import org.junit.*
import org.junit.Assert.*

class CalendarAndShapesDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun isolated() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
    }

    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent(content = content) }
        rule.waitForIdle()
    }

    private fun capture(name: String) {
        rule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(rule.activity.getExternalFilesDir(null), "v140-$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun swipesArrowsSelectionAndRapidGesturesKeepMonthsAndIndicatorsCorrect() {
        val today = LocalDate.of(2026, 3, 15)
        val recorded = LocalDate.of(2026, 2, 14)
        val entry =
            WaterEntry(
                "fixture",
                350_000,
                recorded.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
                recorded.toString(),
                "Fixture",
                "UTC",
            )
        val state =
            UiState(
                today = today,
                entries = listOf(entry),
                goals = listOf(Goal("2026-01-01", 250_000)),
            )
        var month by mutableStateOf(YearMonth.from(today))
        var selected by mutableStateOf(today)
        show {
            HydriTheme(Settings(theme = "Light")) {
                Surface {
                    TrackerCalendar(state, month, selected, { month = it }, { selected = it })
                }
            }
        }
        val calendar = rule.onNodeWithTag("tracker-calendar")
        calendar.performTouchInput { swipeRight() }
        assertEquals(YearMonth.of(2026, 2), month)
        rule.onNodeWithTag("calendar-day-$recorded").performClick().assertIsSelected()
        assertEquals(recorded, selected)
        rule
            .onNodeWithTag("calendar-day-$recorded")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    tr(
                        R.string.value_of_value_value_percent,
                        Volume.format(350_000, "mL"),
                        Volume.format(250_000, "mL"),
                        140,
                    ),
                )
            )
        rule.onNodeWithContentDescription(tr(R.string.next_month)).performClick()
        assertEquals(YearMonth.from(today), month)
        calendar.performTouchInput { swipeLeft() }
        assertEquals(YearMonth.from(today), month)
        calendar.performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        calendar.performTouchInput { swipeRight(durationMillis = 80) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(30)
        calendar.performTouchInput { swipeRight(durationMillis = 80) }
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(
            "A second gesture during the transition must not skip another month",
            YearMonth.of(2026, 1),
            month,
        )
        calendar.performTouchInput { swipeRight() }
        assertEquals(YearMonth.of(2025, 12), month)
        calendar.performTouchInput { swipeLeft() }
        assertEquals(YearMonth.of(2026, 1), month)
        assertEquals(recorded, selected)
        rule.onNodeWithText(formattedMonth(month)).assertExists()
        assertEquals(listOf(entry), state.entries)
        capture("calendar")
    }

    @Test
    fun verticalScrollingAndSmallHorizontalDragsDoNotChangeMonths() {
        val today = LocalDate.of(2026, 3, 15)
        var month by mutableStateOf(YearMonth.from(today))
        lateinit var scroll: ScrollState
        show {
            HydriTheme(Settings()) {
                Surface {
                    scroll = rememberScrollState()
                    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                        TrackerCalendar(UiState(today = today), month, today, { month = it }, {})
                        Spacer(Modifier.height(androidx.compose.ui.unit.Dp(650f)))
                    }
                }
            }
        }
        val calendar = rule.onNodeWithTag("tracker-calendar")
        calendar.performTouchInput {
            swipe(Offset(center.x - 20, center.y), Offset(center.x + 20, center.y), 200)
        }
        assertEquals(YearMonth.from(today), month)
        calendar.performTouchInput { swipeUp() }
        assertTrue("The parent page must retain vertical scrolling", scroll.value > 0)
        assertEquals(YearMonth.from(today), month)
    }

    @Test
    fun trackerOnlyShowsBalancedWeekMonthControlsAndKeepsStatisticsAndBadges() {
        val today = LocalDate.of(2026, 3, 15)
        val state =
            UiState(today = today, goals = listOf(Goal("2026-01-01", 250_000)), ready = true)
        val before = state.statistics
        show { HydriTheme(Settings()) { Surface { TrackerScreen(state, {}) } } }
        rule.onNodeWithText(tr(R.string.log_water)).assertDoesNotExist()
        rule.onNodeWithText(formattedDate(today, "System")).assertDoesNotExist()
        rule.onNodeWithTag("intake-range").performScrollTo()
        rule.onNodeWithText(displaySetting("Day")).assertDoesNotExist()
        val week = rule.onNodeWithTag("intake-range-Week")
        val month = rule.onNodeWithTag("intake-range-Month")
        week.assertIsSelected()
        assertTrue(
            abs(
                week.fetchSemanticsNode().boundsInRoot.width -
                    month.fetchSemanticsNode().boundsInRoot.width
            ) < 1f
        )
        fun countBars() =
            rule
                .onAllNodes(
                    SemanticsMatcher("chart bar") {
                        it.config
                            .getOrNull(SemanticsProperties.TestTag)
                            ?.startsWith("intake-bar-") == true
                    }
                )
                .fetchSemanticsNodes()
                .size
        assertEquals(7, countBars())
        month.performClick().assertIsSelected()
        assertEquals(15, countBars())
        week.performClick().assertIsSelected()
        assertEquals(7, countBars())
        rule
            .onNodeWithText(plural(R.plurals.completed_days_overall, state.statistics.completed))
            .assertDoesNotExist()
        rule
            .onNodeWithText(tr(R.string.badges_reflect_your_recorded_goals_editing_history_can_cha))
            .assertDoesNotExist()
        rule.onNodeWithText(tr(R.string.badges)).performScrollTo().assertExists()
        assertEquals(before, state.statistics)
        capture("tracker-clean")
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun allFourteenShapesRenderDistinctlyInEveryBottleColorAndTheme() {
        var settings by mutableStateOf(Settings(theme = "Light"))
        var color by mutableIntStateOf(0)
        show {
            HydriTheme(settings) {
                Surface {
                    FlowRow(Modifier.fillMaxSize(), maxItemsInEachRow = 4) {
                        bottleIcons.forEachIndexed { i, image ->
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment =
                                    androidx.compose.ui.Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    image,
                                    bottleNames[i],
                                    tint = accents[color],
                                    modifier = Modifier.size(androidx.compose.ui.unit.Dp(36f)),
                                )
                                Text(bottleNames[i], style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        assertEquals(BottleShape.entries.size, bottleIcons.size)
        assertEquals(14, bottleIcons.map { it.name }.toSet().size)
        listOf(
                Settings(theme = "Light"),
                Settings(theme = "Dark"),
                Settings(theme = "Dark", oled = true),
                Settings(theme = "Light", dynamicColors = true),
            )
            .forEachIndexed { theme, base ->
                (0..5).forEach { index ->
                    rule.runOnIdle {
                        settings = base
                        color = index
                    }
                    rule.waitForIdle()
                    val hashes =
                        bottleNames.map { label ->
                            val pixels =
                                rule
                                    .onNodeWithContentDescription(label)
                                    .captureToImage()
                                    .toPixelMap()
                            var hash = 1
                            for (y in 0 until pixels.height) for (x in 0 until pixels.width) hash =
                                31 * hash + pixels[x, y].toArgb()
                            hash
                        }
                    assertEquals(
                        "Shapes must have distinct rendered silhouettes",
                        14,
                        hashes.toSet().size,
                    )
                    if (index == 0) capture("shapes-theme-$theme")
                }
            }
    }
}
