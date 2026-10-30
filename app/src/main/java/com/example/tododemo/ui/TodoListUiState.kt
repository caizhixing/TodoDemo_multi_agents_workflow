package com.example.tododemo.ui

import com.example.tododemo.domain.model.TodoItem

data class TodoListUiState(
    val items: List<TodoItem>,
    val totalPoints: Int,
    val completedCount: Int,
    val rankTitle: String,
    val isBusy: Boolean,
)
