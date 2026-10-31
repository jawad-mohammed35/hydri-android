// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class LanguageDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repo: Repository
    private var snapshot: String? = null
    private val codes = listOf("en", "es", "fr", "zh-Hans", "ko", "ja", "id")

    @Before
    fun setup() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
        repo = (context.applicationContext as HydriApplication).repository
        runBlocking {
            snapshot = repo.backup()
            repo.preferences.save(
                repo.preferences.flow
                    .first()
                    .copy(onboarded = true, haptics = false, reminders = true)
            )
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun restore() {
        rule.mainClock.autoAdvance = true
        snapshot?.let {
            val originalLanguage = JSONObject(it).optString("applicationLocales", "")
            runBlocking { repo.restore(it) }
            rule.waitUntil(15000) { AppLocales.tags(context) == originalLanguage }
        }
    }

    private fun tab(route: String) {
        rule.onNodeWithContentDescription(routeLabel(route)).performClick()
        rule.waitForIdle()
    }

    private fun tap(label: String) {
        val dialogOpen = rule.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()
        val node =
            if (dialogOpen) rule.onNode(hasText(label) and hasAnyAncestor(isDialog()))
            else rule.onNodeWithText(label)
        if (rule.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()) {
            if (
                rule
                    .onAllNodes(hasText(label) and hasAnyAncestor(hasScrollAction()))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            )
                node.performScrollTo()
            node.clearFloatingNavigation(rule)
            rule.mainClock.advanceTimeBy(1000)
        }
        node.performClick()
        rule.waitForIdle()
    }

    private fun choose(code: String) {
        rule.runOnUiThread { AppLocales.select(code) }
        rule.waitUntil(15000) {
            AppLocales.current(context) == code &&
                rule
                    .onAllNodesWithContentDescription(routeLabel("Home"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        rule.waitForIdle()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        Thread.sleep(250)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "v130-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertVisibleTextFits() {
        val nodes =
            rule.onAllNodes(
                androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
                    androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                ),
                useUnmergedTree = true,
            )
        val count = nodes.fetchSemanticsNodes().size
        repeat(count) { index ->
            if (nodes[index].isDisplayed()) {
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                nodes[index].performSemanticsAction(
                    androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                ) {
                    it(layouts)
                }
                layouts.forEach { result ->
                    assertFalse(
                        "Visible text overflows: ${result.layoutInput.text.text}; size=${result.size}; paragraph=${result.multiParagraph.width}x${result.multiParagraph.height}; width=${result.didOverflowWidth}; constraints=${result.layoutInput.constraints}",
                        (0 until result.lineCount).any { line ->
                            result.getLineRight(line) - result.getLineLeft(line) >
                                result.size.width + 1f
                        } || result.multiParagraph.height > result.size.height + 1f,
                    )
                }
            }
        }
    }

    private fun picker(code: String) {
        val title = tr(R.string.language)
        rule
            .onNodeWithContentDescription(
                tr(R.string.value_value, title, AppLocales.label(AppLocales.current(context)))
            )
            .performScrollTo()
            .clearFloatingNavigation(rule)
        rule.mainClock.advanceTimeBy(1000)
        rule
            .onNodeWithContentDescription(
                tr(R.string.value_value, title, AppLocales.label(AppLocales.current(context)))
            )
            .performClick()
        AppLocales.languages.keys.forEach { candidate ->
            rule
                .onNode(hasText(AppLocales.label(candidate)) and hasAnyAncestor(isPopup()))
                .assertExists()
        }
        rule
            .onNode(hasText(AppLocales.label(code)) and hasAnyAncestor(isPopup()))
            .performScrollTo()
            .performClick()
        rule.waitUntil(15000) {
            AppLocales.current(context) == code &&
                rule
                    .onAllNodesWithContentDescription(routeLabel("More"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        rule.waitForIdle()
    }

    @Test
    fun pickerAndAllPrimaryAndSecondaryScreensUseEveryLanguage() {
        runBlocking { repo.log(250_000, "Locale QA") }
        val beforeEntries = runBlocking { repo.dao.allEntries() }
        val beforeBottles = runBlocking { repo.dao.allBottles() }
        val beforeGoals = runBlocking { repo.dao.allGoals() }
        tab("More")
        codes.forEach { code ->
            picker(code)
            val expectedHome =
                mapOf(
                        "en" to "Home",
                        "es" to "Inicio",
                        "fr" to "Accueil",
                        "zh-Hans" to "首页",
                        "ko" to "홈",
                        "ja" to "ホーム",
                        "id" to "Beranda",
                    )
                    .getValue(code)
            assertEquals(expectedHome, routeLabel("Home"))
            rule.onNodeWithContentDescription(routeLabel("More")).assertIsSelected()
            tap(tr(R.string.preferred_name))
            rule.onNodeWithTag("preferred-name-input").assertExists()
            assertVisibleTextFits()
            tap(tr(R.string.cancel))
            tap(tr(R.string.daily_hydration_goal))
            rule
                .onNode(hasText(tr(R.string.daily_water_goal)) and hasAnyAncestor(isDialog()))
                .assertExists()
            assertVisibleTextFits()
            tap(tr(R.string.cancel))
            tab("Home")
            rule.onNodeWithText(tr(R.string.quick_add)).assertExists()
            assertVisibleTextFits()
            capture("home-$code")
            tap(tr(R.string.reset))
            rule
                .onNode(hasText(tr(R.string.reset_today_s_intake)) and hasAnyAncestor(isDialog()))
                .assertExists()
            assertVisibleTextFits()
            tap(tr(R.string.cancel))
            tap("Locale QA")
            rule
                .onNode(hasText(tr(R.string.edit_sip)) and hasAnyAncestor(isDialog()))
                .assertExists()
            assertVisibleTextFits()
            tap(tr(R.string.cancel))
            if (code != "en") rule.onNodeWithText("Quick add").assertDoesNotExist()
            tap(tr(R.string.custom_amount))
            rule
                .onNode(hasText(tr(R.string.log_water)) and hasAnyAncestor(isDialog()))
                .assertExists()
            rule
                .onNodeWithText(
                    tr(
                        R.string.amount_value,
                        unitLabel(runBlocking { repo.preferences.flow.first().unit }),
                    )
                )
                .assertExists()
            tap(tr(R.string.cancel))
            tab("Bottles")
            tap(tr(R.string.create_a_bottle))
            rule
                .onNode(hasText(tr(R.string.bottle_name)) and hasAnyAncestor(isDialog()))
                .assertExists()
            assertVisibleTextFits()
            rule.onNode(hasText(tr(R.string.shape)) and hasAnyAncestor(isDialog())).assertExists()
            tap(tr(R.string.cancel))
            tab("Tracker")
            rule.onNodeWithText(tr(R.string.badges)).performScrollTo().assertExists()
            tab("More")
            listOf("Notifications", "Hydration Guide", "Data & Backup", "About").forEach { page ->
                tap(routeLabel(page))
                rule.onNodeWithTag("settings-page-$page").assertExists()
                when (page) {
                    "Notifications" -> {
                        rule.onNodeWithText(tr(R.string.frequency)).assertExists()
                        tap(tr(R.string.active_hours_start))
                        rule.onNodeWithTag("glass-time-picker").assertExists()
                        rule
                            .onNode(hasText(tr(R.string.hour)) and hasAnyAncestor(isDialog()))
                            .assertExists()
                        assertVisibleTextFits()
                        tap(tr(R.string.cancel))
                    }
                    "Hydration Guide" ->
                        rule.onNodeWithText(tr(R.string.daily_total_water)).assertExists()
                    "Data & Backup" ->
                        rule.onNodeWithText(tr(R.string.create_local_backup)).assertExists()
                    "About" ->
                        rule.onNodeWithText(tr(R.string.developed_by_jawad_mohammed)).assertExists()
                }
                assertVisibleTextFits()
                rule.onNodeWithContentDescription(tr(R.string.back_to_more)).performClick()
                rule.waitUntil(15000) {
                    rule.onAllNodesWithTag("settings-page-$page").fetchSemanticsNodes().isEmpty()
                }
            }
            capture("more-$code")
            assertVisibleTextFits()
            assertEquals(beforeEntries, runBlocking { repo.dao.allEntries() })
            assertEquals(beforeBottles, runBlocking { repo.dao.allBottles() })
            assertEquals(beforeGoals, runBlocking { repo.dao.allGoals() })
        }
    }

    @Test
    fun onboardingCompletesInEveryLanguageWithLocalizedDecimalInput() {
        val entries = runBlocking { repo.dao.allEntries() }
        val bottles = runBlocking { repo.dao.allBottles() }
        codes.forEach { code ->
            choose(code)
            runBlocking {
                repo.preferences.save(repo.preferences.flow.first().copy(onboarded = false))
            }
            rule.waitUntil(15000) {
                rule.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty()
            }
            tap(tr(R.string.get_started))
            rule.onNodeWithText(tr(R.string.name_optional)).performTextReplacement("Hydri QA")
            tap(tr(R.string.continue_action))
            rule.waitUntil(15000) {
                androidx.core.view.ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
            }
            tap(tr(R.string.dark))
            tap(tr(R.string.continue_action))
            tap(tr(R.string.litres_l))
            tap(tr(R.string.continue_action))
            tap(tr(R.string.custom))
            rule
                .onNodeWithText(tr(R.string.amount_value, unitLabel("L")))
                .performTextReplacement(Volume.input(2_500_000, "L"))
            capture("onboarding-goal-$code")
            assertVisibleTextFits()
            tap(tr(R.string.done))
            rule.waitUntil(15000) {
                rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty()
            }
            rule.waitUntil(15000) {
                runBlocking {
                    Repository.goalFor(repo.dao.allGoals(), java.time.LocalDate.now()) == 2_500_000L
                }
            }
            assertEquals(
                2_500_000L,
                runBlocking { Repository.goalFor(repo.dao.allGoals(), java.time.LocalDate.now()) },
            )
            assertEquals(entries, runBlocking { repo.dao.allEntries() })
            assertEquals(bottles, runBlocking { repo.dao.allBottles() })
        }
    }

    @Test
    fun languageSurvivesRecreationAndBackupAndOldBackupsRemainValid() {
        tab("More")
        picker("es")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(15000) {
            rule.onAllNodesWithContentDescription("Más").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Más").assertIsSelected()
        val backup = runBlocking { repo.backup() }
        assertEquals("es", JSONObject(backup).getString("applicationLocales"))
        choose("ja")
        runBlocking { repo.restore(backup) }
        rule.waitUntil(15000) {
            AppLocales.current(context) == "es" &&
                rule.onAllNodesWithContentDescription("Más").fetchSemanticsNodes().isNotEmpty()
        }
        val legacy = JSONObject(backup).apply { remove("applicationLocales") }.toString()
        choose("fr")
        runBlocking { repo.restore(legacy) }
        assertEquals("fr", AppLocales.current(context))
        choose("")
        assertTrue(AppLocales.tags(context).isEmpty())
        assertEquals("en", AppText.locale.language) // isolated QA device uses English
        rule.onNodeWithContentDescription("More").assertIsSelected()
    }
}
