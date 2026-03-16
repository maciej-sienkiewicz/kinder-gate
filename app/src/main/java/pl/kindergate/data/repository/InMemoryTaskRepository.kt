package pl.kindergate.data.repository

import pl.kindergate.domain.model.task.CognitiveSkill
import pl.kindergate.domain.model.task.DifficultyMode
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskMetadata
import pl.kindergate.domain.model.task.TaskSource
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType
import pl.kindergate.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Static, in-memory task catalog for MVP.
 *
 * ## Math tasks (subject = MATH) — 3 difficulty levels each:
 *
 * ### Addition
 *   L1 – 1-digit + 1-digit, sum ≤ 10
 *   L2 – 1-digit + 2-digit, sum ≤ 20
 *   L3 – 2-digit + 2-digit, varied sums ≤ 100
 *
 * ### Subtraction
 *   L1 – 1-digit − 1-digit, result ≥ 0
 *   L2 – 2-digit − 1-digit
 *   L3 – 2-digit − 2-digit
 *
 * ### Multiplication
 *   L1 – ×2, ×3  (tables of 2 and 3)
 *   L2 – ×4, ×5, ×6
 *   L3 – ×7, ×8, ×9
 *
 * ### Division  (always exact integer result)
 *   L1 – ÷2, ÷3, ÷4, ÷5  (result 1–5)
 *   L2 – ÷2–÷5  (result 6–10)
 *   L3 – ÷6–÷9
 *
 * ### Mixed / Parentheses
 *   L1 – (a+b)×c  with small factors
 *   L2 – (a−b)×c  and  a×b±c
 *   L3 – nested parentheses, two-pair products, PEMDAS chains
 *
 * ## Letter-tracing tasks (subject = WRITING, level = 1): A–Z
 *
 * Migration path to Room:
 *   Replace [InMemoryTaskRepository] binding in TaskModule – no domain changes needed.
 */
@Singleton
class InMemoryTaskRepository @Inject constructor() : TaskRepository {

    private val catalog: List<Task> = buildCatalog()

    override suspend fun getTaskById(id: String): Task? =
        catalog.find { it.id == id }

    override suspend fun getTasksForSet(taskSetId: String): List<Task> =
        catalog.filter { task ->
            when (taskSetId) {
                TASK_SET_LEVEL_1 -> task.difficultyLevel == 1
                TASK_SET_LEVEL_2 -> task.difficultyLevel == 2
                TASK_SET_LEVEL_3 -> task.difficultyLevel == 3
                else -> false
            }
        }

    override suspend fun getRandomTask(subject: TaskSubject, difficultyLevel: Int): Task? =
        catalog
            .filter { it.subject == subject && it.difficultyLevel == difficultyLevel }
            .randomOrNull()

    override suspend fun getRandomTaskFiltered(
        subjects: Set<TaskSubject>,
        taskTypes: Set<TaskType>,
        difficultyLevel: Int,
    ): Task? = catalog
        .filter { task ->
            task.difficultyLevel == difficultyLevel &&
                task.subject in subjects &&
                task.taskType in taskTypes
        }
        .randomOrNull()

