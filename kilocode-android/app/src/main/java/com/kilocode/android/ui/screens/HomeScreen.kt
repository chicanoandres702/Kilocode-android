package com.kilocode.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kilocode.android.BuildConfig
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.repository.SessionRepository
import com.kilocode.android.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverUrl: String,
    onSessionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val apiClient = remember { ApiClient.getInstance(serverUrl, BuildConfig.KILO_SHARED_SECRET) }
    val repository = remember { SessionRepository(apiClient) }
    val scope = rememberCoroutineScope()

    val sessions by repository.sessions.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val error by repository.error.collectAsState()

    var showNewSessionDialog by remember { mutableStateOf(false) }
    var directoryPath by remember { mutableStateOf("/") }

    LaunchedEffect(Unit) {
        repository.loadSessions()
    }

    fun createSession() {
        scope.launch {
            val session = repository.createSession(directoryPath.ifBlank { "/" })
            if (session != null) {
                onSessionClick(session.id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kilo Code")
                        StatusChip(
                            text = if (error == null) "Connected" else "Disconnected",
                            isOnline = error == null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewSessionDialog = true },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Session",
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isLoading && sessions.isEmpty() -> {
                    LoadingIndicator(message = "Loading sessions...")
                }
                error != null && sessions.isEmpty() -> {
                    ErrorCard(
                        message = error ?: "Unknown error",
                        onRetry = {
                            scope.launch {
                                repository.clearError()
                                repository.loadSessions()
                            }
                        },
                    )
                }
                else -> {
                    SessionList(
                        sessions = sessions,
                        onSessionClick = onSessionClick,
                        onNewSession = { showNewSessionDialog = true },
                        onDeleteSession = { sessionId ->
                            scope.launch {
                                repository.deleteSession(sessionId)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showNewSessionDialog) {
        AlertDialog(
            onDismissRequest = { showNewSessionDialog = false },
            title = { Text("New Session") },
            text = {
                Column {
                    Text(
                        text = "Enter the working directory for this session:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = directoryPath,
                        onValueChange = { directoryPath = it },
                        label = { Text("Directory Path") },
                        placeholder = { Text("/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewSessionDialog = false
                        createSession()
                    },
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
