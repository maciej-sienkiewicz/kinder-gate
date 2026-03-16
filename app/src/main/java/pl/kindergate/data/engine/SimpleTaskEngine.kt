package pl.kindergate.data.engine

import android.util.Log
import pl.kindergate.domain.engine.TaskEngine
import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.ChildProgress
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.LevelProgress
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContext
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType
import pl.kindergate.domain.repository.ChildRepository
import pl.kindergate.domain.repository.TaskRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MVP implementation of [TaskEngine] with simple adaptive difficulty.
 *
 * ## Task filtering (per child)
 * Before selecting a task the engine reads [ChildRepository] to get the active
 * [pl.kindergate.domain.model.ChildProfile] for [childId]:
 *   - [ChildProfile.enabledSubjects] empty → all subjects allowed
 *   - [ChildProfile.enabledTaskTypes] empty → all task types allowed
 *
 * If the combined filter yields no candidates at the requested difficulty level the engine
 * retries with lower levels before applying a safe fallback (simplest MATH task).
 *
 * ## Difficulty adaptation (per child)
 * After every [WINDOW_SIZE] submissions the window is evaluated:
 *   - All [WINDOW_SIZE] correct          → level up   (capped at [MAX_LEVEL])
 *   - More than [FAILURE_THRESHOLD] wrong → level down (floored at [MIN_LEVEL])
 *   - Otherwise                          → stay
 *
 * ## History storage
 * MVP: [ConcurrentHashMap] – survives the Activity lifecycle but resets on process death.
 * Migration path: inject a TaskHistoryRepository backed by Room.
 *
 * ## Thread safety
 * [ConcurrentHashMap] provides safe concurrent reads/writes for the MVP single-child case.
 */
@Singleton
class SimpleTaskEngine @Inject constructor(
    private val taskRepository: TaskRepository,
    private val childRepository: ChildRepository,
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

        // Resolve allow-lists from the child's profile.
        // Empty set in the profile ⇒ all entries of that dimension are allowed.
        val profile = childRepository.getChildById(childId)
        val allowedSubjects = profile?.enabledSubjects?.takeIf { it.isNotEmpty() }
            ?: TaskSubject.entries.toSet()
        val allowedTypes = profile?.enabledTaskTypes?.takeIf { it.isNotEmpty() }
            ?: TaskType.entries.toSet()

        // Try the desired level first, then fall back to lower levels to avoid getting stuck
        // when the filter is narrow at a high difficulty.
        for (tryLevel in level downTo MIN_LEVEL) {
            val task = taskRepository.getRandomTaskFiltered(allowedSubjects, allowedTypes, tryLevel)
            if (task != null) return task
        }

        // Safe fallback: the parent's configuration is overly restrictive – serve the
        // simplest available math task so the child is never stuck on the blocking screen.
        Log.w(
            TAG,
            "No tasks found for childId=$childId subjects=$allowedSubjects " +
                "types=$allowedTypes level=$level – using MATH/L1 fallback",
        )
        return taskRepository.getRandomTask(TaskSubject.MATH, MIN_LEVEL)
            ?: error("Task catalog is empty – at least one MATH/L1 task must exist")
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
        // Recalculate level immediately so the NEXT getNextTask call is already adaptive
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
     *   2. [context.preferredDifficultyLevel] – honoured only on the very first call.
     *   3. [MIN_LEVEL] fallback.
     *
     * Adaptation fires only once [WINDOW_SIZE] answers are available.
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

    private fun buildLevelProgress(
        @Suppress("UNUSED_PARAMETER") results: List<EvaluationResult>,
    ): Map<Int, LevelProgress> = emptyMap() // Future: embed difficultyLevel in EvaluationResult

    companion object {
        private const val TAG = "SimpleTaskEngine"
        internal const val MIN_LEVEL = 1
        internal const val MAX_LEVEL = 3
        /** Number of recent answers to inspect for adaptation. */
        internal const val WINDOW_SIZE = 5
        /** How many wrong answers in the window triggers a level-down. */
        internal const val FAILURE_THRESHOLD = 2
    }
}
