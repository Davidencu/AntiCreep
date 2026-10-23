package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.todolist.ui.theme.ToDoListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoListTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                // The repository is created once with the ViewModel, so it outlives rotation.
                val taskViewModel: TaskListViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { TaskListViewModel(InMemoryTaskRepository()) }
                    }
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = {},
                                label = { Text("Taskuri") }
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = {},
                                label = { Text("State lab") }
                            )
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    if (tab == 0) {
                        TodoScreen(taskViewModel, contentModifier)
                    } else {
                        StateLabScreen(contentModifier)
                    }
                }
            }
        }
    }
}
