package com.example.tododemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tododemo.domain.model.TodoItem

@Composable
fun TodoListScreen(
    uiState: TodoListUiState,
    onAddTodo: (String) -> Unit,
    onCompleteTodo: (Long) -> Unit,
    onRemoveTodo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "积分 ${uiState.totalPoints} · 称号 ${uiState.rankTitle} · 已完成 ${uiState.completedCount}",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("输入新待办") },
            )
            Button(
                onClick = {
                    onAddTodo(inputText)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !uiState.isBusy,
            ) {
                Text("添加")
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(uiState.items, key = { it.id }) { item ->
                TodoRow(
                    item = item,
                    onCompleteTodo = onCompleteTodo,
                    onRemoveTodo = onRemoveTodo,
                )
            }
        }
    }
}

@Composable
private fun TodoRow(
    item: TodoItem,
    onCompleteTodo: (Long) -> Unit,
    onRemoveTodo: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = { checked -> if (checked) onCompleteTodo(item.id) },
            enabled = !item.isCompleted,
        )
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onRemoveTodo(item.id) },
            enabled = !item.isCompleted,
        ) {
            Text("删除")
        }
    }
}
