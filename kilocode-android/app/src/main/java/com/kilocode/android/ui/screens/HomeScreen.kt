package com.kilocode.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.ui.components.*
import com.kilocode.android.ui.viewmodel.SessionViewModel
import com.kilocode.android.ui.viewmodel.SessionViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverUrl: String,
    sharedSecret: String?,
    initialDirectory: String = "/",
    onNavigateToSession: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    viewModel: SessionViewModel = viewModel(
        key     = "$serverUrl|$sharedSecret|$initialDirectory",
        factory = SessionViewModelFactory(ApiClient.getInstance(serverUrl, sharedSecret ?: "")),
    ),
) {
    val scope     = rememberCoroutineScope()
    val sessions  by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()

    // Derive a friendly repo name from the directory path
    val repoName = remember(initialDirectory) {
        initialDirectory.trimEnd('/').substringAfterLast('/').replace("_", "/")
    }

    // Load sessions for the directory on first composition
    LaunchedEffect(initialDirectory) {
        viewModel.loadSessions(initialDirectory)
    }

    fun createSession() {
        scope.launch {
            val session = viewModel.createSession(initialDirectory)
            session?.id?.let(onNavigateToSession)
        }
    }

    // FAB scale — spring-in on first frame
    var fabVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { fabVisible = true }
    val fabScale by animateFloatAsState(
        targetValue   = if (fabVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label         = "fabScale",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                repoName,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            StatusChip(
                                text     = if (error == null) "Connected" else "Disconnected",
                                isOnline = error == null,
                            )
                        }
                        Text(
                            "${sessions.size} session${if (sessions.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToRepos) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to repos", modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        println("D/HomeScreen: Planning clicked")
                        onNavigateToPlanning()
                    }) {
                        Icon(Icons.Rounded.TaskAlt, "Planning", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, "Settings", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { createSession() },
                shape          = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                elevation      = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier       = Modifier.scale(fabScale),
            ) {
                Icon(Icons.Rounded.Add, "New session")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            // ── Session List ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState  = when {
                        isLoading && sessions.isEmpty() -> "loading"
                        else -> "list"
                    },
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label        = "listState",
                ) { state ->
                    when (state) {
                        "loading" -> SessionListSkeleton()
                        else -> SessionList(
                            sessions        = sessions,
                            currentDirectory = initialDirectory,
                            onSessionClick  = onNavigateToSession,
                            onNewSession    = { createSession() },
                            onDeleteSession = viewModel::deleteSession,
                        )
                    }
                }

                if (error != null) {
                    error?.let { msg ->
                        ErrorCard(
                            message = msg,
                            onRetry = { scope.launch { viewModel.clearError(); viewModel.loadSessions(initialDirectory) } },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}
