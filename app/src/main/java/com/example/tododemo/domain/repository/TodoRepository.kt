package com.example.tododemo.domain.repository

import com.example.tododemo.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun observeTodos(): Flow<List<TodoItem>>

    suspend fun addTodo(title: String): Long

    suspend fun completeTodo(id: Long): CompleteOutcome

    suspend fun removeTodo(id: Long): Boolean
}

sealed interface CompleteOutcome {
    data object Completed : CompleteOutcome
    data object AlreadyCompleted : CompleteOutcome
    data object NotFound : CompleteOutcome
}
