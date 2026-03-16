package pl.kindergate.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType
import pl.kindergate.domain.repository.ChildRepository
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.usecase.GetChildByIdUseCase
import pl.kindergate.domain.usecase.UpsertChildUseCase
import pl.kindergate.feature.settings.CategoryConfigViewModel

/**
 * Unit tests for [CategoryConfigViewModel].
 *
 * Verifies:
 *   - Tile states are initialised from the profile's enabled sets.
 *   - Toggling a tile flips its [isEnabled] in [uiState].
 *   - [UpsertChildUseCase] is called with the correctly updated profile.
 *   - When all tiles are ON the stored set is empty (future-proof invariant).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var configRepository: ConfigRepository
    private lateinit var childRepository: ChildRepository
    private lateinit var getChildByIdUseCase: GetChildByIdUseCase
    private lateinit var upsertChildUseCase: UpsertChildUseCase

    private val childId = "child-1"
    private val baseProfile = ChildProfile(
        id = childId,
        name = "Ania",
        age = 7,
        gradeLevel = 1,
        enabledSubjects = emptySet(),   // all enabled
        enabledTaskTypes = emptySet(),  // all enabled
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        configRepository = mockk(relaxed = true)
        childRepository = mockk(relaxed = true)
        getChildByIdUseCase = mockk()
        upsertChildUseCase = mockk(relaxed = true)

        coEvery { configRepository.getSelectedChildId() } returns childId
        coEvery { getChildByIdUseCase(childId) } returns baseProfile
        coEvery { childRepository.getChildren() } returns listOf(baseProfile)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = CategoryConfigViewModel(
        configRepository = configRepository,
        getChildByIdUseCase = getChildByIdUseCase,
        upsertChildUseCase = upsertChildUseCase,
        childRepository = childRepository,
    )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has all subject tiles enabled when profile has empty set`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        val tiles = vm.uiState.value.subjectTiles
        assertTrue("Should have at least one subject tile", tiles.isNotEmpty())
        assertTrue("All tiles should be enabled", tiles.all { it.isEnabled })
    }

    @Test
    fun `initial state has all task type tiles enabled when profile has empty set`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        val tiles = vm.uiState.value.taskTypeTiles
        assertTrue("Should have at least one task type tile", tiles.isNotEmpty())
        assertTrue("All tiles should be enabled", tiles.all { it.isEnabled })
    }

    @Test
    fun `initial state reflects restricted profile`() = runTest {
        val restricted = baseProfile.copy(
            enabledSubjects = setOf(TaskSubject.MATH),
            enabledTaskTypes = setOf(TaskType.SIMPLE_ADDITION, TaskType.MULTIPLICATION),
        )
        coEvery { getChildByIdUseCase(childId) } returns restricted

        val vm = buildViewModel()
        advanceUntilIdle()

        val subjectTiles = vm.uiState.value.subjectTiles
        assertTrue(subjectTiles.first { it.subject == TaskSubject.MATH }.isEnabled)
        assertFalse(subjectTiles.first { it.subject == TaskSubject.WRITING }.isEnabled)

        val typeTiles = vm.uiState.value.taskTypeTiles
        assertTrue(typeTiles.first { it.taskType == TaskType.SIMPLE_ADDITION }.isEnabled)
        assertFalse(typeTiles.first { it.taskType == TaskType.LETTER_TRACING }.isEnabled)
    }

    @Test
    fun `childName is populated from profile`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("Ania", vm.uiState.value.childName)
    }

    // ── Subject toggle ────────────────────────────────────────────────────────

    @Test
    fun `toggling subject flips tile isEnabled in uiState`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        val wasEnabled = vm.uiState.value.subjectTiles
            .first { it.subject == TaskSubject.MATH }.isEnabled

        vm.onSubjectToggled(TaskSubject.MATH)
        advanceUntilIdle()

        val nowEnabled = vm.uiState.value.subjectTiles
            .first { it.subject == TaskSubject.MATH }.isEnabled

        assertEquals(!wasEnabled, nowEnabled)
    }

    @Test
    fun `toggling subject calls upsertChild with updated enabledSubjects`() = runTest {
        // Start with MATH+WRITING enabled (empty set)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onSubjectToggled(TaskSubject.WRITING) // disable WRITING
        advanceUntilIdle()

        val slot = slot<ChildProfile>()
        coVerify { upsertChildUseCase(capture(slot)) }

        val saved = slot.captured
        assertTrue("MATH should remain enabled", TaskSubject.MATH in saved.enabledSubjects)
        assertFalse("WRITING should be disabled", TaskSubject.WRITING in saved.enabledSubjects)
    }

    // ── TaskType toggle ───────────────────────────────────────────────────────

    @Test
    fun `toggling task type flips tile isEnabled in uiState`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        val wasEnabled = vm.uiState.value.taskTypeTiles
            .first { it.taskType == TaskType.LETTER_TRACING }.isEnabled

        vm.onTaskTypeToggled(TaskType.LETTER_TRACING)
        advanceUntilIdle()

        val nowEnabled = vm.uiState.value.taskTypeTiles
            .first { it.taskType == TaskType.LETTER_TRACING }.isEnabled

        assertEquals(!wasEnabled, nowEnabled)
    }

    @Test
    fun `toggling task type calls upsertChild with updated enabledTaskTypes`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onTaskTypeToggled(TaskType.LETTER_TRACING) // disable it
        advanceUntilIdle()

        val slot = slot<ChildProfile>()
        coVerify { upsertChildUseCase(capture(slot)) }

        assertFalse(
            "LETTER_TRACING should not be in saved set",
            TaskType.LETTER_TRACING in slot.captured.enabledTaskTypes,
        )
    }

    // ── "All enabled" invariant ───────────────────────────────────────────────

    @Test
    fun `when all tiles are enabled the stored enabledSubjects set is empty`() = runTest {
        // Start with restricted profile, then re-enable the disabled subject
        val restricted = baseProfile.copy(enabledSubjects = setOf(TaskSubject.MATH))
        coEvery { getChildByIdUseCase(childId) } returns restricted

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onSubjectToggled(TaskSubject.WRITING) // re-enable → now all ON
        advanceUntilIdle()

        val slot = slot<ChildProfile>()
        coVerify { upsertChildUseCase(capture(slot)) }

        assertTrue(
            "enabledSubjects should be empty when all subjects are ON",
            slot.captured.enabledSubjects.isEmpty(),
        )
    }
}
