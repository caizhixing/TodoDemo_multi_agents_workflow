package com.example.tododemo.data

import com.example.tododemo.domain.model.TodoItem
import com.example.tododemo.domain.repository.CompleteOutcome
import com.example.tododemo.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodoRepositoryImpl(
    private val dao: TodoDao,
) : TodoRepository {

    override fun observeTodos(): Flow<List<TodoItem>> = dao.observeAll().map(::toSortedTodoItems)

    override suspend fun addTodo(title: String): Long = dao.insert(
        TodoEntity(title = title, isCompleted = false, createdAtMillis = System.currentTimeMillis()),
    )

    override suspend fun completeTodo(id: Long): CompleteOutcome {
        val entity = dao.findById(id) ?: return CompleteOutcome.NotFound
        if (entity.isCompleted) return CompleteOutcome.AlreadyCompleted
        val completed = entity.toDomain()
            .copy(isCompleted = true, completedAtMillis = System.currentTimeMillis())
            .toEntity(createdAtMillis = entity.createdAtMillis)
        dao.update(completed)
        return CompleteOutcome.Completed
    }

    override suspend fun removeTodo(id: Long): Boolean {
        val entity = dao.findById(id) ?: return false
        if (entity.isCompleted) return false
        dao.deleteById(id)
        return true
    }

    private fun toSortedTodoItems(entities: List<TodoEntity>): List<TodoItem> {
        val (completed, pending) = entities.partition { it.isCompleted }
        return pending.sortedByDescending { it.createdAtMillis }.map { it.toDomain() } +
            completed.sortedByDescending { it.completedAtMillis ?: 0L }.map { it.toDomain() }
    }
}
