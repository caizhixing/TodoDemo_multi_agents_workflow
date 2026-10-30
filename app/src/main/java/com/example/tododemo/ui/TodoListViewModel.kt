package com.example.tododemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tododemo.domain.ProgressCalculator
import com.example.tododemo.domain.RankResolver
import com.example.tododemo.domain.model.TodoItem
import com.example.tododemo.domain.repository.TodoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoListViewModel(private val repository: TodoRepository) : ViewModel() {

    private val isBusy = MutableStateFlow(false)

    val uiState: StateFlow<TodoListUiState> =
        combine(repository.observeTodos(), isBusy, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(emptyList(), false))

    fun addTodo(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        launchWrite { repository.addTodo(trimmed) }
    }

    fun completeTodo(id: Long) = launchWrite { repository.completeTodo(id) }

    fun removeTodo(id: Long) = launchWrite { repository.removeTodo(id) }

    private fun launchWrite(block: suspend () -> Unit) {
        if (isBusy.value) return
        isBusy.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 行为 3.8：repository 故障兜底——吞掉异常，uiState 保持上一有效值
            } finally {
                isBusy.value = false
            }
        }
    }

    private fun toUiState(items: List<TodoItem>, busy: Boolean): TodoListUiState {
        val progress = ProgressCalculator.calculate(items)
        return TodoListUiState(
            items = items,
            totalPoints = progress.totalPoints,
            completedCount = progress.completedCount,
            rankTitle = RankResolver.displayName(progress.rank),
            isBusy = busy,
        )
    }
}
