// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
import com.hydri.app.ui.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class CustomLoggingDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun isolated() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
    }

    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent(content = content) }
        rule.waitForIdle()
    }

    @Test
    fun amountFocusKeyboardDoneValidationAndUnicodeNames() {
        var open by mutableStateOf(true)
        var saved: String? = null

        show {
            HydriTheme(Settings()) {
                if (open)
                    EntryDialog(null, LocalDate.now(), Settings(), { open = false }, {}) {
                        volume,
                        date,
                        _,
                        name ->
                        assertEquals(375_000L, volume)
                        assertEquals(LocalDate.now(), date)
                        saved = name
                        open = false
                    }
                // Window insets must be read from the dialog window, not the obscured activity.
            }
        }
        // Android window focus is delivered outside Compose's idle clock.
        rule.waitUntil(10000) {
            rule
                .onAllNodes(hasTestTag("entry-amount") and isFocused())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithTag("entry-amount").assertIsFocused().assertTextContains("")
        rule.onNodeWithText(tr(R.string.save_entry)).assertIsNotEnabled()
        rule.waitUntil(10000) {
            val dump =
                InstrumentationRegistry.getInstrumentation()
                    .uiAutomation
                    .executeShellCommand("dumpsys input_method")
                    .use {
                        java.io
                            .FileInputStream(it.fileDescriptor)
                            .readBytes()
                            .toString(Charsets.UTF_8)
                    }
            dump.contains("mInputShown=true") || dump.contains("isInputViewShown=true")
        }
        rule.waitForIdle()
        Thread.sleep(350) // Native keyboard/window animations are outside Compose's clock.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags =
                    flags or
                        android.accessibilityservice.AccessibilityServiceInfo
                            .FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        rule.waitUntil(5000) {
            automation.windows.any {
                it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }
        }
        val windows = automation.windows
        val ime =
            windows.first {
                it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }
        val keyboardBounds = android.graphics.Rect()
        // The IME root can span the screen; the window bounds are its touchable region.
        ime.getBoundsInScreen(keyboardBounds)
        val appBounds = android.graphics.Rect()
        windows
            .first {
                it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION
            }
            .getBoundsInScreen(appBounds)
        for (label in listOf(tr(R.string.cancel), tr(R.string.save_entry))) {
            // Compose coordinates are relative to its window. Include the native window offset,
            // which catches floating-window system-bar fitting as well as IME overlap.
            val actionBottom =
                rule.onNodeWithText(label).getUnclippedBoundsInRoot().bottom.value *
                    rule.activity.resources.displayMetrics.density + appBounds.top
            assertTrue(
                "Action hidden behind keyboard: $label bottom=$actionBottom / $keyboardBounds",
                actionBottom <= keyboardBounds.top + 1,
            )
        }
        val visible = automation.takeScreenshot()
        File(rule.activity.getExternalFilesDir(null), "v150-keyboard-visible.png")
            .outputStream()
            .use { visible.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rule.onNodeWithTag("entry-amount").performTextReplacement("0")
        rule.onNodeWithTag("entry-amount").performImeAction()
        rule.onNodeWithText(tr(R.string.save_entry)).assertIsNotEnabled()
        assertNull(saved)
        rule.onNodeWithTag("entry-amount").performTextReplacement("375")
        rule.onNodeWithTag("entry-name").performTextReplacement("  朝の水 💧  물  ")
        rule.onNodeWithTag("entry-name").performImeAction()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(rule.activity.getExternalFilesDir(null), "v150-custom-keyboard.png")
            .outputStream()
            .use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rule.onNodeWithText(tr(R.string.save_entry)).performClick()
        rule.waitUntil(5000) { saved != null }
        assertEquals("朝の水 💧  물", saved)
        rule.onNodeWithTag("glass-dialog").assertDoesNotExist()
    }

    @Test
    fun customNamesPersistEditBackupAndLeaveBottleNamesIntact() =
        runBlocking<Unit> {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repo = (context.applicationContext as HydriApplication).repository
            val original = repo.backup()
            try {
                val name = "  Morning  朝 💧 "
                val e = repo.log(123_000, name)
                assertEquals(name.trim(), repo.dao.entry(e.id)!!.name)
                assertNull(e.bottleIcon)
                val updated = "운동 후 水 " + "💧".repeat(35)
                repo.updateEntry(e.id, e.volume, e.timestamp, updated)
                assertEquals(updated, repo.dao.entry(e.id)!!.name)
                val backup = repo.backup()
                repo.restore(backup)
                assertEquals(updated, repo.dao.entry(e.id)!!.name)
                val bottle = repo.dao.allBottles().first()
                val bottled = repo.log(bottle.volume, bottle.name, bottle = bottle)
                repo.updateEntry(
                    bottled.id,
                    bottled.volume,
                    bottled.timestamp,
                    "Should not replace bottle",
                )
                assertEquals(bottle.name, repo.dao.entry(bottled.id)!!.name)
                repo.deleteEntry(e.id)
                assertNull(repo.dao.entry(e.id))
            } finally {
                repo.restore(original)
            }
        }

    @Test
    fun namedEntriesAppearInHomeAndTrackerAndRemainEditableToday() {
        val repo = (rule.activity.application as HydriApplication).repository
        val original = runBlocking { repo.backup() }
        try {
            runBlocking {
                repo.preferences.save(repo.preferences.flow.first().copy(onboarded = true))
                repo.log(333_000, "QA morning 朝")
            }
            rule.waitUntil(10000) {
                rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(routeLabel("Home")).performClick()
            rule
                .onNodeWithText("QA morning 朝")
                .performScrollTo()
                .clearFloatingNavigation(rule)
                .performClick()
            rule.onNodeWithTag("entry-name").assertTextContains("QA morning 朝")
            rule.onNodeWithTag("entry-name").performTextReplacement("After workout 💧")
            rule.onNodeWithTag("entry-name").performImeAction()
            rule.onNodeWithText(tr(R.string.save_entry)).performClick()
            rule.waitUntil(10000) {
                runBlocking { repo.dao.allEntries().any { it.name == "After workout 💧" } }
            }
            rule.onNodeWithContentDescription(routeLabel("Tracker")).performClick()
            rule
                .onNodeWithTag("history-${LocalDate.now()}")
                .performScrollTo()
                .clearFloatingNavigation(rule)
                .performClick()
            rule.onNodeWithText("After workout 💧").performScrollTo().assertExists()
        } finally {
            runBlocking { repo.restore(original) }
        }
    }

    @Test
    fun progressivelyEnergizedRingReturnsAndCapsAcrossThemes() {
        var total by mutableLongStateOf(2_000_000)
        var settings by mutableStateOf(Settings())
        show { HydriTheme(settings) { Surface { WaterProgress(total, 2_000_000, "mL") } } }
        val ring = rule.onNode(SemanticsMatcher.keyIsDefined(SuperchargeIntensity))
        for (s in
            listOf(
                Settings(theme = "Light"),
                Settings(theme = "Dark"),
                Settings(theme = "Dark", oled = true),
                Settings(theme = "System", dynamicColors = true),
            )) {
            rule.runOnIdle { settings = s }
            for (percent in listOf(50, 100, 105, 125, 150, 200, 250, 150, 100, 50)) {
                rule.runOnIdle { total = 2_000_000L * percent / 100 }
                rule.waitForIdle()
                assertEquals(
                    ((percent - 100) / 100f).coerceIn(0f, 1f),
                    ring.fetchSemanticsNode().config[SuperchargeIntensity],
                    .001f,
                )
                if (percent in listOf(105, 150, 200)) {
                    val image = ring.captureToImage().asAndroidBitmap()
                    File(
                            rule.activity.getExternalFilesDir(null),
                            "v150-ring-${s.theme}-${s.oled}-${s.dynamicColors}-$percent.png",
                        )
                        .outputStream()
                        .use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
        rule.runOnIdle {
            settings = settings.copy(reducedMotion = true)
            total = 3_000_000
        }
        rule.waitForIdle()
        assertEquals(.5f, ring.fetchSemanticsNode().config[SuperchargeIntensity], .001f)
    }
}
