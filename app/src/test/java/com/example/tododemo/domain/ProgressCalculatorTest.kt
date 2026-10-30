package com.example.tododemo.domain

import com.example.tododemo.domain.model.PlayerProgress
import com.example.tododemo.domain.model.RankTier
import com.example.tododemo.domain.model.TodoItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun mixedListCountsOnlyCompletedTodos() {
        val todos = listOf(
            todoItem(id = 1L, completed = true),
            todoItem(id = 2L, completed = false),
            todoItem(id = 3L, completed = true),
            todoItem(id = 4L, completed = false),
        )

        val progress = ProgressCalculator.calculate(todos)

        assertThat(progress.completedCount).isEqualTo(2)
        assertThat(progress.totalPoints).isEqualTo(20)
        assertThat(progress.rank)
            .isEqualTo(RankResolver.resolveForPoints(progress.totalPoints))
    }

    @Test
    fun rankStaysConsistentWithTotalPoints() {
        val todos = (1L..9L).map { id ->
            todoItem(id = id, completed = id <= 8L)
        } + todoItem(id = 10L, completed = false)

        val progress = ProgressCalculator.calculate(todos)

        assertThat(progress.completedCount).isEqualTo(8)
        assertThat(progress.totalPoints).isEqualTo(80)
        assertThat(progress.rank).isEqualTo(RankTier.DILIGENT)
    }

    @Test
    fun emptyListYieldsZeroProgress() {
        val progress = ProgressCalculator.calculate(emptyList())

        assertThat(progress).isEqualTo(PlayerProgress(0, 0, RankTier.NOVICE))
    }

    private fun todoItem(id: Long, completed: Boolean) = TodoItem(
        id = id,
        title = "todo-$id",
        isCompleted = completed,
        completedAtMillis = if (completed) 1_000L else null,
    )
}
