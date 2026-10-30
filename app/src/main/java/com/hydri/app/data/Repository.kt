// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.data

import androidx.room.withTransaction
import com.hydri.app.domain.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*

class Repository(val db: HydriDatabase, val preferences: PreferenceStore) {
    val dao = db.dao()
    private val writes = Mutex()

    suspend fun initialize() =
        writes.withLock {
            if (dao.allGoals().isEmpty())
                db.withTransaction {
                    dao.putGoal(Goal(LocalDate.now().toString(), 2_500_000))
                    defaults().forEach { dao.putBottle(it) }
                }
        }

    fun defaults() =
        listOf(
            Bottle("glass", "Everyday glass", 250_000, 0, 0, true, 0),
            Bottle("bottle", "Water bottle", 500_000, 1, 1, true, 1),
            Bottle("large", "Large bottle", 1_000_000, 2, 2, true, 2),
        )

    suspend fun log(
        volume: Long,
        name: String,
        date: LocalDate = LocalDate.now(),
        timestamp: Long = System.currentTimeMillis(),
        bottle: Bottle? = null,
    ): WaterEntry =
        writes.withLock {
            require(volume in 1_000..20_000_000 && date <= LocalDate.now())
            val entry =
                WaterEntry(
                    UUID.randomUUID().toString(),
                    volume,
                    timestamp,
                    date.toString(),
                    name.take(60),
                    ZoneId.systemDefault().id,
                    bottle?.id,
                    bottle?.icon,
                    bottle?.color,
                )
            db.withTransaction {
                if (dao.allGoals().none { it.date <= date.toString() })
                    dao.putGoal(
                        Goal(date.toString(), dao.allGoals().firstOrNull()?.volume ?: 2_500_000)
                    )
                dao.putEntry(entry)
            }
            entry
        }

    suspend fun goal(volume: Long) {
        require(volume in 1_000..20_000_000)
        dao.putGoal(Goal(LocalDate.now().toString(), volume))
    }

    suspend fun reorder(bottles: List<Bottle>) =
        db.withTransaction {
            bottles.forEachIndexed { i, b -> dao.putBottle(b.copy(position = i)) }
        }

    suspend fun deleteHistory() =
        db.withTransaction {
            val goal = goalFor(dao.allGoals(), LocalDate.now())
            dao.clearEntries()
            dao.clearGoals()
            dao.putGoal(Goal(LocalDate.now().toString(), goal))
        }

    suspend fun reset() =
        writes.withLock {
            db.withTransaction {
                dao.clearEntries()
                dao.clearBottles()
                dao.clearGoals()
                dao.putGoal(Goal(LocalDate.now().toString(), 2_500_000))
                defaults().forEach { dao.putBottle(it) }
            }
            preferences.save(Settings())
        }

    suspend fun backup(): String =
        db.withTransaction {
            JSONObject()
                .apply {
                    put("format", "hydri")
                    put("version", 1)
                    put("settings", preferences.flow.first().json())
                    put(
                        "entries",
                        JSONArray().apply {
                            dao.allEntries().forEach { e ->
                                put(
                                    JSONObject().apply {
                                        put("id", e.id)
                                        put("volume", e.volume)
                                        put("timestamp", e.timestamp)
                                        put("date", e.date)
                                        put("name", e.name)
                                        put("zone", e.zone)
                                        put("bottleId", e.bottleId ?: JSONObject.NULL)
                                        put("bottleIcon", e.bottleIcon ?: JSONObject.NULL)
                                        put("bottleColor", e.bottleColor ?: JSONObject.NULL)
                                    }
                                )
                            }
                        },
                    )
                    put(
                        "bottles",
                        JSONArray().apply {
                            dao.allBottles().forEach { b ->
                                put(
                                    JSONObject().apply {
                                        put("id", b.id)
                                        put("name", b.name)
                                        put("volume", b.volume)
                                        put("icon", b.icon)
                                        put("color", b.color)
                                        put("favorite", b.favorite)
                                        put("position", b.position)
                                    }
                                )
                            }
                        },
                    )
                    put(
                        "goals",
                        JSONArray().apply {
                            dao.allGoals().forEach { g ->
                                put(
                                    JSONObject().apply {
                                        put("date", g.date)
                                        put("volume", g.volume)
                                    }
                                )
                            }
                        },
                    )
                }
                .toString(2)
        }

