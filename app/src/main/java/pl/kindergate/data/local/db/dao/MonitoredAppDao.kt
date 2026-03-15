package pl.kindergate.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pl.kindergate.data.local.db.entity.MonitoredAppEntity

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps ORDER BY app_label ASC")
    fun observeAll(): Flow<List<MonitoredAppEntity>>

    @Query("SELECT * FROM monitored_apps ORDER BY app_label ASC")
    suspend fun getAll(): List<MonitoredAppEntity>

    @Query("SELECT package_name FROM monitored_apps WHERE is_enabled = 1")
    suspend fun getEnabledPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: MonitoredAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<MonitoredAppEntity>)

    @Query("DELETE FROM monitored_apps WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE monitored_apps SET is_enabled = :enabled WHERE package_name = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Transaction
    suspend fun replaceAll(apps: List<MonitoredAppEntity>) {
        deleteAll()
        insertAll(apps)
    }

    @Query("DELETE FROM monitored_apps")
    suspend fun deleteAll()
}
