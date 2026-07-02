package com.kilocode.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kilocode.android.data.repository.TaskManagerRepository
import com.kilocode.android.ui.viewmodel.TaskManagerViewModel
import com.kilocode.android.ui.viewmodel.TaskManagerViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(
    onBack: () -> Unit,
    viewModel: TaskManagerViewModel = viewModel(
        factory = TaskManagerViewModelFactory(TaskManagerRepository(LocalContext.current))
    )
) {
    val tasks by viewModel.tasks.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshTasks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            viewModel.refreshTasks()
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(tasks) { task ->
                ListItem(
                    headlineContent = { Text(task.description ?: "No description") },
                    supportingContent = { 
                        Text(
                            text = "Status: ${task.status}",
                            color = if (task.status == "FAILED") Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Row {
                            if (task.status == "RUNNING" || task.status == "ENQUEUED") {
                                IconButton(onClick = { 
                                    scope.launch {
                                        task.id?.let {
                                            viewModel.cancelTask(it)
                                            viewModel.refreshTasks()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Rounded.Close, "Cancel")
                                }
                            } else if (task.status == "FAILED") {
                                IconButton(onClick = { 
                                    scope.launch {
                                        task.id?.let {
                                            viewModel.retryTask(it)
                                            viewModel.refreshTasks()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Rounded.Replay, "Retry", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            // Always allow deleting completed/failed tasks
                            IconButton(onClick = { 
                                scope.launch {
                                    task.id?.let {
                                        viewModel.deleteTask(it)
                                        viewModel.refreshTasks()
                                    }
                                }
                            }) {
                                Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
        }
    }
}
