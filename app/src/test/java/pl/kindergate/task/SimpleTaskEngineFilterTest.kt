package pl.kindergate.task

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.kindergate.data.engine.DivisionEvaluator
import pl.kindergate.data.engine.ExpressionEvaluator
import pl.kindergate.data.engine.LetterTracingEvaluator
import pl.kindergate.data.engine.MultiplicationEvaluator
import pl.kindergate.data.engine.SimpleAdditionEvaluator
import pl.kindergate.data.engine.SimpleTaskEngine
import pl.kindergate.data.engine.SubtractionEvaluator
import pl.kindergate.data.repository.InMemoryTaskRepository
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType
import pl.kindergate.domain.repository.ChildRepository

/**
 * Integration tests verifying that [SimpleTaskEngine] honours the allow-lists
 * stored in [ChildProfile.enabledSubjects] and [ChildProfile.enabledTaskTypes].
 *
 * Uses the real [InMemoryTaskRepository] catalog (no mocks for the task side) but
 * a simple stub [ChildRepository] whose profile can be swapped per test.
 */
class SimpleTaskEngineFilterTest {

    private lateinit var engine: SimpleTaskEngine
    private var activeProfile: ChildProfile? = null

    private val stubChildRepo = object : ChildRepository {
        override suspend fun getChildren() = listOfNotNull(activeProfile)
        override suspend fun getChildById(id: String): ChildProfile? = activeProfile
        override suspend fun upsertChild(child: ChildProfile) = Unit
        override suspend fun deleteChild(id: String) = Unit
    }

    @Before
    fun setUp() {
        val repo = InMemoryTaskRepository()
        engine = SimpleTaskEngine(
            taskRepository = repo,
            childRepository = stubChildRepo,
            evaluators = setOf(
                SimpleAdditionEvaluator(),
                SubtractionEvaluator(),
                MultiplicationEvaluator(),
                DivisionEvaluator(),
                ExpressionEvaluator(),
                LetterTracingEvaluator(),
            ),
        )
    }

    // ── Subject filter ────────────────────────────────────────────────────────

    @Test
    fun `when only MATH is enabled engine never returns WRITING task`() = runTest {
        activeProfile = profile(subjects = setOf(TaskSubject.MATH))

        repeat(30) {
            val task = engine.getNextTask(CHILD)
            assertNotEquals(
                "Engine must not return WRITING task when only MATH is enabled",
                TaskSubject.WRITING, task.subject,
            )
        }
    }

    @Test
    fun `when only WRITING is enabled engine never returns MATH task`() = runTest {
        activeProfile = profile(subjects = setOf(TaskSubject.WRITING))

        repeat(30) {
            val task = engine.getNextTask(CHILD)
            assertNotEquals(
                "Engine must not return MATH task when only WRITING is enabled",
                TaskSubject.MATH, task.subject,
            )
        }
    }

    @Test
    fun `when subject set is empty all subjects are allowed`() = runTest {
        activeProfile = profile(subjects = emptySet()) // empty = all

        val subjects = mutableSetOf<TaskSubject>()
        repeat(60) { subjects += engine.getNextTask(CHILD).subject }

        // With enough iterations both MATH and WRITING should appear.
        assertTrue("Expected MATH tasks", TaskSubject.MATH in subjects)
        assertTrue("Expected WRITING tasks", TaskSubject.WRITING in subjects)
    }

    // ── TaskType filter ───────────────────────────────────────────────────────

    @Test
    fun `when LETTER_TRACING is disabled engine never returns it`() = runTest {
        val allTypes = TaskType.entries.toSet() - TaskType.LETTER_TRACING
        activeProfile = profile(types = allTypes)

        repeat(40) {
            val task = engine.getNextTask(CHILD)
            assertNotEquals(
                "Engine must not return LETTER_TRACING when it is disabled",
                TaskType.LETTER_TRACING, task.taskType,
            )
        }
    }

    @Test
    fun `when only SIMPLE_ADDITION is enabled engine only returns that type`() = runTest {
        activeProfile = profile(
            subjects = setOf(TaskSubject.MATH),
            types = setOf(TaskType.SIMPLE_ADDITION),
        )

        repeat(30) {
            val task = engine.getNextTask(CHILD)
            assertTrue(
                "Expected SIMPLE_ADDITION but got ${task.taskType}",
                task.taskType == TaskType.SIMPLE_ADDITION,
            )
        }
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    @Test
    fun `when all task types are disabled engine returns fallback MATH task`() = runTest {
        // An empty enabledTaskTypes means "all allowed" by convention, so to test
        // the fallback we provide a profile with every type explicitly disabled via
        // a set that contains no valid types. We simulate this by mocking the profile
        // with a non-empty set that contains no real TaskType values at the engine level.
        // The cleanest way: restrict to WRITING subject + only SIMPLE_ADDITION type
        // – no WRITING+SIMPLE_ADDITION tasks exist → engine hits fallback.
        activeProfile = profile(
            subjects = setOf(TaskSubject.WRITING),
            types = setOf(TaskType.SIMPLE_ADDITION), // no overlap with WRITING catalog
        )

        // Engine must not crash – it should return a fallback task
        val task = engine.getNextTask(CHILD)
        assertTrue(
            "Fallback must be a MATH task",
            task.subject == TaskSubject.MATH,
        )
    }

    // ── No profile → all tasks allowed ───────────────────────────────────────

    @Test
    fun `when no profile exists all subjects and types are allowed`() = runTest {
        activeProfile = null // no child profile

        val taskTypes = mutableSetOf<TaskType>()
        repeat(60) { taskTypes += engine.getNextTask(CHILD).taskType }

        // Both arithmetic and tracing tasks should appear eventually
        assertTrue("Expected arithmetic tasks", taskTypes.any { it != TaskType.LETTER_TRACING })
        assertTrue("Expected letter tracing tasks", TaskType.LETTER_TRACING in taskTypes)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun profile(
        subjects: Set<TaskSubject> = emptySet(),
        types: Set<TaskType> = emptySet(),
    ) = ChildProfile(
        id = CHILD,
        name = "TestChild",
        age = 7,
        enabledSubjects = subjects,
        enabledTaskTypes = types,
    )

    companion object {
        private const val CHILD = "filter_test_child"
    }
}
