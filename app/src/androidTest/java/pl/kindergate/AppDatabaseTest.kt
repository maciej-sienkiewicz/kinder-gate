package pl.kindergate

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.kindergate.data.local.db.AppDatabase
import pl.kindergate.data.local.db.dao.BlockSessionDao
import pl.kindergate.data.local.db.dao.MonitoredAppDao
import pl.kindergate.data.local.db.entity.BlockSessionEntity
import pl.kindergate.data.local.db.entity.MonitoredAppEntity
import java.io.IOException

/**
 * Instrumented tests for Room database.
 * Run on a real device or emulator – cannot run on JVM (Room uses SQLite internals).
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var monitoredAppDao: MonitoredAppDao
    private lateinit var blockSessionDao: BlockSessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // OK for tests only
            .build()
        monitoredAppDao = db.monitoredAppDao()
        blockSessionDao = db.blockSessionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadMonitoredApp() = runTest {
        val app = MonitoredAppEntity("com.tiktok.android", "TikTok")
        monitoredAppDao.insert(app)

        val apps = monitoredAppDao.getAll()
        assertEquals(1, apps.size)
        assertEquals("com.tiktok.android", apps.first().packageName)
    }

    @Test
    fun replaceAllReplacesExistingApps() = runTest {
        monitoredAppDao.insert(MonitoredAppEntity("com.tiktok.android", "TikTok"))
        monitoredAppDao.insert(MonitoredAppEntity("com.google.youtube", "YouTube"))

        val newApps = listOf(MonitoredAppEntity("com.snapchat.android", "Snapchat"))
        monitoredAppDao.replaceAll(newApps)

        val result = monitoredAppDao.getAll()
        assertEquals(1, result.size)
        assertEquals("com.snapchat.android", result.first().packageName)
    }

    @Test
    fun getEnabledPackageNamesOnlyReturnsEnabled() = runTest {
        monitoredAppDao.insert(MonitoredAppEntity("com.tiktok.android", "TikTok", isEnabled = true))
        monitoredAppDao.insert(MonitoredAppEntity("com.google.youtube", "YouTube", isEnabled = false))

        val enabled = monitoredAppDao.getEnabledPackageNames()
        assertEquals(1, enabled.size)
        assertEquals("com.tiktok.android", enabled.first())
    }

    @Test
    fun insertBlockSessionAndAcknowledge() = runTest {
        val session = BlockSessionEntity(
            packageName = "com.tiktok.android",
            elapsedRealtimeTriggeredMs = 100_000L,
            wallClockTriggeredMs = System.currentTimeMillis()
        )
        val id = blockSessionDao.insert(session)

        blockSessionDao.acknowledge(id, 165_000L)

        val today = blockSessionDao.getToday(System.currentTimeMillis() - 86_400_000L)
        assertEquals(1, today.size)
        assertEquals(165_000L, today.first().acknowledgedAtElapsedMs)
    }

    @Test
    fun blockSessionAcknowledgedAtIsNullByDefault() = runTest {
        val session = BlockSessionEntity(
            packageName = "com.tiktok.android",
            elapsedRealtimeTriggeredMs = 100_000L,
            wallClockTriggeredMs = System.currentTimeMillis()
        )
        blockSessionDao.insert(session)

        val today = blockSessionDao.getToday(System.currentTimeMillis() - 86_400_000L)
        assertNull(today.first().acknowledgedAtElapsedMs)
    }
}
