package com.example.todolist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())

    override fun observeTasks(): Flow<List<Task>> = tasks

    override suspend fun addTask(title: String) {
        tasks.update { it + Task(id = it.size + 1L, title = title) }
    }

    override suspend fun setDone(id: Long, done: Boolean) {
        tasks.update { list -> list.map { if (it.id == id) it.copy(done = done) else it } }
    }

    override suspend fun deleteTask(id: Long) {
        tasks.update { list -> list.filterNot { it.id == id } }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addTask_trimsDraft_emitsSuccess_andClearsDraft() = runTest {
        val viewModel = TaskListViewModel(FakeTaskRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onDraftChange("  Cumpără pâine  ")
        viewModel.addTask()

        assertEquals(
            TaskUiState.Success(listOf(Task(id = 1, title = "Cumpără pâine"))),
            viewModel.uiState.value
        )
        assertEquals("", viewModel.draft.value)
    }
}
