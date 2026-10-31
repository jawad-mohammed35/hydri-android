// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import java.time.*
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class EntryPolicyDeviceTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun isolated() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
    }

    private suspend fun denied(block: suspend () -> Unit) {
        try {
            block()
            fail("A historical mutation was accepted")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun queuedWritesRecheckTheDateAfterWaitingForTheDatabase() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HydriDatabase::class.java).build()
        val before = ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"))
        val clock = AtomicReference(before)
        val repo = Repository(db, PreferenceStore(context)) { clock.get() }
        try {
            var locked = CompletableDeferred<Unit>()
            var release = CompletableDeferred<Unit>()
            var blocker =
                launch(Dispatchers.IO) {
                    db.withTransaction {
                        locked.complete(Unit)
                        release.await()
                    }
                }
            locked.await()
            val queuedLog =
                async(start = CoroutineStart.UNDISPATCHED) {
                    runCatching {
                        repo.log(
                            250_000,
                            "Queued before midnight",
                            before.toLocalDate(),
                            before.toInstant().toEpochMilli(),
                        )
                    }
                }
            clock.set(before.plusSeconds(2))
            release.complete(Unit)
            blocker.join()
            assertTrue(queuedLog.await().exceptionOrNull() is IllegalArgumentException)
            assertTrue(repo.dao.allEntries().isEmpty())

            val old =
                WaterEntry(
                    "old",
                    250_000,
                    before.toInstant().toEpochMilli(),
                    before.toLocalDate().toString(),
                    "Old",
                    "UTC",
                )
            val current =
                old.copy(
                    id = "new",
                    date = before.plusDays(1).toLocalDate().toString(),
                    timestamp = before.plusSeconds(2).toInstant().toEpochMilli(),
                )
            repo.dao.putEntry(old)
            repo.dao.putEntry(current)
            clock.set(before)
            locked = CompletableDeferred()
            release = CompletableDeferred()
            blocker =
                launch(Dispatchers.IO) {
                    db.withTransaction {
                        locked.complete(Unit)
                        release.await()
                    }
                }
            locked.await()
            val queuedReset = async(start = CoroutineStart.UNDISPATCHED) { repo.resetToday() }
            clock.set(before.plusSeconds(2))
            release.complete(Unit)
            blocker.join()
            queuedReset.await()
            assertEquals(listOf(old), repo.dao.allEntries())
        } finally {
            db.close()
        }
    }

    @Test
    fun midnightRejectsStaleEditsDeletesLogsAndUndoWithoutChangingHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HydriDatabase::class.java).build()
        var clock = ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"))
        val repo = Repository(db, PreferenceStore(context)) { clock }
        try {
            val date = clock.toLocalDate()
            val row = repo.log(250_000, "Clock test", date, clock.toInstant().toEpochMilli())
            repo.updateEntry(row.id, 300_000, row.timestamp)
            val deleted = repo.log(100_000, "Pending Undo", date, row.timestamp)
            assertEquals(deleted, repo.deleteEntry(deleted.id))
            val protected = repo.dao.allEntries()
            clock = clock.plusSeconds(2)
            denied { repo.log(250_000, "Backdate", date, row.timestamp) }
            denied { repo.log(250_000, "Forged timestamp", clock.toLocalDate(), row.timestamp) }
            denied {
                repo.log(
                    250_000,
                    "Future",
                    clock.toLocalDate().plusDays(1),
                    clock.toInstant().toEpochMilli(),
                )
            }
            denied { repo.updateEntry(row.id, 900_000, row.timestamp) }
            denied { repo.deleteEntry(row.id) }
            denied { repo.restoreDeletedEntry(deleted) }
            assertEquals(protected, repo.dao.allEntries())
            val current =
                repo.log(400_000, "Today", clock.toLocalDate(), clock.toInstant().toEpochMilli())
            repo.updateEntry(current.id, 450_000, current.timestamp)
            val removed = repo.deleteEntry(current.id)!!
            repo.restoreDeletedEntry(removed)
            repo.resetToday()
            assertEquals(protected, repo.dao.allEntries())
            // A fresh repository cannot bypass the date policy after process recreation.
            val reopened = Repository(db, PreferenceStore(context)) { clock }
            denied { reopened.deleteEntry(row.id) }
            assertEquals(protected, reopened.dao.allEntries())
        } finally {
            db.close()
        }
    }

    @Test
    fun editsPreserveDateZoneBottleSnapshotsAndRejectMovingToAnotherDate() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HydriDatabase::class.java).build()
        var clock = ZonedDateTime.of(2026, 11, 1, 23, 55, 0, 0, ZoneId.of("Asia/Tokyo"))
        val repo = Repository(db, PreferenceStore(context)) { clock }
        try {
            val bottle = Bottle("fixture", "Fixture", 500_000, 5, 3)
            val row =
                repo.log(
                    500_000,
                    bottle.name,
                    clock.toLocalDate(),
                    clock.toInstant().toEpochMilli(),
                    bottle,
                )
            val time = clock.minusHours(1).toInstant().toEpochMilli()
            repo.updateEntry(row.id, 600_000, time)
            assertEquals(row.copy(volume = 600_000, timestamp = time), repo.dao.entry(row.id))
            denied {
                repo.updateEntry(row.id, 600_000, clock.minusDays(1).toInstant().toEpochMilli())
            }
            clock = clock.withZoneSameInstant(ZoneId.of("Pacific/Auckland"))
            assertEquals(LocalDate.of(2026, 11, 2), clock.toLocalDate())
            denied { repo.deleteEntry(row.id) }
            clock = clock.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
            assertNotNull(repo.deleteEntry(row.id))
        } finally {
            db.close()
        }
    }
}
