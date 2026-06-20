package com.kilocode.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.ui.components.*
import com.kilocode.android.ui.viewmodel.SessionViewModel
import com.kilocode.android.ui.viewmodel.SessionViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverUrl: String,
    sharedSecret: String?,
    onNavigateToSession: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: SessionViewModel = viewModel(
        key = "$serverUrl|$sharedSecret",
        factory = SessionViewModelFactory(ApiClient.getInstance(serverUrl, sharedSecret ?: "")),
    )
) {
    val scope = rememberCoroutineScope()
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showNewSessionDialog by remember { mutableStateOf(false) }
    var directoryPath by remember { mutableStateOf("/") }

    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    fun createSession() {
        scope.launch {
            val session = viewModel.createSession(directoryPath.ifBlank { "/" })
            if (session != null) {
                onNavigateToSession(session.id)
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
                    IconButton(onClick = onNavigateToSettings) {
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
                                viewModel.clearError()
                                viewModel.loadSessions()
                            }
                        },
                    )
                }
                else -> {
                    SessionList(
                        sessions = sessions,
                        onSessionClick = onNavigateToSession,
                        onNewSession = { showNewSessionDialog = true },
                        onDeleteSession = { sessionId ->
                            viewModel.deleteSession(sessionId)
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
