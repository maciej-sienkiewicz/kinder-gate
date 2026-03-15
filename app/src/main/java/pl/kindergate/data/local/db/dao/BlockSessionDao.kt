package pl.kindergate.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.kindergate.data.local.db.entity.BlockSessionEntity

@Dao
interface BlockSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: BlockSessionEntity): Long

    @Query(
        """UPDATE block_sessions
           SET acknowledged_elapsed_ms = :elapsedMs
           WHERE id = :sessionId"""
    )
    suspend fun acknowledge(sessionId: Long, elapsedMs: Long)

    @Query(
        """SELECT * FROM block_sessions
           WHERE wall_clock_triggered_ms >= :startOfDayMs
           ORDER BY wall_clock_triggered_ms DESC"""
    )
    fun observeToday(startOfDayMs: Long): Flow<List<BlockSessionEntity>>

    @Query(
        """SELECT * FROM block_sessions
           WHERE wall_clock_triggered_ms >= :startOfDayMs
           ORDER BY wall_clock_triggered_ms DESC"""
    )
    suspend fun getToday(startOfDayMs: Long): List<BlockSessionEntity>

    // Keep only last 30 days of data to avoid unlimited growth
    @Query("DELETE FROM block_sessions WHERE wall_clock_triggered_ms < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
