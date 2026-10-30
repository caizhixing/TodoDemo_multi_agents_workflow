package com.example.tododemo.domain.model

data class TodoItem(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val completedAtMillis: Long? = null,
)
