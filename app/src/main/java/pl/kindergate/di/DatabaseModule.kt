package pl.kindergate.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.kindergate.data.local.db.AppDatabase
import pl.kindergate.data.local.db.dao.BlockSessionDao
import pl.kindergate.data.local.db.dao.ChildDao
import pl.kindergate.data.local.db.dao.MonitoredAppDao
import pl.kindergate.data.local.db.dao.TamperEventDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // MVP only; use proper migrations in v1
            .build()

    @Provides
    fun provideMonitoredAppDao(db: AppDatabase): MonitoredAppDao = db.monitoredAppDao()

    @Provides
    fun provideBlockSessionDao(db: AppDatabase): BlockSessionDao = db.blockSessionDao()

    @Provides
    fun provideTamperEventDao(db: AppDatabase): TamperEventDao = db.tamperEventDao()

    @Provides
    fun provideChildDao(db: AppDatabase): ChildDao = db.childDao()
}
