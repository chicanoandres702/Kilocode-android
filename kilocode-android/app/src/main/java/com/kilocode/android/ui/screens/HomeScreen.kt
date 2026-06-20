package com.kilocode.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
        key     = "$serverUrl|$sharedSecret",
        factory = SessionViewModelFactory(ApiClient.getInstance(serverUrl, sharedSecret ?: "")),
    ),
) {
    val scope     = rememberCoroutineScope()
    val sessions  by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()

    // Bottom sheet state — replaces the FAB dialog
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet    by remember { mutableStateOf(false) }
    var directoryPath by remember { mutableStateOf("/") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { viewModel.loadSessions() }

    fun createSession() {
        scope.launch {
            sheetState.hide()
            showSheet = false
            val session = viewModel.createSession(directoryPath.ifBlank { "/" })
            directoryPath = "/"
            session?.id?.let(onNavigateToSession)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Kilo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        StatusChip(
                            text     = if (error == null) "Connected" else "Disconnected",
                            isOnline = error == null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        // FAB opens the bottom sheet directly — no dialog tap-through
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showSheet = true },
                shape          = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                elevation      = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New session")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && sessions.isEmpty() -> LoadingIndicator(message = "Loading sessions…")
                else -> SessionList(
                    sessions        = sessions,
                    onSessionClick  = onNavigateToSession,
                    onNewSession    = { showSheet = true },
                    onDeleteSession = viewModel::deleteSession,
                )
            }

            // Error banner — overlaid at bottom when sessions are present
            AnimatedVisibility(
                visible  = error != null && sessions.isNotEmpty(),
                enter    = fadeIn() + slideInVertically { it },
                exit     = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                error?.let { msg ->
                    ErrorCard(message = msg, onRetry = {
                        scope.launch { viewModel.clearError(); viewModel.loadSessions() }
                    })
                }
            }

            // Full-screen error when nothing else to show
            if (error != null && sessions.isEmpty() && !isLoading) {
                ErrorCard(message = error ?: "Unknown error", onRetry = {
                    scope.launch { viewModel.clearError(); viewModel.loadSessions() }
                })
            }
        }
    }

    // ── New session bottom sheet ──────────────────────────────────────────────
    // Slides up and auto-focuses the path field, keyboard opens immediately.
    // One tap on "Create" (or hitting the keyboard's action) starts the session.
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; directoryPath = "/" },
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color    = MaterialTheme.colorScheme.outlineVariant,
                        shape    = RoundedCornerShape(2.dp),
                        modifier = Modifier.size(width = 32.dp, height = 4.dp),
                    ) {}
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Text(
                    text       = "New session",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(bottom = 16.dp, top = 4.dp),
                )
                OutlinedTextField(
                    value         = directoryPath,
                    onValueChange = { directoryPath = it },
                    label         = { Text("Working directory") },
                    placeholder   = { Text("/") },
                    singleLine    = true,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape         = RoundedCornerShape(14.dp),
                    leadingIcon   = {
                        Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { createSession() }),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick  = { createSession() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                ) {
                    Text("Create session", fontWeight = FontWeight.SemiBold)
                }
            }

            // Auto-focus + open keyboard as sheet finishes appearing
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
        }
    }
}
