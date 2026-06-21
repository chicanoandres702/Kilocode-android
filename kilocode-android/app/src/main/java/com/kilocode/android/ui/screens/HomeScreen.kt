package com.kilocode.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
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

    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet     by remember { mutableStateOf(false) }
    var directoryPath by remember { mutableStateOf("/") }
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logotype with weight contrast
                        Text(
                            "Kilo",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Code",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Light,
                            color      = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        StatusChip(
                            text     = if (error == null) "Connected" else "Disconnected",
                            isOnline = error == null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, "Settings", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showSheet = true },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Shimmer skeleton while first loading; list once data arrives
            AnimatedContent(
                targetState  = isLoading && sessions.isEmpty(),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label        = "listState",
            ) { loading ->
                if (loading) {
                    SessionListSkeleton()
                } else {
                    SessionList(
                        sessions        = sessions,
                        onSessionClick  = onNavigateToSession,
                        onNewSession    = { showSheet = true },
                        onDeleteSession = viewModel::deleteSession,
                    )
                }
            }

            if (error != null) {
                error?.let { msg ->
                    ErrorCard(
                        message = msg,
                        onRetry = { scope.launch { viewModel.clearError(); viewModel.loadSessions() } },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

    // ── New session bottom sheet ───────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; directoryPath = "/" },
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier         = Modifier.padding(top = 10.dp, bottom = 2.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape    = RoundedCornerShape(2.dp),
                        modifier = Modifier.size(width = 28.dp, height = 3.dp),
                    ) {}
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Text(
                    "New session",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(bottom = 14.dp, top = 2.dp),
                )
                OutlinedTextField(
                    value         = directoryPath,
                    onValueChange = { directoryPath = it },
                    label         = { Text("Working directory") },
                    placeholder   = { Text("/") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape         = RoundedCornerShape(14.dp),
                    leadingIcon   = { Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(16.dp)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { createSession() }),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick  = ::createSession,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(14.dp),
                ) {
                    Text("Create session", fontWeight = FontWeight.SemiBold)
                }
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
        }
    }
}
