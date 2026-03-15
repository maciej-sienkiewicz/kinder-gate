package pl.kindergate

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.kindergate.domain.model.MonitoredApp
import pl.kindergate.domain.repository.MonitoredAppsRepository

class MonitoredAppsRepositoryTest {

    private lateinit var repository: MonitoredAppsRepository

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `getEnabledPackageNames returns only enabled apps`() = runTest {
        val apps = setOf("com.tiktok.android", "com.google.youtube")
        coEvery { repository.getEnabledPackageNames() } returns apps

        val result = repository.getEnabledPackageNames()

        assertEquals(apps, result)
    }

    @Test
    fun `observeMonitoredApps emits list`() = runTest {
        val testApps = listOf(
            MonitoredApp("com.tiktok.android", "TikTok"),
            MonitoredApp("com.google.youtube", "YouTube")
        )
        coEvery { repository.observeMonitoredApps() } returns flowOf(testApps)

        val collected = mutableListOf<List<MonitoredApp>>()
        repository.observeMonitoredApps().collect { collected.add(it) }

        assertEquals(1, collected.size)
        assertEquals(2, collected.first().size)
    }

    @Test
    fun `replaceAll replaces entire list`() = runTest {
        val newApps = listOf(MonitoredApp("com.snapchat.android", "Snapchat"))
        repository.replaceAll(newApps)
        coVerify { repository.replaceAll(newApps) }
    }
}