    /**
     * Parse and validate every record before replacing anything. DB replacement is one transaction.
     */
    suspend fun restore(text: String) =
        writes.withLock {
            require(text.length <= 10_000_000) { "Backup is too large (10 MB maximum)." }
            val root = JSONObject(text)
            require(root.getString("format") == "hydri" && root.getInt("version") == 1) {
                "Unsupported backup format."
            }
            val settings = Settings.from(root.getJSONObject("settings"))
            fun array(key: String) =
                root.getJSONArray(key).let { a ->
                    require(a.length() <= 100_000)
                    (0 until a.length()).map { a.getJSONObject(it) }
                }
            fun volume(j: JSONObject): Long =
                j.getLong("volume").also { require(it in 1_000..20_000_000) }
            fun name(j: JSONObject) = j.getString("name").also { require(it.length in 1..60) }
            fun idValue(value: String) = value.also { require(it.length in 1..100) }
            fun id(j: JSONObject) = idValue(j.getString("id"))
            val entries =
                array("entries").map { j ->
                    WaterEntry(
                        id(j),
                        volume(j),
                        j.getLong("timestamp").also { require(it in 0..4_102_444_800_000) },
                        LocalDate.parse(j.getString("date"))
                            .also { require(it.year in 1970..2100 && it <= LocalDate.now()) }
                            .toString(),
                        name(j),
                        ZoneId.of(j.getString("zone")).id,
                        if (j.isNull("bottleId")) null else idValue(j.getString("bottleId")),
                        if (j.isNull("bottleIcon")) null
                        else j.getInt("bottleIcon").also { require(it in 0..6) },
                        if (j.isNull("bottleColor")) null
                        else j.getInt("bottleColor").also { require(it in 0..5) },
                    )
                }
            val bottles =
                array("bottles").map { j ->
                    Bottle(
                        id(j),
                        name(j),
                        volume(j),
                        j.getInt("icon").also { require(it in 0..6) },
                        j.getInt("color").also { require(it in 0..5) },
                        j.getBoolean("favorite"),
                        j.getInt("position").also { require(it >= 0) },
                    )
                }
            val goals =
                array("goals").map { j ->
                    Goal(
                        LocalDate.parse(j.getString("date"))
                            .also { require(it.year in 1970..2100 && it <= LocalDate.now()) }
                            .toString(),
                        volume(j),
                    )
                }
            require(
                goals.isNotEmpty() &&
                    entries.map { it.id }.toSet().size == entries.size &&
                    bottles.map { it.id }.toSet().size == bottles.size &&
                    goals.map { it.date }.toSet().size == goals.size
            )
            require(entries.all { e -> goals.any { it.date <= e.date } })
            val oldSettings = preferences.flow.first()
            try {
                db.withTransaction {
                    dao.clearEntries()
                    dao.clearBottles()
                    dao.clearGoals()
                    entries.forEach { dao.putEntry(it) }
                    bottles.forEach { dao.putBottle(it) }
                    goals.forEach { dao.putGoal(it) }
                    preferences.save(settings)
                }
            } catch (e: Exception) {
                preferences.save(oldSettings)
                throw e
            }
        }

    suspend fun csv(): String = buildString {
        append("id,local_date,timestamp_utc,timezone,name,millilitres,goal_millilitres\r\n")
        val goals = dao.allGoals()
        dao.allEntries().forEach { e ->
            fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
            // Prefix spreadsheet formulas in user-supplied names.
            val safeName =
                if (e.name.firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'${e.name}"
                else e.name
            append(
                listOf(
                        e.id,
                        e.date,
                        Instant.ofEpochMilli(e.timestamp).toString(),
                        e.zone,
                        safeName,
                        (e.volume / 1000.0).toString(),
                        (goalFor(goals, LocalDate.parse(e.date)) / 1000.0).toString(),
                    )
                    .joinToString(",") { q(it) }
            )
            append("\r\n")
        }
    }

    companion object {
        fun goalFor(goals: List<Goal>, date: LocalDate) =
            goals.lastOrNull { it.date <= date.toString() }?.volume
                ?: goals.firstOrNull()?.volume
                ?: 2_500_000

        fun totals(entries: List<WaterEntry>, goals: List<Goal>): List<DayTotal> =
            entries
                .groupBy { it.date }
                .map { (d, e) ->
                    DayTotal(
                        LocalDate.parse(d),
                        e.sumOf { it.volume },
                        goalFor(goals, LocalDate.parse(d)),
                    )
                }
    }
}
