// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.Volume
import com.hydri.app.i18n.*
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Explicit two-phase QA probe, with an external force-stop and cold launch between invocations. */
class BottleShapeRestartProbeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aNewShapeAndItsSipSnapshotSurviveAColdProcessRestart() =
        runBlocking<Unit> {
            val args = InstrumentationRegistry.getArguments()
            Assume.assumeTrue(
                args.getString("hydriQa") == "true" && args.getString("shapeProbe") == "true"
            )
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repo = (context.applicationContext as HydriApplication).repository
            val baseline =
                File(context.getExternalFilesDir(null), "v140-shape-restart-baseline.json")
            val bottle =
                Bottle(
                    "v140-shape-restart",
                    "QA restart flask",
                    500_000,
                    11,
                    3,
                    true,
                    repo.dao.allBottles().size,
                )
            if (args.getString("phase") == "prepare") {
                baseline.writeText(repo.backup())
                repo.dao.putBottle(bottle)
                repo.log(bottle.volume, bottle.name, bottle = bottle)
                rule.waitUntil(15000) {
                    rule
                        .onAllNodesWithContentDescription(routeLabel("Home"))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            } else {
                assertTrue("Capture the QA snapshot before the cold launch", baseline.exists())
                try {
                    val saved = repo.dao.allBottles().single { it.id == bottle.id }
                    assertEquals(11, saved.icon)
                    assertEquals(3, saved.color)
                    val sip = repo.dao.allEntries().single { it.bottleId == bottle.id }
                    assertEquals(11, sip.bottleIcon)
                    assertEquals(3, sip.bottleColor)
                    rule
                        .onNodeWithContentDescription(
                            tr(
                                R.string.log_value_value,
                                saved.name,
                                Volume.format(saved.volume, repo.preferences.flow.first().unit),
                            )
                        )
                        .performScrollTo()
                        .assertExists()
                } finally {
                    repo.restore(baseline.readText())
                }
            }
        }
}
