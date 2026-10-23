package com.example.todolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<TaskUiState> = repository.observeTasks()
        .map<List<Task>, TaskUiState> { TaskUiState.Success(it) }
        .catch { emit(TaskUiState.Error(it.message ?: "Eroare necunoscută")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskUiState.Loading)

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    fun onDraftChange(text: String) {
        _draft.value = text
    }

    fun addTask() {
        val title = _draft.value.trim()
        if (title.isEmpty()) return
        _draft.value = ""
        viewModelScope.launch { repository.addTask(title) }
    }

    fun setDone(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setDone(id, done) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.deleteTask(id) }
    }
}
