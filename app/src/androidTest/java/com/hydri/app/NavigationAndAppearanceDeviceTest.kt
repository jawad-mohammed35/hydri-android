package com.hydri.app

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.ui.*
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class NavigationAndAppearanceDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repo: Repository
    private var original: String? = null
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
            original = repo.backup()
            repo.preferences.save(
                repo.preferences.flow
                    .first()
                    .copy(
                        onboarded = true,
                        theme = "Light",
                        oled = false,
                        dynamicColors = false,
                        reducedMotion = false,
                        haptics = false,
                        unit = "mL",
                    )
            )
        }
        await { rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun restore() {
        rule.mainClock.autoAdvance = true
        original?.let { runBlocking { repo.restore(it) } }
    }

    private fun await(test: () -> Boolean) {
        rule.waitUntil(10000, test)
    }

    private fun tap(s: String) {
        val n = rule.onNodeWithText(s)
        if (!n.isDisplayed()) n.performScrollTo()
        n.clearFloatingNavigation(rule)
        // Finish the helper's fling before a new touch; otherwise the tap only stops scrolling.
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        n.performClick()
        rule.waitForIdle()
    }

    private fun more() {
        rule.onNodeWithContentDescription("More").clearFloatingNavigation(rule).performClick()
        rule.waitForIdle()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        Thread.sleep(300)
        val b = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "v120-$name.png").outputStream().use {
            b.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun rapidAppearanceChangesPreserveOtherPreferences() {
        val vm = androidx.lifecycle.ViewModelProvider(rule.activity)[HydriViewModel::class.java]
        rule.runOnUiThread {
            vm.settings { it.copy(theme = "Dark") }
            vm.settings { it.copy(oled = true) }
            vm.settings { it.copy(accent = 2) }
            vm.settings { it.copy(dynamicColors = true) }
        }
        await {
            runBlocking {
                val s = repo.preferences.flow.first()
                s.theme == "Dark" && s.oled && s.accent == 2 && s.dynamicColors && s.unit == "mL"
            }
        }
    }

    @Test
    fun dragNavigatesAcrossTabsWhileFingerIsDown() {
        val bar = rule.onNodeWithTag("sliding-navigation")
        bar.performTouchInput {
            down(Offset(width * .125f, centerY))
            moveTo(Offset(width * .375f, centerY), delayMillis = 180)
        }
        rule.onNodeWithContentDescription("Bottles").assertIsSelected()
        bar.performTouchInput { moveTo(Offset(width * .625f, centerY), delayMillis = 180) }
        rule.onNodeWithContentDescription("Tracker").assertIsSelected()
        bar.performTouchInput {
            moveTo(Offset(width * .875f, centerY), delayMillis = 180)
            up()
        }
        rule.onNodeWithContentDescription("More").assertIsSelected()
        bar.performTouchInput {
            down(Offset(width * .875f, centerY))
            moveTo(Offset(width * .125f, centerY), delayMillis = 200)
            up()
        }
        rule.onNodeWithContentDescription("Home").assertIsSelected()
        rule.onNodeWithContentDescription("Tracker").clearFloatingNavigation(rule).performClick()
        rule.onNodeWithContentDescription("Tracker").assertIsSelected()
        capture("sliding-navigation")
    }

    @Test
    fun secondaryPagesSlideAndSystemBackReturns() {
        more()
        val link = rule.onNodeWithText("Notifications")
        link.performScrollTo()
        link.clearFloatingNavigation(rule)
        rule.mainClock.autoAdvance = false
        link.performClick()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(80)
        val early =
            rule.onNodeWithTag("settings-page-Notifications").getUnclippedBoundsInRoot().left.value
        rule.mainClock.advanceTimeBy(700)
        val final =
            rule.onNodeWithTag("settings-page-Notifications").getUnclippedBoundsInRoot().left.value
        assertTrue(
            "Incoming page must move left from the right edge: $early -> $final",
            early > final + 20,
        )
        rule
            .onNodeWithContentDescription("Back to More")
            .clearFloatingNavigation(rule)
            .performClick()
        rule.mainClock.advanceTimeBy(80)
        val outgoing =
            rule.onNodeWithTag("settings-page-Notifications").getUnclippedBoundsInRoot().left.value
        assertTrue("Outgoing page must move right", outgoing > final)
        rule.mainClock.advanceTimeBy(700)
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("settings-page-More").assertExists()
        tap("About")
        // A real edge gesture is delivered through Android's input dispatcher.
        val metrics = context.resources.displayMetrics
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(
                "input swipe 1 ${metrics.heightPixels / 2} ${metrics.widthPixels * 7 / 10} ${metrics.heightPixels / 2} 450"
            )
            .use { fd -> java.io.FileInputStream(fd.fileDescriptor).use { it.readBytes() } }
        await { rule.onAllNodesWithTag("settings-page-More").fetchSemanticsNodes().isNotEmpty() }
        // More enters before About has finished its native predictive-back transition.
        await { rule.onAllNodesWithTag("settings-page-About").fetchSemanticsNodes().isEmpty() }
        rule.waitForIdle()
        tap("Notifications")
        await {
            rule.onAllNodesWithTag("settings-page-Notifications").fetchSemanticsNodes().isNotEmpty()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(
            300
        ) // Android's real input dispatcher is separate from the Compose test clock.
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        await { rule.onAllNodesWithTag("settings-page-More").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun reducedMotionSkipsPageSlide() {
        more()
        runBlocking {
            repo.preferences.save(repo.preferences.flow.first().copy(reducedMotion = true))
        }
        await {
            rule
                .onNodeWithContentDescription("Reduce animations")
                .fetchSemanticsNode()
                .config
                .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ToggleableState) {
                    androidx.compose.ui.state.ToggleableState.Off
                } == androidx.compose.ui.state.ToggleableState.On
        }
        val link = rule.onNodeWithText("About")
        link.performScrollTo()
        link.clearFloatingNavigation(rule)
        rule.mainClock.autoAdvance = false
        link.performClick()
        rule.mainClock.advanceTimeBy(32)
        val left = rule.onNodeWithTag("settings-page-About").getUnclippedBoundsInRoot().left.value
        assertTrue("Reduced motion should place the page immediately", kotlin.math.abs(left) < 1f)
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun guideUsesLiveUnitsAndDynamicThemePersists() {
        more()
        tap("Match Device Colors")
        await { runBlocking { repo.preferences.flow.first().dynamicColors } }
        rule.onNodeWithText("Match Device Colors").assertIsSelected()
        val s = runBlocking { repo.preferences.flow.first() }
        assertEquals(dynamicLightColorScheme(context).primary, hydriColors(context, s).primary)
        assertEquals(
            androidx.compose.ui.graphics.Color.Black,
            hydriColors(context, s.copy(theme = "Dark", oled = true)).background,
        )
        tap("Hydration Guide")
        rule.onNodeWithText("3,700 mL").assertExists()
        runBlocking { repo.preferences.save(repo.preferences.flow.first().copy(unit = "oz")) }
        await { rule.onAllNodesWithText("125.1 oz").fetchSemanticsNodes().isNotEmpty() }
        runBlocking { repo.preferences.save(repo.preferences.flow.first().copy(unit = "L")) }
        await { rule.onAllNodesWithText("3.70 L").fetchSemanticsNodes().isNotEmpty() }
        tap("Nordic · 2023")
        rule.onNodeWithText("2.50 L").assertExists()
        capture("guide-dynamic-units")
        rule
            .onNodeWithContentDescription("Back to More")
            .clearFloatingNavigation(rule)
            .performClick()
        rule.onNodeWithTag("accent-picker").performScrollTo()
        rule
            .onNodeWithContentDescription("Rose")
            .performScrollTo()
            .clearFloatingNavigation(rule)
            .performClick()
        await {
            runBlocking {
                !repo.preferences.flow.first().dynamicColors &&
                    repo.preferences.flow.first().accent == 3
            }
        }
    }
}