    companion object {
        const val TASK_SET_LEVEL_1 = "addition_level_1"
        const val TASK_SET_LEVEL_2 = "addition_level_2"
        const val TASK_SET_LEVEL_3 = "addition_level_3"

        @Suppress("LongMethod")
        private fun buildCatalog(): List<Task> = buildList {

            // ═══════════════════════════════════════════════════════════════════
            // ADDITION
            // ═══════════════════════════════════════════════════════════════════

            // L1 – 1-digit + 1-digit, sum ≤ 10
            listOf(
                1 to 1, 1 to 2, 2 to 2, 1 to 3, 2 to 3,
                3 to 3, 1 to 4, 2 to 4, 3 to 4, 4 to 4,
                1 to 5, 2 to 5, 3 to 5, 1 to 6, 4 to 6,
            ).forEachIndexed { i, (a, b) ->
                add(addition("add_l1_$i", a, b, 1))
            }

            // L2 – 1-digit + 2-digit, sum ≤ 20
            listOf(
                5 to 6, 7 to 8, 6 to 9, 8 to 9, 7 to 7,
                9 to 9, 3 to 11, 4 to 12, 5 to 13, 6 to 14,
                8 to 8, 9 to 7, 2 to 15, 3 to 14, 4 to 13,
            ).forEachIndexed { i, (a, b) ->
                add(addition("add_l2_$i", a, b, 2))
            }

            // L3 – 2-digit + 2-digit, sums ≤ 100
            listOf(
                21 to 19, 34 to 26, 45 to 35, 12 to 48, 27 to 33,
                16 to 54, 43 to 27, 38 to 42, 25 to 65, 11 to 79,
                52 to 38, 47 to 43, 33 to 57, 64 to 24, 19 to 71,
            ).forEachIndexed { i, (a, b) ->
                add(addition("add_l3_$i", a, b, 3))
            }

            // ═══════════════════════════════════════════════════════════════════
            // SUBTRACTION
            // ═══════════════════════════════════════════════════════════════════

            // L1 – 1-digit − 1-digit, result ≥ 0
            listOf(
                5 to 2, 7 to 3, 9 to 4, 8 to 5, 6 to 1,
                9 to 6, 7 to 4, 8 to 3, 6 to 2, 9 to 5,
                7 to 2, 8 to 4, 5 to 3, 6 to 4, 9 to 7,
            ).forEachIndexed { i, (m, s) ->
                add(subtraction("sub_l1_$i", m, s, 1))
            }

            // L2 – 2-digit − 1-digit
            listOf(
                15 to 7, 18 to 9, 23 to 8, 31 to 6, 44 to 7,
                52 to 9, 67 to 8, 73 to 5, 81 to 4, 90 to 6,
                36 to 7, 42 to 8, 55 to 9, 64 to 6, 78 to 5,
            ).forEachIndexed { i, (m, s) ->
                add(subtraction("sub_l2_$i", m, s, 2))
            }

            // L3 – 2-digit − 2-digit (mix of with/without borrowing)
            listOf(
                35 to 12, 47 to 23, 56 to 34, 72 to 41, 85 to 53,
                94 to 62, 38 to 15, 63 to 27, 91 to 48, 75 to 39,
                82 to 46, 67 to 38, 54 to 29, 43 to 17, 86 to 57,
            ).forEachIndexed { i, (m, s) ->
                add(subtraction("sub_l3_$i", m, s, 3))
            }

            // ═══════════════════════════════════════════════════════════════════
            // MULTIPLICATION
            // ═══════════════════════════════════════════════════════════════════

            // L1 – tables of 2 and 3
            listOf(
                2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2,
                7 to 2, 8 to 2, 9 to 2, 2 to 3, 3 to 3,
                4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3,
            ).forEachIndexed { i, (a, b) ->
                add(multiplication("mul_l1_$i", a, b, 1))
            }

            // L2 – tables of 4, 5, 6
            listOf(
                2 to 4, 3 to 4, 4 to 4, 5 to 4, 6 to 4,
                7 to 4, 2 to 5, 3 to 5, 4 to 5, 5 to 5,
                6 to 5, 2 to 6, 3 to 6, 4 to 6, 5 to 6,
            ).forEachIndexed { i, (a, b) ->
                add(multiplication("mul_l2_$i", a, b, 2))
            }

            // L3 – tables of 7, 8, 9
            listOf(
                2 to 7, 3 to 7, 4 to 7, 5 to 7, 6 to 7,
                2 to 8, 3 to 8, 4 to 8, 5 to 8, 6 to 8,
                2 to 9, 3 to 9, 4 to 9, 5 to 9, 6 to 9,
            ).forEachIndexed { i, (a, b) ->
                add(multiplication("mul_l3_$i", a, b, 3))
            }

            // ═══════════════════════════════════════════════════════════════════
            // DIVISION  (exact integer results)
            // ═══════════════════════════════════════════════════════════════════

            // L1 – ÷2–÷5, result 1–5
            listOf(
                4 to 2, 6 to 2, 8 to 2, 10 to 2, 6 to 3,
                9 to 3, 12 to 3, 15 to 3, 8 to 4, 12 to 4,
                16 to 4, 10 to 5, 15 to 5, 20 to 5, 25 to 5,
            ).forEachIndexed { i, (d, v) ->
                add(division("div_l1_$i", d, v, 1))
            }

            // L2 – ÷2–÷5, result 6–10
            listOf(
                16 to 2, 18 to 2, 20 to 2, 18 to 3, 21 to 3,
                24 to 3, 27 to 3, 20 to 4, 24 to 4, 28 to 4,
                30 to 5, 35 to 5, 40 to 5, 45 to 5, 32 to 4,
            ).forEachIndexed { i, (d, v) ->
                add(division("div_l2_$i", d, v, 2))
            }

            // L3 – ÷6–÷9
            listOf(
                24 to 6, 30 to 6, 36 to 6, 42 to 6, 48 to 6,
                42 to 7, 49 to 7, 56 to 7, 63 to 7, 32 to 8,
                40 to 8, 48 to 8, 27 to 9, 36 to 9, 45 to 9,
            ).forEachIndexed { i, (d, v) ->
                add(division("div_l3_$i", d, v, 3))
            }

            // ═══════════════════════════════════════════════════════════════════
            // MIXED OPERATIONS WITH PARENTHESES
            // ═══════════════════════════════════════════════════════════════════

            // L1 – (a+b)×c  with small single-digit factors (introduces parentheses)
            listOf(
                "(2+3)×2" to 10,
                "(1+4)×3" to 15,
                "(3+2)×4" to 20,
                "(2+2)×4" to 16,
                "(4+1)×5" to 25,
                "(1+2)×6" to 18,
                "(3+1)×6" to 24,
                "(2+4)×3" to 18,
                "(3+3)×3" to 18,
                "(5+1)×4" to 24,
                "(2+3)×5" to 25,
                "(1+3)×5" to 20,
                "(4+2)×2" to 12,
                "(3+4)×2" to 14,
                "(5+2)×3" to 21,
            ).forEachIndexed { i, (expr, answer) ->
                add(expression("mix_l1_$i", expr, answer, 1))
            }

            // L2 – (a−b)×c  and  a×b±c  (parentheses with subtraction, or PEMDAS)
            listOf(
                "(5-2)×4"  to 12,
                "(8-3)×3"  to 15,
                "(6-2)×5"  to 20,
                "(10-4)×3" to 18,
                "(7-3)×4"  to 16,
                "2×3+5"    to 11,
                "3×4+6"    to 18,
                "4×2+7"    to 15,
                "5×3+4"    to 19,
                "2×6+4"    to 16,
                "3×5-2"    to 13,
                "4×4-3"    to 13,
                "6×3-4"    to 14,
                "5×4-7"    to 13,
                "3×6-5"    to 13,
            ).forEachIndexed { i, (expr, answer) ->
                add(expression("mix_l2_$i", expr, answer, 2))
            }

            // L3 – nested parens, two-product expressions, full PEMDAS chains
            listOf(
                "(2+3)×(4-1)"  to 15,
                "(4+1)×(2+3)"  to 25,
                "(5-2)×(3+2)"  to 15,
                "(6-3)×(4+1)"  to 15,
                "(3+4)×(5-2)"  to 21,
                "2×(3+5)"      to 16,
                "3×(7-3)"      to 12,
                "4×(2+3)"      to 20,
                "5×(6-2)"      to 20,
                "3×(4+2)"      to 18,
                "2×3+4×2"      to 14,
                "3×4-2×3"      to 6,
                "2×(4+3)-1"    to 13,
                "3×(5-2)+4"    to 13,
                "(4+3)×(3-1)"  to 14,
            ).forEachIndexed { i, (expr, answer) ->
                add(expression("mix_l3_$i", expr, answer, 3))
            }

            // ═══════════════════════════════════════════════════════════════════
            // LETTER TRACING  (A–Z, level 1)
            // ═══════════════════════════════════════════════════════════════════
            ('A'..'Z').forEach { letter ->
                add(letterTracing(letter))
            }
        }

        // ── Task-builder helpers ───────────────────────────────────────────────

        private fun addition(id: String, a: Int, b: Int, level: Int): Task = Task(
            id = id,
            source = TaskSource.APP_LIBRARY,
            subject = TaskSubject.MATH,
            cognitiveSkill = CognitiveSkill.SIMPLE_ARITHMETIC,
            difficultyMode = DifficultyMode.MANUAL,
            difficultyLevel = level,
            taskType = TaskType.SIMPLE_ADDITION,
            timeLimitSeconds = null,
            content = TaskContent.SimpleAdditionContent(a, b, a + b),
            metadata = TaskMetadata(gradeLevel = level, tags = listOf("dodawanie", "arytmetyka")),
        )

        private fun subtraction(id: String, minuend: Int, subtrahend: Int, level: Int): Task = Task(
            id = id,
            source = TaskSource.APP_LIBRARY,
            subject = TaskSubject.MATH,
            cognitiveSkill = CognitiveSkill.SIMPLE_ARITHMETIC,
            difficultyMode = DifficultyMode.MANUAL,
            difficultyLevel = level,
            taskType = TaskType.SIMPLE_SUBTRACTION,
            timeLimitSeconds = null,
            content = TaskContent.SubtractionContent(minuend, subtrahend, minuend - subtrahend),
            metadata = TaskMetadata(gradeLevel = level, tags = listOf("odejmowanie", "arytmetyka")),
        )

        private fun multiplication(id: String, a: Int, b: Int, level: Int): Task = Task(
            id = id,
            source = TaskSource.APP_LIBRARY,
            subject = TaskSubject.MATH,
            cognitiveSkill = CognitiveSkill.SIMPLE_ARITHMETIC,
            difficultyMode = DifficultyMode.MANUAL,
            difficultyLevel = level,
            taskType = TaskType.MULTIPLICATION,
            timeLimitSeconds = null,
            content = TaskContent.MultiplicationContent(a, b, a * b),
            metadata = TaskMetadata(gradeLevel = level, tags = listOf("mnożenie", "tabliczka")),
        )

        private fun division(id: String, dividend: Int, divisor: Int, level: Int): Task = Task(
            id = id,
            source = TaskSource.APP_LIBRARY,
            subject = TaskSubject.MATH,
            cognitiveSkill = CognitiveSkill.SIMPLE_ARITHMETIC,
            difficultyMode = DifficultyMode.MANUAL,
            difficultyLevel = level,
            taskType = TaskType.DIVISION,
            timeLimitSeconds = null,
            content = TaskContent.DivisionContent(dividend, divisor, dividend / divisor),
            metadata = TaskMetadata(gradeLevel = level, tags = listOf("dzielenie", "arytmetyka")),
        )

        private fun expression(id: String, expr: String, answer: Int, level: Int): Task = Task(
            id = id,
            source = TaskSource.APP_LIBRARY,
            subject = TaskSubject.MATH,
            cognitiveSkill = CognitiveSkill.SIMPLE_ARITHMETIC,
            difficultyMode = DifficultyMode.MANUAL,
            difficultyLevel = level,
            taskType = TaskType.MIXED_OPERATIONS,
            timeLimitSeconds = null,
            content = TaskContent.ExpressionContent(expr, answer),
            metadata = TaskMetadata(gradeLevel = level, tags = listOf("nawiasy", "działania_mieszane")),
        )

        private fun letterTracing(letter: Char): Task = Task(
            id = "letter_$letter",
            source = TaskSource.APP_LIBRARY,
            subject = TaskSubject.WRITING,
            cognitiveSkill = CognitiveSkill.LETTER_RECOGNITION,
            difficultyMode = DifficultyMode.MANUAL,
            difficultyLevel = 1,
            taskType = TaskType.LETTER_TRACING,
            timeLimitSeconds = null,
            content = TaskContent.LetterTracingContent(letter = letter),
            metadata = TaskMetadata(gradeLevel = 1, tags = listOf("literki", "pisanie", "alfabet")),
        )
    }
}
