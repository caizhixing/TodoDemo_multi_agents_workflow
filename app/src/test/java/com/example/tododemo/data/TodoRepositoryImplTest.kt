package com.example.tododemo.data

import com.example.tododemo.domain.model.TodoItem
import com.example.tododemo.domain.repository.CompleteOutcome
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoRepositoryImplTest {

    private fun entity(
        id: Long,
        title: String,
        isCompleted: Boolean,
        createdAtMillis: Long,
        completedAtMillis: Long? = null,
    ): TodoEntity = TodoEntity(
        id = id,
        title = title,
        isCompleted = isCompleted,
        createdAtMillis = createdAtMillis,
        completedAtMillis = completedAtMillis,
    )

    @Test
    fun observeTodos_sortsPendingByCreatedAtDescThenCompletedByCompletedAtDesc() = runTest {
        val dao = FakeTodoDao(
            listOf(
                entity(1L, "old-pending", false, createdAtMillis = 100L),
                entity(2L, "done-older", true, createdAtMillis = 10L, completedAtMillis = 200L),
                entity(3L, "new-pending", false, createdAtMillis = 300L),
                entity(4L, "done-newer", true, createdAtMillis = 20L, completedAtMillis = 400L),
                entity(5L, "mid-pending", false, createdAtMillis = 200L),
            ),
        )
        val repository = TodoRepositoryImpl(dao)

        val items = repository.observeTodos().first()

        assertThat(items.map { it.id }).containsExactly(3L, 5L, 1L, 4L, 2L).inOrder()
    }

    @Test
    fun observeTodos_keepsStableOrderForEqualTimestamps() = runTest {
        val dao = FakeTodoDao(
            listOf(
                entity(1L, "first-inserted", false, createdAtMillis = 100L),
                entity(2L, "second-inserted", false, createdAtMillis = 100L),
                entity(3L, "done-a", true, createdAtMillis = 10L, completedAtMillis = 500L),
                entity(4L, "done-b", true, createdAtMillis = 20L, completedAtMillis = 500L),
            ),
        )
        val repository = TodoRepositoryImpl(dao)

        val items = repository.observeTodos().first()

        assertThat(items.map { it.id }).containsExactly(1L, 2L, 3L, 4L).inOrder()
    }

    @Test
    fun addTodo_returnsFreshIdAndInsertsIncompleteItemWithoutCompletedAt() = runTest {
        val dao = FakeTodoDao()
        val repository = TodoRepositoryImpl(dao)

        val firstId = repository.addTodo("买牛奶")
        val secondId = repository.addTodo("写周报")

        assertThat(firstId).isEqualTo(1L)
        assertThat(secondId).isEqualTo(2L)
        val stored = checkNotNull(dao.findById(firstId))
        assertThat(stored.title).isEqualTo("买牛奶")
        assertThat(stored.isCompleted).isFalse()
        assertThat(stored.completedAtMillis).isNull()
        assertThat(stored.createdAtMillis).isGreaterThan(0L)
    }

    @Test
    fun addTodo_doesNotValidateTitle() = runTest {
        val dao = FakeTodoDao()
        val repository = TodoRepositoryImpl(dao)

        val id = repository.addTodo("   ")

        val stored = checkNotNull(dao.findById(id))
        assertThat(stored.title).isEqualTo("   ")
        assertThat(stored.isCompleted).isFalse()
    }

    @Test
    fun completeTodo_pendingId_returnsCompletedAndPersistsCompletedState() = runTest {
        val dao = FakeTodoDao(
            listOf(entity(1L, "买牛奶", false, createdAtMillis = 100L)),
        )
        val repository = TodoRepositoryImpl(dao)

        val outcome = repository.completeTodo(1L)

        assertThat(outcome).isEqualTo(CompleteOutcome.Completed)
        val stored = checkNotNull(dao.findById(1L))
        assertThat(stored.isCompleted).isTrue()
        assertThat(checkNotNull(stored.completedAtMillis)).isGreaterThan(0L)
        assertThat(stored.title).isEqualTo("买牛奶")
        assertThat(stored.createdAtMillis).isEqualTo(100L)
    }

    @Test
    fun completeTodo_alreadyCompletedId_returnsAlreadyCompletedAndKeepsData() = runTest {
        val original = entity(1L, "买牛奶", true, createdAtMillis = 100L, completedAtMillis = 123_456L)
        val dao = FakeTodoDao(listOf(original))
        val repository = TodoRepositoryImpl(dao)

        val outcome = repository.completeTodo(1L)

        assertThat(outcome).isEqualTo(CompleteOutcome.AlreadyCompleted)
        assertThat(dao.findById(1L)).isEqualTo(original)
    }

    @Test
    fun completeTodo_twice_isIdempotentAndKeepsFirstCompletedAt() = runTest {
        val dao = FakeTodoDao(
            listOf(entity(1L, "买牛奶", false, createdAtMillis = 100L)),
        )
        val repository = TodoRepositoryImpl(dao)

        assertThat(repository.completeTodo(1L)).isEqualTo(CompleteOutcome.Completed)
        val firstCompletedAt = checkNotNull(dao.findById(1L)).completedAtMillis

        assertThat(repository.completeTodo(1L)).isEqualTo(CompleteOutcome.AlreadyCompleted)

        val stored = checkNotNull(dao.findById(1L))
        assertThat(stored.isCompleted).isTrue()
        assertThat(stored.completedAtMillis).isEqualTo(firstCompletedAt)
    }

    @Test
    fun completeTodo_unknownId_returnsNotFound() = runTest {
        val dao = FakeTodoDao(
            listOf(entity(1L, "买牛奶", false, createdAtMillis = 100L)),
        )
        val repository = TodoRepositoryImpl(dao)

        val outcome = repository.completeTodo(999L)

        assertThat(outcome).isEqualTo(CompleteOutcome.NotFound)
        assertThat(dao.table.value).hasSize(1)
    }

    @Test
    fun removeTodo_pendingId_returnsTrueAndRemovesItem() = runTest {
        val dao = FakeTodoDao(
            listOf(
                entity(1L, "买牛奶", false, createdAtMillis = 100L),
                entity(2L, "写周报", false, createdAtMillis = 200L),
            ),
        )
        val repository = TodoRepositoryImpl(dao)

        val removed = repository.removeTodo(1L)

        assertThat(removed).isTrue()
        assertThat(dao.findById(1L)).isNull()
        assertThat(dao.table.value.map { it.id }).containsExactly(2L)
    }

    @Test
    fun removeTodo_completedId_returnsFalseAndKeepsItem() = runTest {
        val original = entity(1L, "买牛奶", true, createdAtMillis = 100L, completedAtMillis = 123_456L)
        val dao = FakeTodoDao(listOf(original))
        val repository = TodoRepositoryImpl(dao)

        val removed = repository.removeTodo(1L)

        assertThat(removed).isFalse()
        assertThat(dao.findById(1L)).isEqualTo(original)
    }

    @Test
    fun removeTodo_unknownId_returnsFalse() = runTest {
        val dao = FakeTodoDao()
        val repository = TodoRepositoryImpl(dao)

        assertThat(repository.removeTodo(999L)).isFalse()
    }

    @Test
    fun observeTodos_emitsNewListAfterAddCompleteAndRemove() = runTest {
        val dao = FakeTodoDao()
        val repository = TodoRepositoryImpl(dao)
        val emissions = mutableListOf<List<TodoItem>>()
        val collector = launch { repository.observeTodos().toList(emissions) }
        advanceUntilIdle()

        repository.addTodo("买牛奶")
        advanceUntilIdle()
        repository.addTodo("写周报")
        advanceUntilIdle()
        repository.completeTodo(1L)
        advanceUntilIdle()
        repository.removeTodo(2L)
        advanceUntilIdle()
        collector.cancel()
        advanceUntilIdle()

        assertThat(emissions).hasSize(5)
        assertThat(emissions[0]).isEmpty()
        assertThat(emissions[1].map { it.id }).containsExactly(1L)
        assertThat(emissions[1].single().isCompleted).isFalse()
        assertThat(emissions[2].map { it.id }).containsExactly(1L, 2L)
        assertThat(emissions[2].all { !it.isCompleted }).isTrue()
        assertThat(emissions[3].map { it.id }).containsExactly(2L, 1L).inOrder()
        assertThat(emissions[3].first().isCompleted).isFalse()
        assertThat(emissions[3].last().isCompleted).isTrue()
        assertThat(emissions[4].map { it.id }).containsExactly(1L)
        assertThat(emissions[4].single().isCompleted).isTrue()
    }
}
