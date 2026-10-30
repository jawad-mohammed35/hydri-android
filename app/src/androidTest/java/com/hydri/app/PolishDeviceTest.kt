package com.hydri.app

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.ui.hydriColors
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class PolishDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repo: Repository
    private var snapshot: String? = null
    private val appearances =
        listOf(
            Triple("Light", false, false),
            Triple("Dark", false, false),
            Triple("Dark", true, false),
            Triple("Dark", true, true),
        )

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
                        name = "Jawad",
                        theme = "Light",
                        dynamicColors = false,
                        oled = false,
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

    private fun await(test: () -> Boolean) = rule.waitUntil(10000, test)

    private fun tap(text: String) {
        val matcher =
            if (rule.onAllNodesWithTag("glass-dialog").fetchSemanticsNodes().isNotEmpty())
                hasText(text) and hasAnyAncestor(hasTestTag("glass-dialog"))
            else hasText(text)
        if (
            rule
                .onAllNodes(matcher and hasAnyAncestor(hasScrollAction()))
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
            rule.onNode(matcher).performScrollTo()
        rule.onNode(matcher).clearFloatingNavigation(rule).performClick()
        rule.waitForIdle()
    }

    private fun theme(value: Triple<String, Boolean, Boolean>) = runBlocking {
        repo.preferences.save(
            repo.preferences.flow
                .first()
                .copy(theme = value.first, oled = value.second, dynamicColors = value.third)
        )
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        Thread.sleep(350) // Android's window/IME animations run outside Compose's test clock.
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "v122-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun preferredNameUsesGlassInEveryThemeAndCancelDoesNotSave() {
        rule.onNodeWithContentDescription("More").clearFloatingNavigation(rule).performClick()
        for ((index, appearance) in appearances.withIndex()) {
            theme(appearance)
            val original = runBlocking { repo.preferences.flow.first().name }
            tap("Preferred name")
            rule.onNodeWithTag("glass-dialog").assertIsDisplayed()
            rule.onNodeWithTag("preferred-name-input").performTextReplacement("Cancelled")
            tap("Cancel")
            await { rule.onAllNodesWithTag("glass-dialog").fetchSemanticsNodes().isEmpty() }
            assertEquals(original, runBlocking { repo.preferences.flow.first().name })
            tap("Preferred name")
            rule.onNodeWithTag("preferred-name-input").assertTextContains(original)
            rule.onNodeWithTag("preferred-name-input").clearFloatingNavigation(rule).performClick()
            Thread.sleep(500) // Let Android's IME resize settle before injecting replacement text.
            rule.onNodeWithTag("preferred-name-input").performTextReplacement("jAWad $index")
            rule.onNodeWithText("Save").assertIsDisplayed()
            capture("name-$index")
            tap("Save")
            await { runBlocking { repo.preferences.flow.first().name == "jAWad $index" } }
        }
    }

    @Test
    fun reviewPreferencesUsesGlassAndRetainsHistoryAndBottles() {
        val beforeEntries = runBlocking { repo.dao.allEntries() }
        val beforeBottles = runBlocking { repo.dao.allBottles() }
        rule.onNodeWithContentDescription("More").clearFloatingNavigation(rule).performClick()
        for ((index, appearance) in appearances.withIndex()) {
            theme(appearance)
            tap("Review onboarding preferences")
            rule.onNodeWithTag("glass-dialog").assertIsDisplayed()
            capture("review-$index")
            tap("Cancel")
            await { rule.onAllNodesWithTag("glass-dialog").fetchSemanticsNodes().isEmpty() }
            assertTrue(runBlocking { repo.preferences.flow.first().onboarded })
        }
        tap("Review onboarding preferences")
        tap("Review")
        await { rule.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty() }
        tap("Get started")
        tap("Continue")
        tap("Continue")
        tap("Continue")
        tap("Done")
        await { runBlocking { repo.preferences.flow.first().onboarded } }
        assertEquals(beforeEntries, runBlocking { repo.dao.allEntries() })
        assertEquals(beforeBottles, runBlocking { repo.dao.allBottles() })
    }

    @Test
    fun resetUsesGlassAndCancelKeepsTodayWhileResetKeepsOtherDays() {
        val today = runBlocking { repo.log(321_000, "QA reset today") }
        val past = runBlocking {
            repo.log(432_000, "QA reset history", LocalDate.now().minusDays(1))
        }
        for ((index, appearance) in appearances.withIndex()) {
            theme(appearance)
            rule
                .onNodeWithTag("daily-reset")
                .performScrollTo()
                .clearFloatingNavigation(rule)
                .performClick()
            rule.onNodeWithTag("glass-dialog").assertIsDisplayed()
            rule.onNodeWithText("Reset today's intake?").assertExists()
            capture("reset-$index")
            tap("Cancel")
            await { rule.onAllNodesWithTag("glass-dialog").fetchSemanticsNodes().isEmpty() }
            assertTrue(runBlocking { repo.dao.allEntries().contains(today) })
        }
        rule.onNodeWithTag("daily-reset").clearFloatingNavigation(rule).performClick()
        tap("Reset")
        await {
            runBlocking { repo.dao.allEntries().none { it.date == LocalDate.now().toString() } }
        }
        assertTrue(runBlocking { repo.dao.allEntries().contains(past) })
    }

    @Test
    fun personalizationUsesNeutralTextAndContentExtendsBehindFloatingBar() {
        capture("home")
        rule.onNodeWithContentDescription("More").clearFloatingNavigation(rule).performClick()
        capture("personalization")
        for (appearance in appearances) {
            theme(appearance)
            rule.onNodeWithText("Preferred name").performScrollTo()
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText("Preferred name").performSemanticsAction(
                SemanticsActions.GetTextLayoutResult
            ) {
                it(layouts)
            }
            assertEquals(
                hydriColors(context, runBlocking { repo.preferences.flow.first() }).onSurface,
                layouts.single().layoutInput.style.color,
            )
            val bar = rule.onNodeWithTag("sliding-navigation").getUnclippedBoundsInRoot()
            val viewport = rule.onNodeWithTag("page-viewport").getUnclippedBoundsInRoot()
            assertTrue(
                "Content should draw behind the floating navigation",
                viewport.bottom > bar.bottom,
            )
            val notifications = rule.onNodeWithText("Notifications")
            notifications.performScrollTo()
            InstrumentationRegistry.getInstrumentation()
                .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB)
            notifications.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            rule.waitForIdle()
            assertTrue(
                "Focused controls should clear the overlay",
                notifications.getUnclippedBoundsInRoot().bottom <= bar.top,
            )
            rule.onNodeWithText("Version ${BuildConfig.VERSION_NAME}").performScrollTo().assertIsDisplayed()
            // The viewport draws behind the overlay; scroll through the reserved end padding.
            rule.onNodeWithTag("page-viewport").performTouchInput {
                swipeUp(startY = height * .6f, endY = height * .2f, durationMillis = 350)
            }
            assertTrue(
                "Bottom content must remain reachable above the bar",
                rule.onNodeWithText("Version ${BuildConfig.VERSION_NAME}").getUnclippedBoundsInRoot().bottom < bar.top,
            )
        }
        capture("floating-navigation")
    }
}
