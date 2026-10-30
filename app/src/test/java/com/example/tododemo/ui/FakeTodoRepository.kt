package com.example.tododemo.ui

import com.example.tododemo.domain.model.TodoItem
import com.example.tododemo.domain.repository.CompleteOutcome
import com.example.tododemo.domain.repository.TodoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTodoRepository : TodoRepository {

    val addTodoTitles = mutableListOf<String>()
    val completeTodoIds = mutableListOf<Long>()
    val removeTodoIds = mutableListOf<Long>()

    var errorFactory: (() -> RuntimeException)? = null
    var addTodoGate: CompletableDeferred<Unit>? = null

    private val table = MutableStateFlow<List<TodoItem>>(emptyList())

    private var nextId = 1L
    private var completedAtCounter = 0L

    override fun observeTodos(): Flow<List<TodoItem>> = table

    override suspend fun addTodo(title: String): Long {
        addTodoTitles += title
        addTodoGate?.await()
        errorFactory?.let { throw it() }
        val id = nextId++
        table.value = sorted(table.value + TodoItem(id = id, title = title, isCompleted = false))
        return id
    }

    override suspend fun completeTodo(id: Long): CompleteOutcome {
        completeTodoIds += id
        errorFactory?.let { throw it() }
        val target = table.value.firstOrNull { it.id == id } ?: return CompleteOutcome.NotFound
        if (target.isCompleted) return CompleteOutcome.AlreadyCompleted
        completedAtCounter += 1L
        table.value = sorted(
            table.value.map {
                if (it.id == id) it.copy(isCompleted = true, completedAtMillis = completedAtCounter) else it
            },
        )
        return CompleteOutcome.Completed
    }

    override suspend fun removeTodo(id: Long): Boolean {
        removeTodoIds += id
        errorFactory?.let { throw it() }
        val target = table.value.firstOrNull { it.id == id } ?: return false
        if (target.isCompleted) return false
        table.value = table.value.filterNot { it.id == id }
        return true
    }

    private fun sorted(items: List<TodoItem>): List<TodoItem> {
        val (completed, pending) = items.partition { it.isCompleted }
        return pending.sortedByDescending { it.id } + completed.sortedByDescending { it.completedAtMillis ?: 0L }
    }
}
