package pl.kindergate.data.engine

import pl.kindergate.domain.engine.TaskEngine
import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.ChildProgress
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.LevelProgress
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContext
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.repository.TaskRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MVP implementation of [TaskEngine] with simple adaptive difficulty.
 *
 * ## Difficulty adaptation (per child)
 * The engine tracks the current difficulty level per [childId] in memory.
 * After every [WINDOW_SIZE] submissions the window is evaluated:
 *   - All [WINDOW_SIZE] correct  → level up   (capped at [MAX_LEVEL])
 *   - More than [FAILURE_THRESHOLD] wrong → level down (floored at [MIN_LEVEL])
 *   - Otherwise                  → stay
 *
 * ## History storage
 * MVP: [ConcurrentHashMap] – survives the Activity lifecycle but resets on process death.
 * Migration path: inject a [SessionRepository]-like TaskHistoryRepository backed by Room,
 * load history on first [getNextTask] call, flush on each [submitAnswer].
 *
 * ## Thread safety
 * [ConcurrentHashMap] provides safe concurrent reads/writes.
 * The adaptive-level computation is not atomic across concurrent submits, but this is
 * acceptable for a single-child MVP where only one coroutine submits at a time.
 */
@Singleton
class SimpleTaskEngine @Inject constructor(
    private val taskRepository: TaskRepository,
    private val evaluators: Set<@JvmSuppressWildcards TaskEvaluator>,
) : TaskEngine {

    /** Answer history per childId – used for progress reporting and difficulty adaptation. */
    private val history = ConcurrentHashMap<String, MutableList<EvaluationResult>>()

    /** Current difficulty level per childId – updated after each adaptive decision. */
    private val activeLevels = ConcurrentHashMap<String, Int>()

    // ── TaskEngine ────────────────────────────────────────────────────────────

    override suspend fun getNextTask(childId: String, context: TaskContext?): Task {
        val level = resolveLevel(childId, context)
        activeLevels[childId] = level
        return taskRepository.getRandomTask(
            subject = TaskSubject.MATH,
            difficultyLevel = level,
        ) ?: error("No tasks available for subject=MATH, level=$level")
    }

    override suspend fun submitAnswer(
        childId: String,
        taskId: String,
        answer: String,
    ): EvaluationResult {
        val task = taskRepository.getTaskById(taskId)
            ?: error("Task not found: $taskId")
        val evaluator = evaluators.find { it.canEvaluate(task) }
            ?: error("No evaluator registered for taskType=${task.taskType}")

        val result = evaluator.evaluate(task, answer)
        history.getOrPut(childId) { mutableListOf() }.add(result)
        // Recalculate level immediately so the NEXT getNextTask call is adaptive
        activeLevels[childId] = resolveLevel(childId, context = null)
        return result
    }

    override suspend fun getChildProgress(childId: String): ChildProgress {
        val results = history[childId].orEmpty()
        val correct = results.count { it.isCorrect }
        val accuracy = if (results.isEmpty()) 0.0 else correct.toDouble() / results.size
        return ChildProgress(
            childId = childId,
            totalAnswered = results.size,
            correctCount = correct,
            accuracy = accuracy,
            progressByLevel = buildLevelProgress(results),
        )
    }

    // ── Adaptive difficulty ───────────────────────────────────────────────────

    /**
     * Determines the difficulty level for the next task.
     *
     * Priority order:
     *   1. Existing active level (from previous adaptive decision).
     *   2. [context.preferredDifficultyLevel] – used only on first call.
     *   3. [MIN_LEVEL] fallback.
     *
     * Adaptation only fires once [WINDOW_SIZE] answers are available.
     */
    private fun resolveLevel(childId: String, context: TaskContext?): Int {
        val baseLevel = activeLevels[childId]
            ?: context?.preferredDifficultyLevel?.coerceIn(MIN_LEVEL, MAX_LEVEL)
            ?: MIN_LEVEL

        val recent = history[childId]?.takeLast(WINDOW_SIZE) ?: return baseLevel
        if (recent.size < WINDOW_SIZE) return baseLevel

        val correctInWindow = recent.count { it.isCorrect }
        val wrongInWindow = WINDOW_SIZE - correctInWindow

        return when {
            correctInWindow == WINDOW_SIZE -> (baseLevel + 1).coerceAtMost(MAX_LEVEL)
            wrongInWindow > FAILURE_THRESHOLD -> (baseLevel - 1).coerceAtLeast(MIN_LEVEL)
            else -> baseLevel
        }
    }

    private fun buildLevelProgress(results: List<EvaluationResult>): Map<Int, LevelProgress> {
        // MVP: we don't embed difficulty level in EvaluationResult yet, so we return empty.
        // Future: add difficultyLevel field to EvaluationResult and group here.
        return emptyMap()
    }

    companion object {
        internal const val MIN_LEVEL = 1
        internal const val MAX_LEVEL = 3
        /** Number of recent answers to inspect for adaptation. */
        internal const val WINDOW_SIZE = 5
        /** How many wrong answers in the window triggers a level-down. */
        internal const val FAILURE_THRESHOLD = 2
    }
}
