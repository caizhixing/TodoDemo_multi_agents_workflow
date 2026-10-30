package com.example.tododemo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTodoDao(
    initial: List<TodoEntity> = emptyList(),
) : TodoDao {

    val table = MutableStateFlow(initial.toList())

    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun observeAll(): Flow<List<TodoEntity>> = table

    override suspend fun insert(entity: TodoEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        table.value = table.value + entity.copy(id = id)
        return id
    }

    override suspend fun update(entity: TodoEntity) {
        table.value = table.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun findById(id: Long): TodoEntity? = table.value.firstOrNull { it.id == id }

    override suspend fun deleteById(id: Long) {
        table.value = table.value.filterNot { it.id == id }
    }
}
