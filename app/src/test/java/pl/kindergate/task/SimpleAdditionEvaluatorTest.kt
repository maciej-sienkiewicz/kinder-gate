package pl.kindergate.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.kindergate.data.engine.SimpleAdditionEvaluator
import pl.kindergate.domain.model.task.CognitiveSkill
import pl.kindergate.domain.model.task.DifficultyMode
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskSource
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType

class SimpleAdditionEvaluatorTest {

    private lateinit var evaluator: SimpleAdditionEvaluator

    @Before
    fun setUp() {
        evaluator = SimpleAdditionEvaluator()
    }

    // ── canEvaluate ───────────────────────────────────────────────────────────

    @Test
    fun `canEvaluate returns true for SIMPLE_ADDITION task`() {
        assertTrue(evaluator.canEvaluate(additionTask(3, 4)))
    }

    // ── Correct answers ───────────────────────────────────────────────────────

    @Test
    fun `correct answer returns isCorrect=true`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "5")
        assertTrue(result.isCorrect)
    }

    @Test
    fun `correct answer with leading spaces is accepted`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "  5  ")
        assertTrue(result.isCorrect)
    }

    @Test
    fun `correct answer feedback contains both operands and result`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "5")
        assertTrue("Feedback should mention operandA", "2" in result.feedbackMessage)
        assertTrue("Feedback should mention operandB", "3" in result.feedbackMessage)
        assertTrue("Feedback should mention correct answer", "5" in result.feedbackMessage)
    }

    @Test
    fun `EvaluationResult carries correct taskId`() {
        val task = additionTask(1, 1, id = "test_id_42")
        val result = evaluator.evaluate(task, answer = "2")
        assertEquals("test_id_42", result.taskId)
    }

    @Test
    fun `EvaluationResult correct answer field is string representation`() {
        val result = evaluator.evaluate(additionTask(7, 8), answer = "15")
        assertEquals("15", result.correctAnswer)
    }

    // ── Wrong answers ─────────────────────────────────────────────────────────

    @Test
    fun `wrong numeric answer returns isCorrect=false`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "4")
        assertFalse(result.isCorrect)
    }

    @Test
    fun `non-numeric answer returns isCorrect=false`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "abc")
        assertFalse(result.isCorrect)
    }

    @Test
    fun `empty answer returns isCorrect=false`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "")
        assertFalse(result.isCorrect)
    }

    @Test
    fun `wrong answer feedback contains user answer`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "99")
        assertTrue("Feedback should mention the wrong answer", "99" in result.feedbackMessage)
    }

    @Test
    fun `wrong answer feedback contains correct answer`() {
        val result = evaluator.evaluate(additionTask(2, 3), answer = "99")
        assertTrue("Feedback should reveal correct answer", "5" in result.feedbackMessage)
    }

    @Test
    fun `userAnswer in result is trimmed`() {
        val result = evaluator.evaluate(additionTask(1, 1), answer = "  2  ")
        assertEquals("2", result.userAnswer)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `zero as answer is handled correctly`() {
        // 0 + 0 = 0 (level-1 edge; unlikely in catalog but the evaluator must handle it)
        val result = evaluator.evaluate(additionTask(0, 0), answer = "0")
        assertTrue(result.isCorrect)
    }

    @Test
    fun `negative number answer is incorrect`() {
        val result = evaluator.evaluate(additionTask(3, 4), answer = "-7")
        assertFalse(result.isCorrect)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun additionTask(a: Int, b: Int, id: String = "test_add"): Task = Task(
        id = id,
        source = TaskSource.APP_LIBRARY,
        subject = TaskSubject.MATH,
        cognitiveSkill = CognitiveSkill.SIMPLE_ARITHMETIC,
        difficultyMode = DifficultyMode.MANUAL,
        difficultyLevel = 1,
        taskType = TaskType.SIMPLE_ADDITION,
        timeLimitSeconds = null,
        content = TaskContent.SimpleAdditionContent(
            operandA = a,
            operandB = b,
            correctAnswer = a + b,
        ),
    )
}
