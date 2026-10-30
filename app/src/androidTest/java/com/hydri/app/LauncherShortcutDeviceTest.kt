package com.hydri.app

import android.app.*
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class LauncherShortcutDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repo: Repository
    private var snapshot: String? = null

    @Before
    fun setup() = runBlocking {
        Assume.assumeTrue(
            "Dedicated QA emulator only",
            InstrumentationRegistry.getArguments().getString("hydriQa") == "true",
        )
        repo = (context.applicationContext as HydriApplication).repository
        snapshot = repo.backup()
        repo.preferences.save(
            repo.preferences.flow
                .first()
                .copy(onboarded = true, unit = "oz", theme = "Dark", oled = true)
        )
        LauncherShortcuts.publish(context)
    }

    @After
    fun restore() {
        snapshot?.let { runBlocking { repo.restore(it) } }
    }

    private fun shortcut(id: String) =
        context
            .getSystemService(ShortcutManager::class.java)
            .dynamicShortcuts
            .first { it.id == id }
            .intent!!

    private fun await(test: () -> Boolean) = rule.waitUntil(10000, test)

    private fun verify(id: String) {
        val text =
            when (id) {
                "log-water" -> "Save entry"
                "create-bottle" -> "Save bottle"
                else -> "Current streak"
            }
        await { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        rule
            .onNodeWithContentDescription(
                when (id) {
                    "log-water" -> "Home"
                    "create-bottle" -> "Bottles"
                    else -> "Tracker"
                }
            )
            .assertIsSelected()
        if (id == "log-water") rule.onNodeWithText("Amount (oz)").assertExists()
    }

    @Test
    fun allNativeShortcutsOpenFromClosedActivity() {
        val shortcuts = context.getSystemService(ShortcutManager::class.java).dynamicShortcuts
        assertEquals(
            setOf("Log Water", "Create Bottle", "Tracker"),
            shortcuts.map { it.shortLabel.toString() }.toSet(),
        )
        assertEquals(3, shortcuts.size)
        assertTrue(
            shortcuts.all {
                it.isEnabled && it.intent?.component?.className == MainActivity::class.java.name
            }
        )
        for (id in listOf("log-water", "create-bottle", "tracker")) {
            ActivityScenario.launch<MainActivity>(shortcut(id)).use { verify(id) }
        }
    }

    @Test
    fun pendingShortcutWaitsForSetupAndDoesNotReplayAfterRecreation() {
        runBlocking { repo.preferences.save(repo.preferences.flow.first().copy(onboarded = false)) }
        ActivityScenario.launch<MainActivity>(shortcut("log-water")).use { scenario ->
            await { rule.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Save entry").assertDoesNotExist()
            runBlocking {
                repo.preferences.save(repo.preferences.flow.first().copy(onboarded = true))
            }
            verify("log-water")
            rule.onNodeWithText("Cancel").performClick()
            await { rule.onAllNodesWithText("Save entry").fetchSemanticsNodes().isEmpty() }
            scenario.recreate()
            await { rule.onAllNodesWithTag("daily-undo").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Save entry").assertDoesNotExist()
        }
    }

    @Test
    fun warmAndBackgroundShortcutsReuseOneActivityAndCanRepeat() {
        var created = 0
        val listener =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, state: Bundle?) {
                    if (activity is MainActivity) created++
                }

                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {}
            }
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(listener)
        try {
            ActivityScenario.launch<MainActivity>(shortcut("tracker")).use { scenario ->
                verify("tracker")
                var original: MainActivity? = null
                scenario.onActivity { original = it }
                for (id in listOf("log-water", "create-bottle", "tracker", "log-water")) {
                    context.startActivity(shortcut(id))
                    verify(id)
                    scenario.onActivity { assertSame(original, it) }
                }
                rule.onNodeWithText("Cancel").performClick()
                await { rule.onAllNodesWithText("Save entry").fetchSemanticsNodes().isEmpty() }
                scenario.onActivity { it.moveTaskToBack(true) }
                context.startActivity(shortcut("create-bottle"))
                verify("create-bottle")
                scenario.onActivity { assertSame(original, it) }
                assertEquals(
                    "Shortcuts should deliver onNewIntent, not stack activities",
                    1,
                    created,
                )
                // ActivityScenario filters lifecycle events by its original launch intent.
                scenario.onActivity { it.intent = shortcut("tracker") }
            }
        } finally {
            app.unregisterActivityLifecycleCallbacks(listener)
        }
    }
}
