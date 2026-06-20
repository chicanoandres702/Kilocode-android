package com.kilocode.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
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
    val agents by repository.agents.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val isConnected by repository.isConnected.collectAsState()
    val error by repository.error.collectAsState()

    val listState = rememberLazyListState()
    var selectedAgent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.listAgents()
    }

    LaunchedEffect(agents) {
        val currentAgent = selectedAgent
        selectedAgent = agents.firstOrNull { it.name == currentAgent }?.name
            ?: agents.firstOrNull { it.mode == "primary" || it.mode == "all" }?.name
            ?: agents.firstOrNull()?.name
    }

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
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSession?.title ?: "Active Session",
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${selectedAgent ?: "Default Agent"}",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusChip(
                                text = if (isConnected) "Live" else "Offline",
                                isOnline = isConnected,
                            )
                        }
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
                        repository.sendPrompt(sessionId, text, selectedAgent)
                    }
                },
                isLoading = isLoading,
                agents = agents,
                selectedAgent = selectedAgent,
                onAgentSelected = { selectedAgent = it },
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
                    onRetry = {
                        scope.launch {
                            repository.clearError()
                            repository.selectSession(sessionId)
                        }
                    },
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Ask anything. Kilo will handle the rest.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Choose an agent and send a prompt to begin.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                items(
                    items = messages,
                    key = { it.id ?: it.sessionID ?: "" },
                ) { message ->
                    val messageParts = message.id?.let { parts[it] } ?: emptyList()
                    MessageBubble(
                        isUser = message.role == "user",
                        parts = messageParts,
                        agent = message.agent,
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
