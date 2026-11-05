package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TodoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(vm: TodoViewModel = viewModel()) {
    var text by rememberSaveable { mutableStateOf("") }
    val remaining = vm.todos.count { !it.done }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("To-Do List ($remaining rămase)") })
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Task nou...") },
                    singleLine = true
                )
                Button(onClick = {
                    vm.add(text)
                    text = ""
                }) { Text("Adaugă") }
            }

            vm.error.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(vm.todos, key = { it.id }) { todo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = todo.done, onCheckedChange = { vm.toggle(todo.id) })
                        Text(
                            text = todo.title,
                            modifier = Modifier.weight(1f),
                            textDecoration = if (todo.done) TextDecoration.LineThrough else null
                        )
                        TextButton(onClick = { vm.delete(todo.id) }) { Text("Șterge") }
                    }
                }
            }

            if (vm.todos.any { it.done }) {
                TextButton(onClick = { vm.clearDone() }) { Text("Șterge cele terminate") }
            }
        }
    }
}
