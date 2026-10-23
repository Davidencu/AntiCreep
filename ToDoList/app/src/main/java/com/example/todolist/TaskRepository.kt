package com.example.todolist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun addTask(title: String)
    suspend fun setDone(id: Long, done: Boolean)
    suspend fun deleteTask(id: Long)
}

class InMemoryTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    override fun observeTasks(): Flow<List<Task>> = tasks.asStateFlow()

    override suspend fun addTask(title: String) {
        val task = Task(id = nextId++, title = title)
        tasks.update { listOf(task) + it }
    }

    override suspend fun setDone(id: Long, done: Boolean) {
        tasks.update { list -> list.map { if (it.id == id) it.copy(done = done) else it } }
    }

    override suspend fun deleteTask(id: Long) {
        tasks.update { list -> list.filterNot { it.id == id } }
    }
}
