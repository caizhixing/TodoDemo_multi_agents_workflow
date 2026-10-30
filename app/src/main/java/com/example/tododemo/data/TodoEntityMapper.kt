package com.example.tododemo.data

import com.example.tododemo.domain.model.TodoItem

fun TodoEntity.toDomain(): TodoItem = TodoItem(
    id = id,
    title = title,
    isCompleted = isCompleted,
    completedAtMillis = completedAtMillis,
)

fun TodoItem.toEntity(createdAtMillis: Long): TodoEntity = TodoEntity(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    completedAtMillis = completedAtMillis,
)
