package pl.kindergate.child

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.kindergate.data.local.db.AppDatabase
import pl.kindergate.data.local.db.dao.ChildDao
import pl.kindergate.data.local.db.entity.ChildEntity
import pl.kindergate.data.repository.ChildRepositoryImpl
import pl.kindergate.domain.model.ChildProfile
import java.io.IOException

/**
 * Instrumented Room tests for the child domain.
 *
 * Covers both [ChildDao] (raw SQL) and [ChildRepositoryImpl] (mapping layer).
 * Must run on a device or emulator – Room uses SQLite internals unavailable on JVM.
 */
@RunWith(AndroidJUnit4::class)
class ChildDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ChildDao
    private lateinit var repository: ChildRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.childDao()
        repository = ChildRepositoryImpl(dao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // ── ChildDao tests ──────────────────────────────────────────────────────

    @Test
    fun dao_insertAndRetrieve() = runTest {
        val entity = ChildEntity(id = "id-1", name = "Ania", age = 7, gradeLevel = 1)
        dao.upsert(entity)

        val result = dao.getById("id-1")

        assertNotNull(result)
        assertEquals("Ania", result!!.name)
        assertEquals(7, result.age)
        assertEquals(1, result.gradeLevel)
    }

    @Test
    fun dao_upsertOverwritesExisting() = runTest {
        dao.upsert(ChildEntity(id = "id-1", name = "Ania", age = 7, gradeLevel = null))
        dao.upsert(ChildEntity(id = "id-1", name = "Ania Z.", age = 8, gradeLevel = 2))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("Ania Z.", all.first().name)
        assertEquals(8, all.first().age)
    }

    @Test
    fun dao_getAll_orderedByCreatedAtMs() = runTest {
        // Insert with explicit timestamps to guarantee order
        dao.upsert(ChildEntity(id = "id-1", name = "First", age = 6, gradeLevel = null, createdAtMs = 1_000L))
        dao.upsert(ChildEntity(id = "id-2", name = "Second", age = 8, gradeLevel = null, createdAtMs = 2_000L))

        val all = dao.getAll()
        assertEquals("First", all[0].name)
        assertEquals("Second", all[1].name)
    }

    @Test
    fun dao_getById_returnsNull_forUnknownId() = runTest {
        val result = dao.getById("does-not-exist")
        assertNull(result)
    }

    @Test
    fun dao_delete_removesRecord() = runTest {
        dao.upsert(ChildEntity(id = "id-1", name = "Ania", age = 7, gradeLevel = null))
        dao.delete("id-1")

        assertNull(dao.getById("id-1"))
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun dao_gradeLevelNullable() = runTest {
        dao.upsert(ChildEntity(id = "id-1", name = "Ania", age = 5, gradeLevel = null))
        val result = dao.getById("id-1")!!
        assertNull(result.gradeLevel)
    }

    // ── ChildRepositoryImpl tests ───────────────────────────────────────────

    @Test
    fun repository_getChildren_emptyInitially() = runTest {
        val children = repository.getChildren()
        assertEquals(emptyList<ChildProfile>(), children)
    }

    @Test
    fun repository_upsert_thenGetById() = runTest {
        val profile = ChildProfile(id = "id-1", name = "Bartek", age = 10, gradeLevel = 4)
        repository.upsertChild(profile)

        val result = repository.getChildById("id-1")
        assertNotNull(result)
        assertEquals(profile, result)
    }

    @Test
    fun repository_upsert_preservesCreatedAtMs() = runTest {
        val profile = ChildProfile(id = "id-1", name = "Bartek", age = 10)
        repository.upsertChild(profile)
        val firstCreatedAt = dao.getById("id-1")!!.createdAtMs

        // Update
        repository.upsertChild(profile.copy(name = "Bartek Updated"))
        val secondCreatedAt = dao.getById("id-1")!!.createdAtMs

        assertEquals(firstCreatedAt, secondCreatedAt)
    }

    @Test
    fun repository_delete_removesProfile() = runTest {
        val profile = ChildProfile(id = "id-1", name = "Ania", age = 7)
        repository.upsertChild(profile)
        repository.deleteChild("id-1")

        assertNull(repository.getChildById("id-1"))
        assertEquals(0, repository.getChildren().size)
    }

    @Test
    fun repository_getChildren_multipleSortedByCreation() = runTest {
        // Insert with small sleep to ensure distinct timestamps
        val p1 = ChildProfile(id = "id-1", name = "Alpha", age = 6)
        val p2 = ChildProfile(id = "id-2", name = "Beta", age = 8)
        dao.upsert(ChildEntity.fromDomain(p1, createdAtMs = 100L))
        dao.upsert(ChildEntity.fromDomain(p2, createdAtMs = 200L))

        val children = repository.getChildren()
        assertEquals(2, children.size)
        assertEquals("Alpha", children[0].name)
        assertEquals("Beta", children[1].name)
    }
}
