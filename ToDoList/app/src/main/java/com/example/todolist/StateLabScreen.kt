package com.example.todolist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CounterViewModel : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun increment() = _count.update { it + 1 }
}

/**
 * Same stateless counter, three different owners of its state.
 * Increment each one, then rotate the device: see which value survives.
 */
@Composable
fun StateLabScreen(modifier: Modifier = Modifier) {
    val viewModel: CounterViewModel = viewModel()
    val viewModelCount by viewModel.count.collectAsStateWithLifecycle()

    var rememberCount by remember { mutableIntStateOf(0) }
    var saveableCount by rememberSaveable { mutableIntStateOf(0) }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("State lab", style = MaterialTheme.typography.headlineMedium)
        Text("Incrementează fiecare contor, apoi rotește ecranul.")

        Counter("remember (se pierde la rotire)", rememberCount) { rememberCount++ }
        Counter("rememberSaveable (supraviețuiește)", saveableCount) { saveableCount++ }
        Counter("ViewModel (supraviețuiește)", viewModelCount) { viewModel.increment() }

        Text("Formular cu state ridicat", style = MaterialTheme.typography.titleMedium)
        ProfileForm(
            name = name,
            email = email,
            onNameChange = { name = it },
            onEmailChange = { email = it },
            onSubmit = { submitted = "$name <$email>" }
        )
        submitted?.let { Text("Trimis: $it") }
    }
}

/** Stateless: the count and the click handler are owned by the caller. */
@Composable
fun Counter(label: String, count: Int, onIncrement: () -> Unit) {
    Column {
        Text("$label: $count")
        Button(onClick = onIncrement) { Text("+1") }
    }
}

/** Stateless: both fields are hoisted to the caller. */
@Composable
fun ProfileForm(
    name: String,
    email: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nume") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSubmit, enabled = name.isNotBlank() && email.isNotBlank()) {
            Text("Trimite")
        }
    }
}
