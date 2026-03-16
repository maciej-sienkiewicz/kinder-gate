package pl.kindergate.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import pl.kindergate.data.engine.DivisionEvaluator
import pl.kindergate.data.engine.ExpressionEvaluator
import pl.kindergate.data.engine.LetterTracingEvaluator
import pl.kindergate.data.engine.MultiplicationEvaluator
import pl.kindergate.data.engine.SimpleAdditionEvaluator
import pl.kindergate.data.engine.SubtractionEvaluator
import pl.kindergate.data.engine.SimpleTaskEngine
import pl.kindergate.data.repository.InMemoryTaskRepository
import pl.kindergate.domain.engine.TaskEngine
import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.repository.TaskRepository
import javax.inject.Singleton

/**
 * Hilt DI bindings for the Task Engine sub-domain.
 *
 * ## Evaluator multibinding
 * Each [TaskEvaluator] is contributed to a `Set<TaskEvaluator>` via [@IntoSet].
 * [SimpleTaskEngine] receives this set and picks the right evaluator at runtime.
 * Adding a new task type only requires:
 *   1. Implementing [TaskEvaluator]
 *   2. Adding one [@Binds @IntoSet] entry here
 *   3. No changes to [SimpleTaskEngine] or any other existing class.
 *
 * ## Repository migration
 * To switch from [InMemoryTaskRepository] to a Room-backed implementation:
 *   replace the [bindTaskRepository] binding – all call sites remain untouched.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TaskModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: InMemoryTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTaskEngine(impl: SimpleTaskEngine): TaskEngine

    // ── Evaluator set ─────────────────────────────────────────────────────────
    // Add one @Binds @IntoSet entry per TaskType.

    @Binds
    @IntoSet
    abstract fun bindSimpleAdditionEvaluator(impl: SimpleAdditionEvaluator): TaskEvaluator

    @Binds
    @IntoSet
    abstract fun bindSubtractionEvaluator(impl: SubtractionEvaluator): TaskEvaluator

    @Binds
    @IntoSet
    abstract fun bindMultiplicationEvaluator(impl: MultiplicationEvaluator): TaskEvaluator

    @Binds
    @IntoSet
    abstract fun bindDivisionEvaluator(impl: DivisionEvaluator): TaskEvaluator

    @Binds
    @IntoSet
    abstract fun bindExpressionEvaluator(impl: ExpressionEvaluator): TaskEvaluator

    @Binds
    @IntoSet
    abstract fun bindLetterTracingEvaluator(impl: LetterTracingEvaluator): TaskEvaluator
}
