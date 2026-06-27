package com.kilocode.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.FileNode
import com.kilocode.android.ui.components.*
import com.kilocode.android.ui.viewmodel.SessionViewModel
import com.kilocode.android.ui.viewmodel.SessionViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverUrl: String,
    sharedSecret: String?,
    onNavigateToSession: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRepos: () -> Unit,
    viewModel: SessionViewModel = viewModel(
        key     = "$serverUrl|$sharedSecret",
        factory = SessionViewModelFactory(ApiClient.getInstance(serverUrl, sharedSecret ?: "")),
    ),
) {
    val scope     = rememberCoroutineScope()
    val sessions  by viewModel.sessions.collectAsState()
    val folders   by viewModel.folders.collectAsState()
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingFolders by viewModel.isLoadingFolders.collectAsState()
    val directoryExists by viewModel.directoryExists.collectAsState()
    val isCheckingDirectory by viewModel.isCheckingDirectory.collectAsState()
    val error     by viewModel.error.collectAsState()

    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet     by remember { mutableStateOf(false) }
    var directoryPath by remember { mutableStateOf("/") }
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    // Check directory and load sessions on first composition
    LaunchedEffect(Unit) {
        viewModel.loadAndCheckDirectory("/")
    }

    // Reload sessions when directory changes (only if directory exists)
    LaunchedEffect(currentDirectory) {
        if (directoryExists == true) {
            viewModel.loadSessions(currentDirectory)
        }
    }

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
                    IconButton(onClick = onNavigateToRepos) {
                        Icon(Icons.Rounded.Cloud, "Repositories", modifier = Modifier.size(20.dp))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Folder Browser ─────────────────────────────────────────────────
            FolderBrowser(
                currentDirectory = currentDirectory,
                folders          = folders,
                isLoading        = isLoadingFolders,
                onFolderClick    = { folder ->
                    val folderPath = folder.absolute ?: folder.path
                    viewModel.navigateToFolder(folderPath)
                },
                onNavigateUp     = { viewModel.navigateUp() },
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            // ── Session List (scoped to directory) ─────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState  = when {
                        isCheckingDirectory -> "checking"
                        directoryExists == false -> "not_found"
                        isLoading && sessions.isEmpty() -> "loading"
                        else -> "list"
                    },
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label        = "listState",
                ) { state ->
                    when (state) {
                        "checking" -> DirectoryCheckingIndicator()
                        "not_found" -> DirectoryNotFound(
                            path = currentDirectory,
                            onRetry = {
                                scope.launch {
                                    viewModel.clearError()
                                    viewModel.loadAndCheckDirectory(currentDirectory)
                                }
                            },
                            onGoRoot = {
                                scope.launch {
                                    viewModel.clearError()
                                    viewModel.navigateToFolder("/")
                                }
                            }
                        )
                        "loading" -> SessionListSkeleton()
                        else -> SessionList(
                            sessions        = sessions,
                            currentDirectory = currentDirectory,
                            onSessionClick  = onNavigateToSession,
                            onNewSession    = { showSheet = true },
                            onDeleteSession = viewModel::deleteSession,
                        )
                    }
                }

                if (error != null && directoryExists != false) {
                    error?.let { msg ->
                        ErrorCard(
                            message = msg,
                            onRetry = { scope.launch { viewModel.clearError(); viewModel.loadAndCheckDirectory(currentDirectory) } },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
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
                    placeholder   = { Text(currentDirectory) },
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

@Composable
fun FolderBrowser(
    currentDirectory: String,
    folders: List<FileNode>,
    isLoading: Boolean,
    onFolderClick: (FileNode) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // Breadcrumb / current path with up button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentDirectory,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (currentDirectory != "/") {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Go up",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Folder list
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.5.dp,
                )
            }
        } else {
            val directories = folders.filter { it.isDirectory }
            if (directories.isEmpty()) {
                Text(
                    text = "No subdirectories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                directories.forEach { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFolderClick(folder) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoryCheckingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Checking directory…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun DirectoryNotFound(
    path: String,
    onRetry: () -> Unit,
    onGoRoot: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            modifier = Modifier.size(60.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.FolderOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Directory not found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry", fontSize = 12.sp)
            }
            Button(onClick = onGoRoot) {
                Icon(Icons.Rounded.Home, null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Go to root", fontSize = 12.sp)
            }
        }
    }
}
