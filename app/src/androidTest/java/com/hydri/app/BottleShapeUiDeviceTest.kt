// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.Volume
import com.hydri.app.i18n.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class BottleShapeUiDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repo: Repository
    private var backup: String? = null

    @Before
    fun setup() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
        repo =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                    as HydriApplication)
                .repository
        runBlocking {
            backup = repo.backup()
            repo.preferences.save(
                repo.preferences.flow.first().copy(onboarded = true, unit = "mL", haptics = false)
            )
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun restore() {
        backup?.let { runBlocking { repo.restore(it) } }
    }

    private fun tab(route: String) {
        rule.onNodeWithContentDescription(routeLabel(route)).performClick()
        rule.waitForIdle()
    }

    private fun tap(label: String) {
        val dialog = rule.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()
        val match = if (dialog) hasText(label) and hasAnyAncestor(isDialog()) else hasText(label)
        val node = rule.onNode(match)
        if (
            rule
                .onAllNodes(match and hasAnyAncestor(hasScrollAction()))
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
            node.performScrollTo()
        if (!dialog) node.clearFloatingNavigation(rule)
        node.performClick()
        rule.waitForIdle()
    }

    @Test
    fun newShapesAreSelectableSavedAndUsedByQuickAddAndSipIcons() {
        val previousBottles = runBlocking { repo.dao.allBottles() }
        for (id in 7..13) {
            tab("Bottles")
            tap(tr(R.string.create_a_bottle))
            rule.onNodeWithTag("bottle-shapes").performScrollTo()
            val label = com.hydri.app.ui.bottleNames[id]
            rule
                .onNode(
                    hasContentDescription(label) and hasAnyAncestor(hasTestTag("bottle-shapes"))
                )
                .performScrollTo()
                .performClick()
                .assertIsSelected()
            rule.onNodeWithText(tr(R.string.bottle_name)).performTextReplacement("QA shape $id")
            tap(tr(R.string.save_bottle))
            rule.waitUntil(15000) {
                runBlocking { repo.dao.allBottles().any { it.name == "QA shape $id" } }
            }
            val bottle = runBlocking { repo.dao.allBottles().single { it.name == "QA shape $id" } }
            assertEquals(id, bottle.icon)
            tab("Home")
            rule
                .onNodeWithContentDescription(
                    tr(R.string.log_value_value, bottle.name, Volume.format(bottle.volume, "mL"))
                )
                .performScrollTo()
                .clearFloatingNavigation(rule)
                .performClick()
            rule.waitUntil(15000) {
                runBlocking { repo.dao.allEntries().any { it.bottleId == bottle.id } }
            }
            val sip = runBlocking { repo.dao.allEntries().first { it.bottleId == bottle.id } }
            assertEquals(id, sip.bottleIcon)
            assertEquals(bottle.color, sip.bottleColor)
            rule
                .onNodeWithContentDescription(tr(R.string.value_sip, label))
                .performScrollTo()
                .assertExists()
        }
        previousBottles.forEach { original ->
            assertEquals(
                original,
                runBlocking { repo.dao.allBottles().single { it.id == original.id } },
            )
        }
    }
}
