package pl.kindergate.task

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskContext

/**
 * Unit tests for [SimpleTaskEngine].
 *
 * Uses real [InMemoryTaskRepository] and all evaluators – both are pure/fast
 * so we don't need fakes here. If the catalog or evaluators grow expensive, swap with fakes.
 */
class SimpleTaskEngineTest {

    private lateinit var engine: SimpleTaskEngine

    @Before
    fun setUp() {
        val repo = InMemoryTaskRepository()
        engine = SimpleTaskEngine(
            taskRepository = repo,
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

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `getNextTask with no history uses level 1`() = runTest {
        val task = engine.getNextTask(CHILD)
        assertEquals(1, task.difficultyLevel)
    }

    @Test
    fun `getNextTask respects preferredDifficultyLevel from context when no history`() = runTest {
        val task = engine.getNextTask(CHILD, TaskContext(preferredDifficultyLevel = 2))
        assertEquals(2, task.difficultyLevel)
    }

    @Test
    fun `preferredDifficultyLevel is clamped to valid range`() = runTest {
        val taskAbove = engine.getNextTask(CHILD, TaskContext(preferredDifficultyLevel = 99))
        assertEquals(SimpleTaskEngine.MAX_LEVEL, taskAbove.difficultyLevel)

        setUp() // fresh engine for next sub-test
        val taskBelow = engine.getNextTask(CHILD, TaskContext(preferredDifficultyLevel = -5))
        assertEquals(SimpleTaskEngine.MIN_LEVEL, taskBelow.difficultyLevel)
    }

    // ── submitAnswer stores history ───────────────────────────────────────────

    @Test
    fun `submitAnswer returns correct result for right answer`() = runTest {
        val task = engine.getNextTask(CHILD)
        val answer = correctAnswerFor(task)
        val result = engine.submitAnswer(CHILD, task.id, answer)
        assertTrue(result.isCorrect)
    }

    @Test
    fun `submitAnswer returns incorrect result for wrong answer`() = runTest {
        val task = engine.getNextTask(CHILD)
        val result = engine.submitAnswer(CHILD, task.id, "9999")
        assertFalse(result.isCorrect)
    }

    @Test
    fun `getChildProgress totals reflect submitted answers`() = runTest {
        val task1 = engine.getNextTask(CHILD)
        engine.submitAnswer(CHILD, task1.id, correctAnswerFor(task1))   // correct

        val task2 = engine.getNextTask(CHILD)
        engine.submitAnswer(CHILD, task2.id, "9999")                     // wrong

        val progress = engine.getChildProgress(CHILD)
        assertEquals(2, progress.totalAnswered)
        assertEquals(1, progress.correctCount)
        assertEquals(0.5, progress.accuracy, 0.001)
    }

    @Test
    fun `fresh child has zero progress`() = runTest {
        val progress = engine.getChildProgress("no_such_child")
        assertEquals(0, progress.totalAnswered)
        assertEquals(0, progress.correctCount)
        assertEquals(0.0, progress.accuracy, 0.001)
    }

    // ── Adaptive difficulty ───────────────────────────────────────────────────

    @Test
    fun `level increases after WINDOW_SIZE consecutive correct answers`() = runTest {
        // Answer WINDOW_SIZE tasks correctly at level 1
        repeat(SimpleTaskEngine.WINDOW_SIZE) {
            val task = engine.getNextTask(CHILD)
            engine.submitAnswer(CHILD, task.id, correctAnswerFor(task))
        }
        val next = engine.getNextTask(CHILD)
        assertEquals("Level should increase to 2 after full correct window", 2, next.difficultyLevel)
    }

    @Test
    fun `level does not increase before WINDOW_SIZE correct answers`() = runTest {
        repeat(SimpleTaskEngine.WINDOW_SIZE - 1) {
            val task = engine.getNextTask(CHILD)
            engine.submitAnswer(CHILD, task.id, correctAnswerFor(task))
        }
        val next = engine.getNextTask(CHILD)
        assertEquals("Level should still be 1 with only WINDOW_SIZE-1 answers", 1, next.difficultyLevel)
    }

    @Test
    fun `level decreases after too many wrong answers`() = runTest {
        // First push to level 2
        repeat(SimpleTaskEngine.WINDOW_SIZE) {
            val task = engine.getNextTask(CHILD)
            engine.submitAnswer(CHILD, task.id, correctAnswerFor(task))
        }
        // Now fail FAILURE_THRESHOLD+1 times in a full window
        repeat(SimpleTaskEngine.WINDOW_SIZE) {
            val task = engine.getNextTask(CHILD)
            engine.submitAnswer(CHILD, task.id, "9999") // wrong
        }
        val next = engine.getNextTask(CHILD)
        assertEquals("Level should drop back to 1 after many wrong answers", 1, next.difficultyLevel)
    }

    @Test
    fun `level never drops below MIN_LEVEL`() = runTest {
        // Fail a lot at level 1
        repeat(SimpleTaskEngine.WINDOW_SIZE * 3) {
            val task = engine.getNextTask(CHILD)
            engine.submitAnswer(CHILD, task.id, "9999")
        }
        val next = engine.getNextTask(CHILD)
        assertTrue("Level must be >= MIN_LEVEL", next.difficultyLevel >= SimpleTaskEngine.MIN_LEVEL)
    }

    @Test
    fun `level never exceeds MAX_LEVEL`() = runTest {
        // Keep answering correctly to try to push above MAX_LEVEL
        repeat(SimpleTaskEngine.WINDOW_SIZE * (SimpleTaskEngine.MAX_LEVEL + 2)) {
            val task = engine.getNextTask(CHILD)
            engine.submitAnswer(CHILD, task.id, correctAnswerFor(task))
        }
        val next = engine.getNextTask(CHILD)
        assertTrue("Level must be <= MAX_LEVEL", next.difficultyLevel <= SimpleTaskEngine.MAX_LEVEL)
    }

    // ── Multi-child isolation ─────────────────────────────────────────────────

    @Test
    fun `progress is isolated per childId`() = runTest {
        val task = engine.getNextTask("alice")
        engine.submitAnswer("alice", task.id, correctAnswerFor(task))

        val bobProgress = engine.getChildProgress("bob")
        assertEquals("Bob should have no answers", 0, bobProgress.totalAnswered)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the correct answer string for any task content type. */
    private fun correctAnswerFor(task: Task): String = when (val c = task.content) {
        is TaskContent.SimpleAdditionContent  -> c.correctAnswer.toString()
        is TaskContent.SubtractionContent     -> c.correctAnswer.toString()
        is TaskContent.MultiplicationContent  -> c.correctAnswer.toString()
        is TaskContent.DivisionContent        -> c.correctAnswer.toString()
        is TaskContent.ExpressionContent      -> c.correctAnswer.toString()
        is TaskContent.LetterTracingContent   -> "100.0" // coverage % – letter tasks need tracing evaluator
    }

    companion object {
        private const val CHILD = "test_child"
    }
}
