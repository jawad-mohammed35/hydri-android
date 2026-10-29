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

    @Query("SELECT * FROM bottles ORDER BY position, name") fun bottles(): Flow<List<Bottle>>

    @Query("SELECT * FROM goals ORDER BY date") fun goals(): Flow<List<Goal>>

    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    suspend fun allEntries(): List<WaterEntry>

    @Query("SELECT * FROM bottles ORDER BY position, name") suspend fun allBottles(): List<Bottle>

    @Query("SELECT * FROM goals ORDER BY date") suspend fun allGoals(): List<Goal>

    @Upsert suspend fun putEntry(entry: WaterEntry)

    @Upsert suspend fun putBottle(bottle: Bottle)

    @Upsert suspend fun putGoal(goal: Goal)

    @Query("DELETE FROM entries WHERE id = :id") suspend fun deleteEntry(id: String)

    @Query("DELETE FROM bottles WHERE id = :id") suspend fun deleteBottle(id: String)

    @Query("DELETE FROM entries") suspend fun clearEntries()

    @Query("DELETE FROM bottles") suspend fun clearBottles()

    @Query("DELETE FROM goals") suspend fun clearGoals()
}

@Database(
    entities = [WaterEntry::class, Bottle::class, Goal::class],
    version = 1,
    exportSchema = true,
)
abstract class HydriDatabase : RoomDatabase() {
    abstract fun dao(): HydriDao
}
