package pl.kindergate.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.kindergate.data.local.db.dao.BlockSessionDao
import pl.kindergate.data.local.db.dao.ChildDao
import pl.kindergate.data.local.db.dao.MonitoredAppDao
import pl.kindergate.data.local.db.dao.TamperEventDao
import pl.kindergate.data.local.db.entity.BlockSessionEntity
import pl.kindergate.data.local.db.entity.ChildEntity
import pl.kindergate.data.local.db.entity.MonitoredAppEntity
import pl.kindergate.data.local.db.entity.TamperEventEntity

@Database(
    entities = [MonitoredAppEntity::class, BlockSessionEntity::class, TamperEventEntity::class, ChildEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun blockSessionDao(): BlockSessionDao
    abstract fun tamperEventDao(): TamperEventDao
    abstract fun childDao(): ChildDao

    companion object {
        const val DATABASE_NAME = "kindergate.db"
    }
}
