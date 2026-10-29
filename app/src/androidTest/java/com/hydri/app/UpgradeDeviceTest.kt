package com.hydri.app

import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.Hydration
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.*
import org.junit.Assert.*

class UpgradeDeviceTest {
    @Test
    fun previousVersionDataSurvivesSignedUpdate() = runBlocking {
        Assume.assumeTrue(
            "Use only the dedicated QA emulator",
            InstrumentationRegistry.getArguments().getString("hydriQa") == "true",
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val baseline = File(context.getExternalFilesDir(null), "upgrade-1.1.0.json")
        Assume.assumeTrue("Requires the captured 1.1.0 baseline", baseline.exists())
        val original = JSONObject(baseline.readText())
        val repo = (context.applicationContext as HydriApplication).repository
        val current = JSONObject(repo.backup())
        fun rows(root: JSONObject, key: String) =
            root.getJSONArray(key).let { array ->
                (0 until array.length())
                    .map { i ->
                        val row = array.getJSONObject(i)
                        row.keys().asSequence().sorted().joinToString("|") { "$it=${row.get(it)}" }
                    }
                    .sorted()
            }
        listOf("entries", "bottles", "goals").forEach {
            assertEquals("Changed $it during update", rows(original, it), rows(current, it))
        }
        assertEquals(
            Settings.from(original.getJSONObject("settings")),
            repo.preferences.flow.first(),
        )
        val expectedEntries =
            original.getJSONArray("entries").let { a ->
                (0 until a.length()).map { i ->
                    val e = a.getJSONObject(i)
                    WaterEntry(
                        e.getString("id"),
                        e.getLong("volume"),
                        e.getLong("timestamp"),
                        e.getString("date"),
                        e.getString("name"),
                        e.getString("zone"),
                    )
                }
            }
        val expectedGoals =
            original.getJSONArray("goals").let { a ->
                (0 until a.length()).map { i ->
                    val g = a.getJSONObject(i)
                    Goal(g.getString("date"), g.getLong("volume"))
                }
            }
        assertEquals(
            Hydration.statistics(
                Repository.totals(expectedEntries, expectedGoals),
                LocalDate.now(),
            ),
            Hydration.statistics(
                Repository.totals(repo.dao.allEntries(), repo.dao.allGoals()),
                LocalDate.now(),
            ),
        )
    }
}
