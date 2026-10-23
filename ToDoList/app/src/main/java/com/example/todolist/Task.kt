package com.example.todolist

data class Task(
    val id: Long,
    val title: String,
    val done: Boolean = false
)
