package pl.kindergate.domain.model.task

/**
 * Optional descriptive metadata about a [Task].
 * Not used in evaluation – purely informational / for future UI filtering.
 *
 * @param gradeLevel Approximate school grade (1 = first grade, etc.)
 * @param tags Free-form labels, e.g. ["szybkie", "wizualne"]
 * @param estimatedTimeSeconds Rough expected solve time; null means no estimate
 */
data class TaskMetadata(
    val gradeLevel: Int? = null,
    val tags: List<String> = emptyList(),
    val estimatedTimeSeconds: Int? = null,
)
