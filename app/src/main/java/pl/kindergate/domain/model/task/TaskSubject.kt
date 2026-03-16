package pl.kindergate.domain.model.task

/**
 * Academic subject / knowledge domain.
 *
 * Each value maps to a subset of [TaskType] entries:
 *   - MATH    → SIMPLE_ADDITION, SIMPLE_SUBTRACTION, MULTIPLICATION, DIVISION, MIXED_OPERATIONS
 *   - WRITING → LETTER_TRACING
 *
 * Future: READING, SCIENCE, CODING, GEOGRAPHY, FOREIGN_LANGUAGE, …
 */
enum class TaskSubject {
    MATH,
    WRITING,
    // READING,
    // SCIENCE,
    // CODING,
}
