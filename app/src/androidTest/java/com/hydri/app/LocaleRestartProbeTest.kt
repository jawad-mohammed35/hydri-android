// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.i18n.*
import java.io.File
import org.junit.*
import org.junit.Assert.*

/** Two explicit QA-only invocations, separated by a real process stop and cold launch. */
class LocaleRestartProbeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun preferenceAcrossColdProcessRestart() {
        val arguments = InstrumentationRegistry.getArguments()
        Assume.assumeTrue(
            arguments.getString("hydriQa") == "true" && arguments.getString("localeProbe") == "true"
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = File(context.getExternalFilesDir(null), "v130-locale-probe.txt")
        if (arguments.getString("phase") == "prepare") {
            original.writeText(AppLocales.tags(context))
            rule.runOnUiThread { AppLocales.select("fr") }
            rule.waitUntil(15000) {
                AppLocales.current(context) == "fr" &&
                    rule
                        .onAllNodesWithContentDescription("Accueil")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
            }
            assertEquals("fr", AppText.locale.language)
        } else {
            assertTrue("Prepare the QA probe before killing the process", original.exists())
            assertEquals("fr", AppLocales.current(context))
            assertEquals("fr", AppText.locale.language)
            rule.onNodeWithContentDescription("Accueil").assertExists()
            assertEquals("2,50", com.hydri.app.domain.Volume.input(2_500_000, "L"))
            val prior = original.readText()
            rule.runOnUiThread { AppLocales.select(prior) }
            rule.waitUntil(15000) { AppLocales.tags(context) == prior }
        }
    }
}
