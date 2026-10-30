package com.example.tododemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null,
)
