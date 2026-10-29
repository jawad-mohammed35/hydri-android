package com.hydri.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpgradeDeviceTest {
    @Test
    fun versionOneDataSurvivesSignedUpgrade() = runBlocking {
        Assume.assumeTrue(
            "Use only the dedicated QA emulator",
            InstrumentationRegistry.getArguments().getString("hydriQa") == "true",
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val baseline = File(context.getExternalFilesDir(null), "upgrade-baseline.json")
        Assume.assumeTrue("Requires a captured 1.0.0 upgrade baseline", baseline.exists())
        val original = JSONObject(baseline.readText())
        val repo = (context.applicationContext as HydriApplication).repository
        val entries = repo.dao.allEntries().sortedBy { it.id }
        val bottles = repo.dao.allBottles().sortedBy { it.id }
        val goals = repo.dao.allGoals()
        val expectedEntries = original.getJSONArray("entries")
        assertEquals(expectedEntries.length(), entries.size)
        entries.forEachIndexed { i, e ->
            val old = expectedEntries.getJSONObject(i)
            assertEquals(old.getString("id"), e.id)
            assertEquals(old.getLong("volume"), e.volume)
            assertEquals(old.getLong("timestamp"), e.timestamp)
            assertEquals(old.getString("date"), e.date)
            assertEquals(old.getString("name"), e.name)
            assertEquals(old.getString("zone"), e.zone)
        }
        val expectedBottles = original.getJSONArray("bottles")
        assertEquals(expectedBottles.length(), bottles.size)
        bottles.forEachIndexed { i, b ->
            val old = expectedBottles.getJSONObject(i)
            assertEquals(old.getString("id"), b.id)
            assertEquals(old.getString("name"), b.name)
            assertEquals(old.getLong("volume"), b.volume)
            assertEquals(old.getInt("icon"), b.icon)
            assertEquals(old.getInt("color"), b.color)
            assertEquals(old.getInt("favorite") != 0, b.favorite)
            assertEquals(old.getInt("position"), b.position)
        }
        val expectedGoals = original.getJSONArray("goals")
        assertEquals(expectedGoals.length(), goals.size)
        goals.forEachIndexed { i, g ->
            assertEquals(expectedGoals.getJSONObject(i).getString("date"), g.date)
            assertEquals(expectedGoals.getJSONObject(i).getLong("volume"), g.volume)
        }
        val bytes = File(context.filesDir, "datastore/hydri_preferences.preferences_pb").readBytes()
        val digest =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it)
            }
        assertEquals(original.getString("preferencesSha256"), digest)
    }
}
