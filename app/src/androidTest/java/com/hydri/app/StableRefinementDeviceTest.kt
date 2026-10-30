package com.hydri.app

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.Volume
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class StableRefinementDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repo: Repository
    private var snapshot: String? = null
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Assume.assumeTrue(
            "Dedicated QA emulator only",
            InstrumentationRegistry.getArguments().getString("hydriQa") == "true",
        )
        repo = (context.applicationContext as HydriApplication).repository
        runBlocking {
            snapshot = repo.backup()
            repo.preferences.save(
                repo.preferences.flow
                    .first()
                    .copy(
                        onboarded = true,
                        theme = "Light",
                        oled = false,
                        dynamicColors = false,
                        unit = "mL",
                        haptics = false,
                    )
            )
        }
        await { rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun restore() {
        snapshot?.let { runBlocking { repo.restore(it) } }
    }

    private fun await(condition: () -> Boolean) = rule.waitUntil(10000, condition)

    private fun entries() = runBlocking { repo.dao.allEntries() }

    private fun tap(text: String) {
        val node = rule.onNodeWithText(text)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        rule.waitForIdle()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "v121-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun homeShortcutAndTabsShareNavigationState() {
        repeat(3) {
            rule
                .onNode(hasText("Bottles") and !hasAnyAncestor(hasTestTag("sliding-navigation")))
                .performScrollTo()
                .performClick()
            rule.onNodeWithContentDescription("Bottles").assertIsSelected()
            rule.onNodeWithText("Create a bottle").assertExists()
            rule.onNodeWithContentDescription("Home").performClick()
            rule.onNodeWithContentDescription("Home").assertIsSelected()
            rule.onNodeWithTag("daily-undo").assertExists()
        }
        capture("home-navigation")
    }

    @Test
    fun glassIndicatorHasNoTouchRippleAndRetainsFocus() {
        val bar = rule.onNodeWithTag("sliding-navigation")
        val before = bar.captureToImage().toPixelMap()
        bar.performTouchInput { down(Offset(width * .375f, centerY)) }
        rule.mainClock.advanceTimeBy(100)
        val pressed = bar.captureToImage().toPixelMap()
        for (fraction in listOf(.3f, .375f, .45f)) {
            val x = (before.width * fraction).toInt()
            for (y in listOf(4, before.height / 2, before.height - 5)) assertEquals(
                "Touch must not add a box/ripple",
                before[x, y],
                pressed[x, y],
            )
        }
        bar.performTouchInput {
            moveTo(Offset(width * .625f, centerY), delayMillis = 180)
            up()
        }
        rule.onNodeWithContentDescription("Tracker").assertIsSelected()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Home").performSemanticsAction(
            SemanticsActions.RequestFocus
        ) {
            it()
        }
        rule.onNodeWithContentDescription("Home").assertIsFocused()
        capture("navigation-focus")
    }

    @Test
    fun successfulLoggingIsSilentAndDailyUndoRemains() {
        val count = entries().size
        val bottle = runBlocking { repo.dao.allBottles().first { it.favorite } }
        rule
            .onNodeWithContentDescription(
                "Log ${bottle.name}, ${Volume.format(bottle.volume,"mL")}"
            )
            .performScrollTo()
            .performClick()
        await { entries().size == count + 1 }
        rule
            .onNode(hasText("Added", substring = true) and hasAnyAncestor(hasTestTag("snackbar")))
            .assertDoesNotExist()
        tap("Custom amount")
        rule.onNodeWithText("Amount (mL)").performTextReplacement("431")
        tap("Save entry")
        await { entries().size == count + 2 }
        rule.onNode(hasAnyAncestor(hasTestTag("snackbar")) and hasText("Undo")).assertDoesNotExist()
        rule.onNodeWithTag("daily-undo").performScrollTo().performClick()
        await { entries().size == count + 1 }
        assertFalse(entries().any { it.volume == 431_000L && it.name == "Custom water" })
    }

    @Test
    fun bothSwipeDirectionsDeleteAndUndoRestores() {
        for (right in listOf(false, true)) {
            val entry = runBlocking {
                repo.log(321_000, "QA swipe ${if(right) "right" else "left"}")
            }
            val row = rule.onNodeWithTag("sip-${entry.id}")
            await { rule.onAllNodesWithTag("sip-${entry.id}").fetchSemanticsNodes().isNotEmpty() }
            row.performScrollTo()
            row.performTouchInput {
                if (right) swipeRight(durationMillis = 400) else swipeLeft(durationMillis = 400)
            }
            await { entries().none { it.id == entry.id } }
            rule.onNode(hasText("Undo") and hasAnyAncestor(hasTestTag("snackbar"))).performClick()
            await { entries().any { it == entry } }
        }
    }

    @Test
    fun editorDeletesImmediatelyAndFooterAndDeveloperAreClean() {
        val entry = runBlocking { repo.log(287_000, "QA immediate delete") }
        await { rule.onAllNodesWithText("QA immediate delete").fetchSemanticsNodes().isNotEmpty() }
        tap("QA immediate delete")
        tap("Delete entry")
        await { entries().none { it.id == entry.id } }
        rule.onNodeWithText("Delete this water entry?").assertDoesNotExist()
        rule.onNodeWithText("Save entry").assertDoesNotExist()
        rule.onNode(hasText("Undo") and hasAnyAncestor(hasTestTag("snackbar"))).performClick()
        await { entries().any { it == entry } }
        rule.onNodeWithContentDescription("More").performClick()
        rule.onNodeWithText("Version 1.2.1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Version 1.2.1 (4)").assertDoesNotExist()
        tap("About")
        rule.onNodeWithText("Developed by Jawad Mohammed").assertIsDisplayed()
        rule.onNodeWithText("LinkedIn").assertHasClickAction()
    }

    @Test
    fun notificationTimePickerSupportsThemesFormatsValidationAndCancel() {
        runBlocking {
            repo.preferences.save(
                repo.preferences.flow
                    .first()
                    .copy(reminders = true, timeFormat = "24-hour", start = 480)
            )
        }
        rule.onNodeWithContentDescription("More").performClick()
        tap("Notifications")
        for ((theme, oled, dynamic) in
            listOf(
                Triple("Light", false, false),
                Triple("Dark", false, false),
                Triple("Dark", true, false),
                Triple("Dark", true, true),
            )) {
            runBlocking {
                repo.preferences.save(
                    repo.preferences.flow
                        .first()
                        .copy(theme = theme, oled = oled, dynamicColors = dynamic)
                )
            }
            tap("Active hours start")
            rule.onNodeWithTag("glass-time-picker").assertIsDisplayed()
            rule.onNodeWithText("24-hour time").assertExists()
            rule.onNodeWithText("Hour").performTextReplacement("25")
            rule.onNodeWithText("Save time").assertIsNotEnabled()
            rule.onNodeWithText("Hour").performTextReplacement("23")
            rule.onNodeWithText("Minute").performTextReplacement("59")
            rule.onNodeWithText("Save time").assertIsDisplayed()
            rule.onNodeWithText("Minute").performImeAction()
            capture("time-$theme-$oled-$dynamic")
            tap("Save time")
            await { runBlocking { repo.preferences.flow.first().start == 1439 } }
            await { rule.onAllNodesWithTag("glass-time-picker").fetchSemanticsNodes().isEmpty() }
        }
        runBlocking {
            repo.preferences.save(
                repo.preferences.flow.first().copy(timeFormat = "12-hour", start = 0)
            )
        }
        tap("Active hours start")
        rule.onNodeWithText("AM").assertIsSelected()
        rule.onNodeWithText("Hour").performTextReplacement("12")
        tap("PM")
        tap("Save time")
        await { runBlocking { repo.preferences.flow.first().start == 720 } }
        tap("Active hours start")
        rule.onNodeWithText("Hour").performTextReplacement("1")
        tap("Cancel")
        await { rule.onAllNodesWithTag("glass-time-picker").fetchSemanticsNodes().isEmpty() }
        assertEquals(720, runBlocking { repo.preferences.flow.first().start })
    }
}
