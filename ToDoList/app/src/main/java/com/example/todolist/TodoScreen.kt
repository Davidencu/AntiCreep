package com.example.todolist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Stateful: the only place that knows about the ViewModel. */
@Composable
fun TodoScreen(viewModel: TaskListViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    TodoContent(
        uiState = uiState,
        draft = draft,
        onDraftChange = viewModel::onDraftChange,
        onAdd = viewModel::addTask,
        onToggle = viewModel::setDone,
        onDelete = viewModel::deleteTask,
        modifier = modifier
    )
}

/** Stateless: renders what it is given and reports events upward. */
@Composable
fun TodoContent(
    uiState: TaskUiState,
    draft: String,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (id: Long, done: Boolean) -> Unit,
    onDelete: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "To-Do List", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = { Text("Task nou") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onAdd) { Text("Adaugă") }
        }

        when (uiState) {
            TaskUiState.Loading -> Text("Se încarcă...")
            is TaskUiState.Error -> Text("Eroare: ${uiState.message}")
            is TaskUiState.Success -> {
                if (uiState.tasks.isEmpty()) {
                    Text("Nu ai niciun task. Adaugă unul mai sus.")
                }
                LazyColumn {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskRow(task, onToggle, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: (id: Long, done: Boolean) -> Unit,
    onDelete: (id: Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle(task.id, it) })
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.done) TextDecoration.LineThrough else null
        )
        TextButton(onClick = { onDelete(task.id) }) { Text("Șterge") }
    }
}
