package com.example.tododemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import com.example.tododemo.data.TodoDatabase
import com.example.tododemo.data.TodoRepositoryImpl
import com.example.tododemo.ui.TodoListScreen
import com.example.tododemo.ui.TodoListViewModel

class MainActivity : ComponentActivity() {

    private val database: TodoDatabase by lazy {
        Room.databaseBuilder(applicationContext, TodoDatabase::class.java, "todos.db").build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TodoRepositoryImpl(database.todoDao())
        setContent {
            val viewModel: TodoListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { TodoListViewModel(repository) }
                },
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TodoListScreen(
                uiState = uiState,
                onAddTodo = viewModel::addTodo,
                onCompleteTodo = viewModel::completeTodo,
                onRemoveTodo = viewModel::removeTodo,
            )
        }
    }
}
