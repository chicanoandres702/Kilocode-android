package com.kilocode.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.Agent
import com.kilocode.android.data.model.ModelOption
import com.kilocode.android.data.model.Part
import com.kilocode.android.data.repository.SessionRepository
import com.kilocode.android.ui.components.*
import kotlinx.coroutines.delay
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
    // Use a singleton/shared repository instance managed via dependency injection or a provider to ensure consistency.
    // For now, reuse the instance if possible or lift it to a higher level in Navigation.
    // Assuming simple singleton access for the prototype:
    val repository = remember(apiClient) { SessionRepository(apiClient) }
    val scope = rememberCoroutineScope()
    
    // Debugging state flow collection
    val messagesState = repository.messages.collectAsState()
    val partsState = repository.parts.collectAsState()
    
    LaunchedEffect(messagesState.value) {
        android.util.Log.d("SessionScreen", "Messages updated: ${messagesState.value.size}")
    }
    
    val currentSession by repository.currentSession.collectAsState()
    val messages = messagesState.value
    val parts = partsState.value
    val agents by repository.agents.collectAsState()
    val models by repository.models.collectAsState()
    val selectedAgent by repository.selectedAgent.collectAsState()
    val selectedModel by repository.selectedModel.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val isConnected by repository.isConnected.collectAsState()
    val error by repository.error.collectAsState()

    val listState = rememberLazyListState()
    var autonomousMode by remember { mutableStateOf(false) }
    var continueGeneration by remember { mutableStateOf(0) }

    val hasPendingWork by remember(parts) {
        derivedStateOf {
            parts.values.flatten().any { part ->
                part.state?.status == "pending" || part.state?.status == "running"
            }
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (info.totalItemsCount - 1)
        }
    }

    val topBarAlpha by animateFloatAsState(
        targetValue = if (listState.firstVisibleItemIndex > 0) 0.92f else 1f,
        animationSpec = tween(200),
        label = "topBarAlpha",
    )

    LaunchedEffect(Unit) {
        repository.listAgents()
        repository.listModels()
    }
    LaunchedEffect(agents) {
        if (selectedAgent == null) {
            val agent = agents.firstOrNull { it.mode == "primary" || it.mode == "all" }
                ?: agents.firstOrNull()
            repository.setSelectedAgent(agent)
        }
    }
    LaunchedEffect(models) {
        if (selectedModel == null && models.isNotEmpty()) {
            val model = models.firstOrNull { it.modelID == "kilo/nex-agi/nex-n2-pro:free" } ?: models.first()
            repository.setSelectedModel(model)
        }
    }
    LaunchedEffect(sessionId) {
        autonomousMode = false
        repository.selectSession(sessionId)
        repository.connectSse(sessionId, repository.currentSession.value?.directory)
    }
    DisposableEffect(sessionId) { onDispose { repository.disconnectSse() } }

    // Improved scroll-to-bottom logic
    LaunchedEffect(messages.size, parts) {
        // If it's a new message, or we are currently near the bottom (typing), scroll to bottom.
        if (messages.isNotEmpty()) {
             listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(autonomousMode, continueGeneration, isLoading, hasPendingWork, messages.size) {
        if (!autonomousMode || isLoading || hasPendingWork || messages.isEmpty()) return@LaunchedEffect
        delay(900)
        if (autonomousMode && !isLoading && !hasPendingWork && messages.isNotEmpty()) {
            repository.sendPrompt(sessionId, "continue", selectedAgent?.name, selectedModel)
            continueGeneration++
        }
    }

    fun sendPrompt(text: String) {
        scope.launch {
            repository.sendPrompt(sessionId, text, selectedAgent?.name, selectedModel)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.graphicsLayer { alpha = topBarAlpha },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = currentSession?.title ?: "Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        StatusChip(text = if (isConnected) "Live" else "Offline", isOnline = isConnected)
                        if (autonomousMode) {
                            AssistChip(
                                onClick = { autonomousMode = false },
                                label = { Text("Auto", fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Rounded.AutoMode, null, Modifier.size(14.dp)) },
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(300)),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 14.dp).size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            PromptInput(
                onSend = { sendPrompt(it) },
                onContinue = { autonomousMode = !autonomousMode },
                onStop = {
                    scope.launch {
                        repository.abortSession(sessionId)
                        autonomousMode = false
                    }
                },
                isLoading = isLoading,
                models = models,
                selectedModel = selectedModel,
                onModelSelected = { repository.setSelectedModel(it) },
                agents = agents,
                selectedAgent = selectedAgent,
                onAgentSelected = { repository.setSelectedAgent(it) },
                autonomousMode = autonomousMode,
                onAutonomousModeChanged = { autonomousMode = it },
                messages = messages,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                error?.let { msg ->
                    ErrorCard(message = msg, onRetry = {
                        scope.launch {
                            repository.clearError()
                            repository.selectSession(sessionId)
                        }
                    })
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 5.dp),
                    state = listState,
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillParentMaxHeight()
                                    .padding(horizontal = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                val float = rememberInfiniteTransition(label = "float")
                                val floatY by float.animateFloat(
                                    initialValue = 0f,
                                    targetValue = -8f,
                                    animationSpec = infiniteRepeatable(
                                        tween(1800, easing = FastOutSlowInEasing),
                                        RepeatMode.Reverse,
                                    ),
                                    label = "floatY",
                                )
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    modifier = Modifier.size(64.dp).graphicsLayer { translationY = floatY },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // TODO: Add app icon placeholder
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Ask anything",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "Kilo will plan, write, and run code.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }

                    items(
                        items = messages,
                        key = { it.id ?: it.sessionID ?: messages.indexOf(it).toString() },
                    ) { message ->
                        val msgParts = message.id?.let { parts[it] } ?: emptyList()
                        MessageBubble(
                            isUser = message.role == "user",
                            parts = msgParts,
                            agent = message.agent,
                            sessionId = message.sessionID ?: "",
                            onOptionSelected = { option -> sendPrompt(option) }
                        )
                    }

                    if (isLoading && messages.isNotEmpty()) {
                        item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, top = 2.dp, bottom = 4.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 4.dp, topEnd = 16.dp,
                                    bottomStart = 16.dp, bottomEnd = 16.dp,
                                ),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            ) {
                                TypingIndicator()
                            }
                        }
                        }
                    }
                }

                AndroidScrollbar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .width(4.dp),
                )

                if (!isAtBottom && messages.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp, 2.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 12.dp),
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, "Scroll to latest", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
