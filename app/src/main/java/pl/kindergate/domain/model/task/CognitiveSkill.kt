package pl.kindergate.domain.model.task

/**
 * Fine-grained cognitive skill exercised by the task, within a [TaskSubject].
 *
 * MVP: only SIMPLE_ARITHMETIC (single-step addition).
 * Future: MULTI_STEP_ARITHMETIC, READING_COMPREHENSION, LOGICAL_REASONING,
 *         PATTERN_RECOGNITION, CODING_BASICS, …
 */
enum class CognitiveSkill {
    SIMPLE_ARITHMETIC,
    // MULTI_STEP_ARITHMETIC,
    // READING_COMPREHENSION,
    // LOGICAL_REASONING,
    // PATTERN_RECOGNITION,
    // CODING_BASICS,
}
