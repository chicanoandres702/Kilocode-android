package com.kilocode.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val apiClient  = remember(serverUrl, sharedSecret) { ApiClient.getInstance(serverUrl, sharedSecret ?: "") }
    val repository = remember(apiClient) { SessionRepository(apiClient) }
    val scope      = rememberCoroutineScope()

    val currentSession by repository.currentSession.collectAsState()
    val messages       by repository.messages.collectAsState()
    val parts          by repository.parts.collectAsState()
    val agents         by repository.agents.collectAsState()
    val isLoading      by repository.isLoading.collectAsState()
    val isConnected    by repository.isConnected.collectAsState()
    val error          by repository.error.collectAsState()

    val listState     = rememberLazyListState()
    var selectedAgent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit)   { repository.listAgents() }
    LaunchedEffect(agents) {
        selectedAgent = agents.firstOrNull { it.name == selectedAgent }?.name
            ?: agents.firstOrNull { it.mode == "primary" || it.mode == "all" }?.name
            ?: agents.firstOrNull()?.name
    }
    LaunchedEffect(sessionId) {
        repository.selectSession(sessionId)
        repository.connectSse(sessionId)
    }
    DisposableEffect(sessionId) { onDispose { repository.disconnectSse() } }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            // ── Single-line top bar ───────────────────────────────────────────
            // Title only + status chip + back. Agent lives in the prompt field.
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text       = currentSession?.title ?: "Session",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false),
                        )
                        StatusChip(
                            text     = if (isConnected) "Live" else "Offline",
                            isOnline = isConnected,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier           = Modifier.size(22.dp),
                        )
                    }
                },
                actions = {
                    // Spinner sits here only while streaming; disappears otherwise
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier
                                .padding(end = 14.dp)
                                .size(16.dp),
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
                onSend          = { text -> scope.launch { repository.sendPrompt(sessionId, text, selectedAgent) } },
                isLoading       = isLoading,
                agents          = agents,
                selectedAgent   = selectedAgent,
                onAgentSelected = { selectedAgent = it },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Error banner pinned below the top bar
            error?.let { msg ->
                ErrorCard(
                    message = msg,
                    onRetry = {
                        scope.launch {
                            repository.clearError()
                            repository.selectSession(sessionId)
                        }
                    },
                )
            }

            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                state          = listState,
                contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
            ) {
                // ── Empty state ───────────────────────────────────────────────
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier            = Modifier
                                .fillParentMaxHeight()
                                .padding(horizontal = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Surface(
                                shape    = RoundedCornerShape(28.dp),
                                color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.size(68.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector        = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier           = Modifier.size(30.dp),
                                        tint               = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text       = "Ask anything",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text  = "Kilo will plan, write, and run code for you.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                // ── Messages ──────────────────────────────────────────────────
                items(
                    items = messages,
                    key   = { it.id ?: it.sessionID ?: "" },
                ) { message ->
                    val messageParts = message.id?.let { parts[it] } ?: emptyList()
                    MessageBubble(
                        isUser = message.role == "user",
                        parts  = messageParts,
                        agent  = message.agent,
                    )
                }

                // ── Typing indicator ──────────────────────────────────────────
                if (isLoading && messages.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 2.dp),
                        ) {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}
