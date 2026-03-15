package pl.kindergate.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.kindergate.data.local.db.entity.TamperEventEntity

@Dao
interface TamperEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TamperEventEntity)

    @Query("SELECT * FROM tamper_events WHERE detected_at_ms >= :sinceMs ORDER BY detected_at_ms DESC")
    fun observeSince(sinceMs: Long): Flow<List<TamperEventEntity>>

    @Query("SELECT COUNT(*) FROM tamper_events WHERE parent_acknowledged = 0")
    suspend fun getUnacknowledgedCount(): Int

    @Query("UPDATE tamper_events SET parent_acknowledged = 1 WHERE id = :id")
    suspend fun acknowledge(id: Long)

    @Query("DELETE FROM tamper_events WHERE detected_at_ms < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
