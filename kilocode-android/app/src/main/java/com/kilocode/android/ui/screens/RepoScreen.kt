package com.kilocode.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.RepoEntry
import com.kilocode.android.data.repository.RepoRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoScreen(
    serverUrl: String,
    apiServerUrl: String,
    sharedSecret: String?,
    onRepoSelected: (String, String) -> Unit,
) {
    val apiClient = remember(apiServerUrl, sharedSecret) { ApiClient.getInstance(apiServerUrl, sharedSecret ?: "") }
    val repoRepository = remember(apiClient) { RepoRepository(apiClient) }
    val scope = rememberCoroutineScope()

    var repoInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val isLoading by repoRepository.isLoading.collectAsState()
    val error by repoRepository.error.collectAsState()
    val clonedRepos by repoRepository.clonedRepos.collectAsState()

    // Local search results
    var localSearchResults by remember { mutableStateOf<List<RepoEntry>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Load existing repos on first composition
    LaunchedEffect(Unit) {
        repoRepository.listLocalRepos()
    }

    fun performSearch() {
        if (searchQuery.isBlank()) {
            localSearchResults = emptyList()
            return
        }
        scope.launch {
            isSearching = true
            try {
                val results = repoRepository.searchGitHubRepos(searchQuery.trim())
                // Only show repos that match owner/repo format
                localSearchResults = results.filter { it.name.contains("/") }
            } catch (_: Exception) {
                localSearchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repositories") },
                actions = {
                    IconButton(onClick = { /* refresh */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Your Repos") },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Search GitHub") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // ── Your Repos tab ──────────────────────────────────────────────

                // Create repo input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Create a new repository",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create a fresh repository directory for new projects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = repoInput,
                            onValueChange = { repoInput = it },
                            placeholder = { Text("e.g. my-new-project") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            leadingIcon = {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (repoInput.isNotBlank()) {
                                        scope.launch {
                                            val result = repoRepository.createRepo(repoInput.trim())
                                            if (result.isSuccess) {
                                                repoInput = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading && repoInput.isNotBlank()
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create")
                            }
                            Button(
                                onClick = {
                                    if (repoInput.isNotBlank()) {
                                        scope.launch {
                                            val result = repoRepository.cloneRepo(repoInput.trim())
                                            if (result.isSuccess) {
                                                repoInput = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading && repoInput.isNotBlank()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clone")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error display
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Existing repos
                Text(
                    "Your repositories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (clonedRepos.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No repositories yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Clone a repository above to get started",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(clonedRepos) { repo ->
                            val isGitHub = repo.source == "github"
                            val repoName = if (isGitHub) repo.name else repo.name.replace("_", "/")
                            val repoPath = repo.path ?: "/tmp/kilo-repos/${repo.name.replace("/", "_")}"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isGitHub) {
                                            scope.launch {
                                                val result = repoRepository.cloneRepo(repo.name)
                                                if (result.isSuccess) {
                                                    repoRepository.setCurrentRepo(result.getOrNull())
                                                    val clonedPath = "/tmp/kilo-repos/${result.getOrNull() ?: repo.name.replace("/", "_")}"
                                                    onRepoSelected(result.getOrNull() ?: repo.name, clonedPath)
                                                }
                                            }
                                        } else {
                                            // Local repo — just navigate to its sessions
                                            onRepoSelected(repo.name, repoPath)
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isGitHub) Icons.Default.Link else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = repoName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (isGitHub && !repo.description.isNullOrBlank()) {
                                            Text(
                                                text = repo.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = if (isGitHub) "GitHub • Tap to clone" else repoPath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isGitHub) {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = "Clone",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Search GitHub tab ────────────────────────────────────────────

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search GitHub repos...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSearching,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { performSearch() }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { performSearch() },
                        enabled = !isSearching && searchQuery.isNotBlank()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (isSearching) "Searching..." else "Search")
                    }
                }

                // Error display
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (localSearchResults.isEmpty() && !isSearching && searchQuery.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No results found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Try a different search term",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (localSearchResults.isNotEmpty()) {
                    Text(
                        "Search results (${localSearchResults.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(localSearchResults) { repo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val result = repoRepository.cloneRepo(repo.name)
                                            if (result.isSuccess) {
                                                repoRepository.setCurrentRepo(result.getOrNull())
                                                val repoPath = "/tmp/kilo-repos/${repo.name.replace("/", "_")}"
                                                onRepoSelected(result.getOrNull() ?: repo.name, repoPath)
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = repo.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (!repo.description.isNullOrBlank()) {
                                            Text(
                                                text = repo.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                        if (repo.stars > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${repo.stars}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = "Clone",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Initial empty state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Search GitHub repositories",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Enter a keyword and tap Search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
