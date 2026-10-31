// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class WaterEntry(
    @PrimaryKey val id: String,
    val volume: Long,
    val timestamp: Long,
    val date: String,
    val name: String,
    val zone: String,
    val bottleId: String? = null,
    val bottleIcon: Int? = null,
    val bottleColor: Int? = null,
)

@Entity(tableName = "bottles")
data class Bottle(
    @PrimaryKey val id: String,
    val name: String,
    val volume: Long,
    val icon: Int = 1,
    val color: Int = 0,
    val favorite: Boolean = true,
    val position: Int = 0,
)

@Entity(tableName = "goals") data class Goal(@PrimaryKey val date: String, val volume: Long)

@Dao
interface HydriDao {
    @Query("SELECT * FROM entries ORDER BY timestamp DESC") fun entries(): Flow<List<WaterEntry>>

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY timestamp DESC")
    fun entriesOn(date: String): Flow<List<WaterEntry>>

    @Query("SELECT * FROM bottles ORDER BY position, name") fun bottles(): Flow<List<Bottle>>

    @Query("SELECT * FROM goals ORDER BY date") fun goals(): Flow<List<Goal>>

    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    suspend fun allEntries(): List<WaterEntry>

    @Query("SELECT * FROM entries WHERE id = :id") suspend fun entry(id: String): WaterEntry?

    @Query("SELECT * FROM bottles ORDER BY position, name") suspend fun allBottles(): List<Bottle>

    @Query("SELECT * FROM goals ORDER BY date") suspend fun allGoals(): List<Goal>

    @Upsert suspend fun putEntry(entry: WaterEntry)

    @Upsert suspend fun putBottle(bottle: Bottle)

    @Upsert suspend fun putGoal(goal: Goal)

    @Query("DELETE FROM entries WHERE id = :id") suspend fun deleteEntry(id: String)

    @Query("DELETE FROM entries WHERE date = :date") suspend fun clearEntriesOn(date: String)

    @Query("DELETE FROM bottles WHERE id = :id") suspend fun deleteBottle(id: String)

    @Query("DELETE FROM entries") suspend fun clearEntries()

    @Query("DELETE FROM bottles") suspend fun clearBottles()

    @Query("DELETE FROM goals") suspend fun clearGoals()
}

@Database(
    entities = [WaterEntry::class, Bottle::class, Goal::class],
    version = 2,
    exportSchema = true,
)
abstract class HydriDatabase : RoomDatabase() {
    abstract fun dao(): HydriDao

    companion object {
        val MIGRATION_1_2 =
            object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // Nullable snapshots preserve legacy rows without guessing their bottle.
                    db.execSQL("ALTER TABLE entries ADD COLUMN bottleId TEXT")
                    db.execSQL("ALTER TABLE entries ADD COLUMN bottleIcon INTEGER")
                    db.execSQL("ALTER TABLE entries ADD COLUMN bottleColor INTEGER")
                }
            }
    }
}
