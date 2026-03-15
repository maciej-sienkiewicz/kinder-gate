package pl.kindergate.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.kindergate.data.local.db.dao.BlockSessionDao
import pl.kindergate.data.local.db.dao.MonitoredAppDao
import pl.kindergate.data.local.db.dao.TamperEventDao
import pl.kindergate.data.local.db.entity.BlockSessionEntity
import pl.kindergate.data.local.db.entity.MonitoredAppEntity
import pl.kindergate.data.local.db.entity.TamperEventEntity

@Database(
    entities = [MonitoredAppEntity::class, BlockSessionEntity::class, TamperEventEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun blockSessionDao(): BlockSessionDao
    abstract fun tamperEventDao(): TamperEventDao

    companion object {
        const val DATABASE_NAME = "kindergate.db"
    }
}
