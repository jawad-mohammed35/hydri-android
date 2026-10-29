package com.hydri.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HydriFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun text(value: String) = rule.onNodeWithText(value)

    private fun click(value: String) {
        text(value).performClick()
        rule.waitForIdle()
    }

    private fun scrollClick(value: String) {
        rule.waitUntil(10000) {
            rule.onAllNodesWithText("Undo").fetchSemanticsNodes().isEmpty() &&
                rule.onAllNodesWithText("Daily goal updated").fetchSemanticsNodes().isEmpty()
        }
        text(value).performScrollTo()
        rule.waitForIdle()
        text(value).performClick()
        rule.waitForIdle()
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @After
    fun captureFinalState() {
        capture("test-final")
    }

    @Test
    fun completeOnboardingLogManageBottlesTrackerAndAppearance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking { (context.applicationContext as HydriApplication).repository.reset() }
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("Let's begin").fetchSemanticsNodes().isNotEmpty()
        }
        click("Let's begin")
        text("Your name (optional)").performTextInput("Jawad")
        click("Continue")
        click("Continue")
        click("Continue")
        click("Continue")
        click("Continue")
        click("Start hydrating")
        rule.waitUntil(15000) {
            rule.onAllNodesWithContentDescription("Home").fetchSemanticsNodes().isNotEmpty()
        }
        rule
            .onNodeWithContentDescription("Log Everyday glass, 250 mL")
            .performScrollTo()
            .performClick()
        rule.waitUntil(10000) {
            runBlocking {
                (context.applicationContext as HydriApplication)
                    .repository
                    .dao
                    .allEntries()
                    .isNotEmpty()
            }
        }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Undo").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithContentDescription("Bottles").performClick()
        scrollClick("Create a bottle")
        text("Bottle name").performTextInput("Trail flask")
        text("Amount (mL)").performTextReplacement("750")
        click("Save bottle")
        text("Trail flask").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Edit")[3].performScrollTo()
        rule.waitForIdle()
        rule.onAllNodesWithText("Edit")[3].performClick()
        rule.waitForIdle()
        text("Bottle name").assertTextContains("Trail flask")
        text("Amount (mL)").performTextReplacement("800")
        click("Save bottle")
        rule.onAllNodesWithText("Edit")[3].performScrollTo()
        rule.waitForIdle()
        rule.onAllNodesWithText("Edit")[3].performClick()
        rule.waitForIdle()
        text("Amount (mL)").performTextReplacement("750")
        click("Save bottle")
        rule.onNodeWithContentDescription("Tracker").performClick()
        text("Every drop adds up.").assertIsDisplayed()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Undo").fetchSemanticsNodes().isEmpty() }
        text("Change").performScrollTo()
        rule.waitForIdle()
        capture("goal-before")
        click("Change")
        capture("goal-after")
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Amount (mL)").fetchSemanticsNodes().isNotEmpty()
        }
        text("Amount (mL)").performTextReplacement("2000")
        click("Save goal")
        rule.waitUntil(10000) {
            rule.onAllNodesWithText("Daily goal updated").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithContentDescription("More").performClick()
        scrollClick("Theme")
        click("Dark")
        rule.onNodeWithContentDescription("OLED pure black").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Lavender").performScrollTo().performClick()
        rule.waitUntil(10000) {
            runBlocking {
                val preferences =
                    (context.applicationContext as HydriApplication)
                        .repository
                        .preferences
                        .flow
                        .first()
                preferences.accent == 2 && preferences.oled && preferences.theme == "Dark"
            }
        }
        rule.waitForIdle()
        capture("oled")
        scrollClick("Water units")
        click("US fl oz")
        rule.onNodeWithContentDescription("Home").performClick()
        rule
            .onNodeWithContentDescription("Log Trail flask, 25.4 oz")
            .performScrollTo()
            .assertExists()
        rule.onNodeWithContentDescription("Log Trail flask, 25.4 oz").performClick()
        rule.waitUntil(10000) {
            runBlocking {
                (context.applicationContext as HydriApplication).repository.dao.allEntries().size ==
                    2
            }
        }
        scrollClick("Custom amount")
        text("Amount (oz)").performTextReplacement("4")
        click("Save entry")
        rule.waitUntil(10000) { rule.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty() }
        click("Undo")
        rule.waitUntil(10000) {
            runBlocking {
                (context.applicationContext as HydriApplication).repository.dao.allEntries().size ==
                    2
            }
        }
        scrollClick("Custom amount")
        text("Amount (oz)").performTextReplacement("4")
        click("Save entry")
        rule.waitUntil(10000) { rule.onAllNodesWithText("Undo").fetchSemanticsNodes().isEmpty() }
        scrollClick("Custom water")
        text("Amount (oz)").performTextReplacement("8")
        click("Save entry")
        scrollClick("Custom water")
        click("Delete entry")
        click("Confirm")
        rule.waitUntil(10000) {
            runBlocking {
                (context.applicationContext as HydriApplication).repository.dao.allEntries().size ==
                    2
            }
        }
        // Restore default display for a useful delivered screenshot; data remains for persistence
        // inspection.
        runBlocking {
            val prefs = (context.applicationContext as HydriApplication).repository.preferences
            val s = prefs.flow.first()
            prefs.save(s.copy(theme = "Light", oled = false, unit = "mL", accent = 0))
        }
        rule.waitForIdle()
        rule.onNodeWithText("HYDRI / A LITTLE EVERY DAY").performScrollTo()
        rule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(250)
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "home.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
