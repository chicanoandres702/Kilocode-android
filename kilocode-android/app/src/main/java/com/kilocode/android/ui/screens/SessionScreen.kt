package com.kilocode.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.repository.SessionRepository
import com.kilocode.android.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    sessionId: String,
    serverUrl: String,
    sharedSecret: String?,
    onBack: () -> Unit,
) {
    val apiClient = remember(serverUrl, sharedSecret) { ApiClient.getInstance(serverUrl, sharedSecret ?: "") }

    val repository = remember(apiClient) { SessionRepository(apiClient) }
    val scope = rememberCoroutineScope()

    val currentSession by repository.currentSession.collectAsState()
    val messages by repository.messages.collectAsState()
    val parts by repository.parts.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val isConnected by repository.isConnected.collectAsState()
    val error by repository.error.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) {
        repository.selectSession(sessionId)
        repository.connectSse(sessionId)
    }

    DisposableEffect(sessionId) {
        onDispose {
            repository.disconnectSse()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSession?.title ?: "Session",
                            maxLines = 1,
                        )
                        StatusChip(
                            text = if (isConnected) "Live" else "Offline",
                            isOnline = isConnected,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            PromptInput(
                onSend = { text ->
                    scope.launch {
                        repository.sendPrompt(sessionId, text)
                    }
                },
                isLoading = isLoading,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            error?.let { errorMsg ->
                ErrorCard(
                    message = errorMsg,
                    onRetry = { repository.clearError() },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    items = messages,
                    key = { it.id ?: it.sessionID ?: "" },
                ) { message ->
                    val messageParts = message.id?.let { parts[it] } ?: emptyList()
                    MessageBubble(
                        isUser = message.role == "user",
                        parts = messageParts,
                    )
                }

                if (isLoading && messages.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}
