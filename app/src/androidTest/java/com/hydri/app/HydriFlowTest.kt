package com.hydri.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.*
import java.io.File
import java.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HydriFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @get:Rule val testName = TestName()
    private lateinit var repo: Repository
    private var original: String? = null
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun preserveQaData() {
        Assume.assumeTrue(
            "Run only on the isolated QA emulator with -e hydriQa true",
            InstrumentationRegistry.getArguments().getString("hydriQa") == "true",
        )
        repo = (context.applicationContext as HydriApplication).repository
        runBlocking {
            repo.initialize()
            original = repo.backup()
            repo.preferences.save(
                repo.preferences.flow
                    .first()
                    .copy(onboarded = true, timeFormat = "24-hour", haptics = false)
            )
        }
        rule.waitUntil(10000) {
            rule.onAllNodesWithContentDescription("Home").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun restoreQaData() {
        if (original != null) {
            capture(testName.methodName)
            runBlocking { repo.restore(original!!) }
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        // Android window/IME animations run outside Compose's virtual test clock.
        Thread.sleep(350)
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "refinement-$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun text(s: String) = rule.onNodeWithText(s)

    private fun tap(s: String) {
        // A partially visible card can report displayed while its tap center is below the footer.
        if (
            rule
                .onAllNodes(hasScrollAction() and hasAnyDescendant(hasText(s)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
            text(s).performScrollTo()
        rule.waitForIdle()
        text(s).performClick()
        rule.waitForIdle()
    }

    private fun tab(s: String) {
        dismissMessage()
        rule.onNodeWithContentDescription(s).performClick()
        rule.waitForIdle()
    }

    private fun dismissMessage() {
        val nodes = rule.onAllNodesWithContentDescription("Dismiss")
        if (nodes.fetchSemanticsNodes().isNotEmpty()) nodes[0].performClick()
        rule.waitForIdle()
    }

    private fun await(check: () -> Boolean) {
        rule.waitUntil(10000, check)
    }

    private fun entries() = runBlocking { repo.dao.allEntries() }

    @Test
    fun onboardingFitsAndPreviewsTheme() {
        runBlocking {
            repo.preferences.save(
                repo.preferences.flow.first().copy(onboarded = false, theme = "Light", oled = false)
            )
        }
        await { rule.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty() }
        val enlarged = context.resources.configuration.fontScale > 1.1f
        if (!enlarged) text("No account. No ads.").assertIsDisplayed()
        capture("onboarding-welcome")
        tap("Get started")
        text("Name (optional)").performTextReplacement("jAWad")
        text("Continue").assertIsDisplayed()
        tap("Continue")
        rule.waitUntil(10000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
        if (!enlarged) rule.onNodeWithContentDescription("OLED pure black").assertIsDisplayed()
        tap("Dark")
        fun edgeIsDark(): Boolean {
            val map = rule.onNodeWithTag("onboarding").captureToImage().toPixelMap()
            return map[2, map.height / 2].red < .1f
        }
        await { edgeIsDark() }
        rule.onNodeWithContentDescription("OLED pure black").performScrollTo().performClick()
        await {
            val map = rule.onNodeWithTag("onboarding").captureToImage().toPixelMap()
            map[2, map.height / 2].red < .005f
        }
        capture("onboarding-oled")
        tap("Continue")
        assertTrue(edgeIsDark())
        if (!enlarged) text("US fluid ounces (oz)").assertIsDisplayed()
        tap("US fluid ounces (oz)")
        tap("Back")
        tap("Light")
        await {
            val map = rule.onNodeWithTag("onboarding").captureToImage().toPixelMap()
            map[2, map.height / 2].red > .8f
        }
        tap("Follow system")
        tap("Continue")
        tap("Continue")
        if (!enlarged) text("Custom").assertIsDisplayed()
        capture("onboarding-goal")
        tap("Custom")
        text("Amount (oz)").performTextReplacement("80")
        text("Done").assertIsDisplayed()
        tap("Done")
        await { runBlocking { repo.preferences.flow.first().onboarded } }
        assertEquals("jAWad", runBlocking { repo.preferences.flow.first().name })
        assertEquals(
            Volume.parse("80", "oz"),
            runBlocking { Repository.goalFor(repo.dao.allGoals(), LocalDate.now()) },
        )
        assertEquals("oz", runBlocking { repo.preferences.flow.first().unit })
        assertTrue(runBlocking { repo.dao.allBottles().isNotEmpty() })
    }

    @Test
    fun settingsPagesDropdownAndGoal() {
        tab("More")
        val anchor = rule.onNodeWithContentDescription("Theme: Light")
        anchor.performScrollTo()
        rule.waitForIdle()
        val bounds = anchor.fetchSemanticsNode().boundsInWindow
        anchor.performClick()
        rule.waitForIdle()
        val menu = android.graphics.Rect()
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .rootInActiveWindow
            .getBoundsInScreen(menu)
        capture("menu-anchored")
        assertTrue(
            "Menu should overlap the selected value horizontally",
            menu.right > bounds.left && menu.left < bounds.right,
        )
        tap("Dark")
        await { runBlocking { repo.preferences.flow.first().theme == "Dark" } }
        rule.onNodeWithContentDescription("OLED pure black").performScrollTo().performClick()
        rule.onNodeWithTag("accent-picker").performScrollTo()
        rule.onNodeWithContentDescription("Lavender").performScrollTo().performClick()
        await {
            runBlocking {
                val s = repo.preferences.flow.first()
                s.oled && s.accent == 2
            }
        }
        capture("more-oled")
        tap("Daily hydration goal")
        text("Amount (mL)").performTextReplacement("1500")
        tap("Save goal")
        await {
            runBlocking { Repository.goalFor(repo.dao.allGoals(), LocalDate.now()) == 1_500_000L }
        }
        dismissMessage()
        for ((page, content) in
            listOf(
                "Notifications" to "Hydration reminders",
                "Hydration Guide" to "Daily total water",
                "Data & Backup" to "Create local backup",
                "About" to "Libraries & licenses",
            )) {
            tap(page)
            text(content).performScrollTo().assertIsDisplayed()
            capture(page.replace(" ", "-"))
            rule.onNodeWithContentDescription("Back to More").performClick()
            rule.waitForIdle()
        }
        text("Version 1.2.2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bottleEditorAndFavoritesControlHome() {
        tab("Bottles")
        tap("Create a bottle")
        text("Bottle name").performTextInput("QA flask")
        text("Amount (mL)").performTextReplacement("750")
        rule.onNodeWithTag("bottle-shapes").performScrollTo()
        rule.onNodeWithContentDescription("Mug").performScrollTo().performClick()
        rule.onNodeWithTag("accent-picker").performScrollTo()
        rule.onNodeWithContentDescription("Rose").performScrollTo().performClick()
        text("Save bottle").assertIsDisplayed()
        capture("bottle-editor")
        tap("Save bottle")
        await { runBlocking { repo.dao.allBottles().any { it.name == "QA flask" } } }
        val b = runBlocking { repo.dao.allBottles().first { it.name == "QA flask" } }
        assertEquals(750_000L, b.volume)
        assertEquals(5, b.icon)
        assertEquals(3, b.color)
        assertTrue(b.favorite)
        tab("Home")
        await {
            rule
                .onAllNodesWithContentDescription("Log QA flask, 750 mL")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithContentDescription("Log QA flask, 750 mL").performScrollTo().assertExists()
        tab("Bottles")
        rule.onNodeWithContentDescription("Unfavorite QA flask").performScrollTo().performClick()
        await { runBlocking { !repo.dao.allBottles().first { it.id == b.id }.favorite } }
        tab("Home")
        await {
            rule
                .onAllNodesWithContentDescription("Log QA flask, 750 mL")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        rule.onNodeWithContentDescription("Log QA flask, 750 mL").assertDoesNotExist()
        tab("Bottles")
        rule.onNodeWithContentDescription("Favorite QA flask").performScrollTo().performClick()
        await { runBlocking { repo.dao.allBottles().first { it.id == b.id }.favorite } }
        tab("Home")
        await {
            rule
                .onAllNodesWithContentDescription("Log QA flask, 750 mL")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithContentDescription("Log QA flask, 750 mL").performScrollTo().assertExists()
        runBlocking {
            repo.dao.allBottles().forEach { repo.dao.putBottle(it.copy(favorite = false)) }
        }
        await { rule.onAllNodesWithText("No favorite bottles").fetchSemanticsNodes().isNotEmpty() }
        tap("Choose favorites in Bottles")
        rule.onNodeWithContentDescription("Bottles").assertIsSelected()
        // Shared tab navigation restores this page's scroll position.
        text("Create a bottle").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun swipeUndoDailyUndoAndTodayReset() {
        val past = runBlocking { repo.log(400_000, "QA past", LocalDate.now().minusDays(1)) }
        val entry = runBlocking { repo.log(350_000, "QA swipe") }
        val row = rule.onNodeWithTag("sip-${entry.id}")
        row.performScrollTo()
        rule.waitForIdle()
        row.performTouchInput { swipeLeft(durationMillis = 400) }
        await { entries().none { it.id == entry.id } }
        rule.onNode(hasText("Undo") and hasAnyAncestor(hasTestTag("snackbar"))).performClick()
        await { entries().any { it == entry } }
        rule.onNodeWithTag("daily-undo").performScrollTo().performClick()
        await { entries().none { it.id == entry.id } }
        rule.onNodeWithTag("daily-reset").performScrollTo().performClick()
        tap("Cancel")
        assertTrue(entries().any { it.date == LocalDate.now().toString() })
        rule.onNodeWithTag("daily-reset").performClick()
        rule.onNode(hasText("Reset") and hasAnyAncestor(hasTestTag("glass-dialog"))).performClick()
        await { entries().none { it.date == LocalDate.now().toString() } }
        assertTrue(entries().any { it == past })
        rule.onNodeWithTag("daily-undo").assertIsNotEnabled()
        rule.onNodeWithTag("daily-reset").assertIsNotEnabled()
    }

    @Test
    fun customTimeEditsAndExpandableHistory() {
        tap("Custom amount")
        text("Amount (mL)").performTextReplacement("375")
        rule.onNodeWithText("Time:", substring = true).performClick()
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[1].performTextReplacement("23")
        fields[2].performTextReplacement("45")
        tap("Save entry")
        await { entries().any { it.name == "Custom water" && it.volume == 375_000L } }
        val saved = entries().first { it.name == "Custom water" && it.volume == 375_000L }
        assertEquals(LocalDate.now().toString(), saved.date)
        assertEquals(
            LocalTime.of(23, 45),
            Instant.ofEpochMilli(saved.timestamp).atZone(ZoneId.of(saved.zone)).toLocalTime(),
        )
        dismissMessage()
        tap("Custom water")
        text("Amount (mL)").performTextReplacement("500")
        rule.onNodeWithText("Time:", substring = true).performClick()
        rule.onAllNodes(hasSetTextAction())[1].performTextReplacement("0")
        rule.onAllNodes(hasSetTextAction())[2].performTextReplacement("15")
        tap("Save entry")
        await { entries().any { it.id == saved.id && it.volume == 500_000L } }
        val edited = entries().first { it.id == saved.id }
        assertEquals(saved.date, edited.date)
        assertEquals(
            LocalTime.of(0, 15),
            Instant.ofEpochMilli(edited.timestamp).atZone(ZoneId.of(edited.zone)).toLocalTime(),
        )
        val past = runBlocking { repo.log(400_000, "QA history", LocalDate.now().minusDays(1)) }
        tab("Tracker")
        text("Daily goal").assertDoesNotExist()
        val s = runBlocking { repo.preferences.flow.first() }
        val history =
            rule.onNodeWithContentDescription(
                "History ${formattedDate(LocalDate.parse(past.date),s.dateFormat)}"
            )
        history.performScrollTo()
        text("QA history").assertDoesNotExist()
        history.performClick()
        text("QA history").performScrollTo().assertIsDisplayed()
        capture("history-expanded")
        tap("QA history")
        text("Amount (mL)").performTextReplacement("600")
        tap("Save entry")
        await {
            entries().any { it.id == past.id && it.volume == 600_000L && it.date == past.date }
        }
        history.performScrollTo().performClick()
        text("QA history").assertDoesNotExist()
        text("Badges").performScrollTo().assertIsDisplayed()
        text("First Drop").performScrollTo().assertIsDisplayed()
    }
}
