package com.example.todolist

sealed interface TaskUiState {
    data object Loading : TaskUiState
    data class Success(val tasks: List<Task>) : TaskUiState
    data class Error(val message: String) : TaskUiState
}
