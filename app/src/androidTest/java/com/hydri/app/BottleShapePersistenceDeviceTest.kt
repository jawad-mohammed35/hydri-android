// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.domain.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class BottleShapePersistenceDeviceTest {
    @Test
    fun legacyAndNewIdsSurviveDiskReopenBackupRestoreAndDeletedBottleSnapshots() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "hydri-shape-catalog-qa.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, HydriDatabase::class.java, name).build()
        try {
            var repo = Repository(db, PreferenceStore(context))
            repo.initialize()
            val expected =
                BottleShape.entries.map { shape ->
                    Bottle(
                        "shape-${shape.id}",
                        "Shape ${shape.id}",
                        500_000,
                        shape.id,
                        shape.id % 6,
                        true,
                        shape.id + 3,
                    )
                }
            val entries =
                expected.map { bottle ->
                    repo.dao.putBottle(bottle)
                    repo.log(bottle.volume, bottle.name, bottle = bottle)
                }
            val backup = repo.backup()
            db.close()
            db = Room.databaseBuilder(context, HydriDatabase::class.java, name).build()
            repo = Repository(db, PreferenceStore(context))
            expected.forEach { bottle ->
                assertEquals(bottle, repo.dao.allBottles().single { it.id == bottle.id })
            }
            entries.forEach { entry -> assertEquals(entry, repo.dao.entry(entry.id)) }
            expected.forEach { repo.dao.deleteBottle(it.id) }
            entries.forEach { entry ->
                assertEquals(entry.bottleIcon, repo.dao.entry(entry.id)!!.bottleIcon)
            }
            repo.restore(backup)
            expected.forEach { bottle ->
                assertEquals(bottle, repo.dao.allBottles().single { it.id == bottle.id })
            }
            entries.forEach { entry -> assertEquals(entry, repo.dao.entry(entry.id)) }
            val malformed = JSONObject(backup)
            malformed.getJSONArray("bottles").getJSONObject(0).put("icon", 14)
            try {
                repo.restore(malformed.toString())
                fail("Unknown shape accepted")
            } catch (_: IllegalArgumentException) {}
            entries.forEach { entry -> assertEquals(entry, repo.dao.entry(entry.id)) }
            assertEquals(listOf(0, 1, 2), repo.defaults().map { it.icon })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
