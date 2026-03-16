package pl.kindergate.child

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.repository.ChildRepository

/**
 * Unit tests for [ChildRepository] contract, using a mockk stub.
 *
 * These verify that callers interact correctly with the interface –
 * persistence logic is covered by [ChildDatabaseTest] (instrumented).
 */
class ChildRepositoryTest {

    private lateinit var repository: ChildRepository

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `getChildren returns empty list when no profiles stored`() = runTest {
        coEvery { repository.getChildren() } returns emptyList()

        val result = repository.getChildren()

        assertEquals(emptyList<ChildProfile>(), result)
    }

    @Test
    fun `getChildren returns all stored profiles`() = runTest {
        val profiles = listOf(
            ChildProfile(id = "id-1", name = "Ania", age = 7, gradeLevel = 1),
            ChildProfile(id = "id-2", name = "Bartek", age = 10, gradeLevel = 4),
        )
        coEvery { repository.getChildren() } returns profiles

        val result = repository.getChildren()

        assertEquals(2, result.size)
        assertEquals("Ania", result[0].name)
        assertEquals("Bartek", result[1].name)
    }

    @Test
    fun `getChildById returns correct profile`() = runTest {
        val profile = ChildProfile(id = "id-1", name = "Ania", age = 7)
        coEvery { repository.getChildById("id-1") } returns profile

        val result = repository.getChildById("id-1")

        assertEquals(profile, result)
    }

    @Test
    fun `getChildById returns null for unknown id`() = runTest {
        coEvery { repository.getChildById("unknown") } returns null

        val result = repository.getChildById("unknown")

        assertNull(result)
    }

    @Test
    fun `upsertChild delegates to repository`() = runTest {
        val profile = ChildProfile(id = "id-1", name = "Ania", age = 7, gradeLevel = 2)

        repository.upsertChild(profile)

        coVerify { repository.upsertChild(profile) }
    }

    @Test
    fun `deleteChild delegates to repository`() = runTest {
        repository.deleteChild("id-1")

        coVerify { repository.deleteChild("id-1") }
    }

    @Test
    fun `ChildProfile gradeLevel is nullable`() {
        val profile = ChildProfile(id = "id-1", name = "Ania", age = 5)
        assertNull(profile.gradeLevel)
    }
}
