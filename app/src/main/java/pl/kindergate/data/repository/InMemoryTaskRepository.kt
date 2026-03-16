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
 * Contains 45 pre-seeded simple-addition tasks across three difficulty levels:
 *   Level 1 – operands 1–9,  sums ≤ 10  (grade 1)
 *   Level 2 – operands 1–19, sums ≤ 20  (grade 2)
 *   Level 3 – operands 10–71, sums ≤ 100 (grade 3)
 *
 * Stable task IDs (e.g. "add_l1_0") allow [getTaskById] to work reliably.
 *
 * Migration path to Room:
 *   1. Create a TaskEntity and TaskDao in data/local/db/
 *   2. Implement RoomTaskRepository : TaskRepository
 *   3. Re-bind in TaskModule – no domain or feature code changes needed.
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

    companion object {
        const val TASK_SET_LEVEL_1 = "addition_level_1"
        const val TASK_SET_LEVEL_2 = "addition_level_2"
        const val TASK_SET_LEVEL_3 = "addition_level_3"

        private fun buildCatalog(): List<Task> = buildList {
            // ── Level 1: sums ≤ 10 ────────────────────────────────────────────────
            val l1 = listOf(
                1 to 1, 1 to 2, 2 to 2, 1 to 3, 2 to 3,
                3 to 3, 1 to 4, 2 to 4, 3 to 4, 4 to 4,
                1 to 5, 2 to 5, 3 to 5, 1 to 6, 4 to 6,
            )
            l1.forEachIndexed { i, (a, b) ->
                add(addition(id = "add_l1_$i", a = a, b = b, level = 1))
            }

            // ── Level 2: sums ≤ 20 ────────────────────────────────────────────────
            val l2 = listOf(
                5 to 6, 7 to 8, 6 to 9, 8 to 9, 7 to 7,
                9 to 9, 11 to 5, 12 to 6, 13 to 4, 14 to 5,
                8 to 8, 9 to 7, 11 to 9, 15 to 4, 16 to 3,
            )
            l2.forEachIndexed { i, (a, b) ->
                add(addition(id = "add_l2_$i", a = a, b = b, level = 2))
            }

            // ── Level 3: sums ≤ 100 ───────────────────────────────────────────────
            val l3 = listOf(
                21 to 19, 34 to 26, 45 to 35, 12 to 48, 27 to 33,
                16 to 54, 43 to 27, 38 to 42, 25 to 65, 11 to 79,
                52 to 38, 47 to 43, 33 to 57, 64 to 24, 19 to 71,
            )
            l3.forEachIndexed { i, (a, b) ->
                add(addition(id = "add_l3_$i", a = a, b = b, level = 3))
            }
        }

        private fun addition(id: String, a: Int, b: Int, level: Int): Task = Task(
            id = id,
            source = TaskSource.APP_LIBRARY,
            subject = TaskSubject.MATH,
            cognitiveSkill = CognitiveSkill.SIMPLE_ARITHMETIC,
            difficultyMode = DifficultyMode.MANUAL,
            difficultyLevel = level,
            taskType = TaskType.SIMPLE_ADDITION,
            timeLimitSeconds = null,
            content = TaskContent.SimpleAdditionContent(
                operandA = a,
                operandB = b,
                correctAnswer = a + b,
            ),
            metadata = TaskMetadata(
                gradeLevel = level,
                tags = listOf("dodawanie", "arytmetyka"),
            ),
        )
    }
}
