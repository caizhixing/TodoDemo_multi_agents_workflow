package com.example.tododemo.domain

import com.example.tododemo.domain.model.PlayerProgress
import com.example.tododemo.domain.model.TodoItem

object ProgressCalculator {

    fun calculate(todos: List<TodoItem>): PlayerProgress {
        val completedCount = todos.count(TodoItem::isCompleted)
        val totalPoints = completedCount * PointsPolicy.POINTS_PER_TODO
        return PlayerProgress(
            totalPoints = totalPoints,
            completedCount = completedCount,
            rank = RankResolver.resolveForPoints(totalPoints),
        )
    }
}
