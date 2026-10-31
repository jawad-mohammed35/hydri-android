// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.ui.NavigationIndicatorPosition
import com.hydri.app.ui.PrimaryTabRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class IndicatorReturnDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repo: Repository
    private var snapshot: String? = null

    @Before
    fun setup() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
        repo =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                    as HydriApplication)
                .repository
        runBlocking {
            snapshot = repo.backup()
            repo.preferences.save(
                repo.preferences.flow.first().copy(onboarded = true, reducedMotion = false)
            )
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun restore() {
        rule.mainClock.autoAdvance = true
        snapshot?.let { runBlocking { repo.restore(it) } }
    }

    private fun position() =
        rule
            .onNodeWithTag("sliding-navigation")
            .fetchSemanticsNode()
            .config[NavigationIndicatorPosition]

    private fun open(page: String) {
        rule.onNodeWithText(page).performScrollTo().clearFloatingNavigation(rule)
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText(page).performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("settings-page-$page").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
        assertEquals(
            "More",
            rule.onNodeWithTag("page-viewport").fetchSemanticsNode().config[PrimaryTabRoute],
        )
    }

    private fun sampleReturn(back: () -> Unit) {
        rule.mainClock.autoAdvance = false
        back()
        var samples = 0
        repeat(60) {
            rule.mainClock.advanceTimeByFrame()
            Thread.sleep(8) // Allow Android's real back dispatcher to deliver its callback.
            if (rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty()) {
                samples++
                assertEquals("Returning More must never begin at Home", 3f, position(), .001f)
            }
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertTrue("No returning navigation frames sampled", samples > 0)
        rule.onNodeWithTag("tab-icon-More-filled", useUnmergedTree = true).assertExists()
    }

    @Test
    fun allMorePagesReturnAtMoreFromTheirFirstFrame() {
        rule.onNodeWithContentDescription("More").performClick()
        rule.waitForIdle()
        listOf("Notifications", "Hydration Guide", "Data & Backup", "About").forEach { page ->
            open(page)
            sampleReturn { rule.onNodeWithContentDescription("Back to More").performClick() }
            open(page)
            sampleReturn {
                val metrics = rule.activity.resources.displayMetrics
                InstrumentationRegistry.getInstrumentation()
                    .uiAutomation
                    .executeShellCommand(
                        "input swipe 1 ${metrics.heightPixels / 2} ${metrics.widthPixels * 7 / 10} ${metrics.heightPixels / 2} 450"
                    )
                    .use { fd -> java.io.FileInputStream(fd.fileDescriptor).use { it.readBytes() } }
            }
        }
    }

    @Test
    fun intentionalSwitchStillAnimatesAndRestoredMoreKeepsItsSelection() {
        rule.onNodeWithContentDescription("Home").performClick()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("More").performClick()
        rule.mainClock.advanceTimeBy(64)
        assertTrue("Primary-tab motion must remain animated", position() in 0f..2.99f)
        rule.mainClock.advanceTimeBy(1000)
        assertEquals(3f, position(), .001f)
        rule.mainClock.autoAdvance = true
        open("About")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("settings-page-About").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            "More",
            rule.onNodeWithTag("page-viewport").fetchSemanticsNode().config[PrimaryTabRoute],
        )
        sampleReturn { rule.onNodeWithContentDescription("Back to More").performClick() }
    }
}
