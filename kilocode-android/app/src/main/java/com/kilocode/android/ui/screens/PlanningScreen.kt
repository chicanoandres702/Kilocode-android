package com.kilocode.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.Issue
import com.kilocode.android.data.model.Milestone
import com.kilocode.android.data.repository.PlanningRepository
import com.kilocode.android.ui.components.ErrorCard
import com.kilocode.android.ui.components.LoadingIndicator
import com.kilocode.android.ui.components.StatusChip
import kotlinx.coroutines.launch

/**
 * Planning screen — cascading milestone → issue → description UI.
 * Users browse milestones, expand to see issues, tap an issue to see full details.
 * Follows AIDDE protocol: issue #44.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    serverUrl: String,
    apiServerUrl: String,
    sharedSecret: String?,
    onBack: () -> Unit,
    onNavigateToWizard: () -> Unit,
) {
    val apiClient = remember(apiServerUrl, sharedSecret) {
        ApiClient.getInstance(apiServerUrl, sharedSecret ?: "")
    }
    val repository = remember(apiClient) { PlanningRepository(apiClient) }
    Log.d("PlanningScreen", "PlanningScreen composed")
    val scope = rememberCoroutineScope()

    val milestones by repository.milestones.collectAsState()
    val issues by repository.issues.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val error by repository.error.collectAsState()

    var expandedMilestone by remember { mutableStateOf<Int?>(null) }
    var selectedIssue by remember { mutableStateOf<Issue?>(null) }

    // Load milestones on first composition
    LaunchedEffect(Unit) {
        Log.d("PlanningScreen", "Loading milestones")
        repository.getMilestones()
    }

    // Load issues when a milestone is expanded
    LaunchedEffect(expandedMilestone) {
        expandedMilestone?.let { number ->
            repository.getIssues(number)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Planning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${milestones.size} milestone${if (milestones.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToWizard) {
                        Icon(Icons.Rounded.Add, "Create new", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Error banner
            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                error?.let { appError ->
                    ErrorCard(
                        message = appError.message ?: "Unknown error",
                        onRetry = {
                            scope.launch {
                                repository.clearError()
                                repository.getMilestones(forceRefresh = true)
                            }
                        },
                    )
                }
            }

            if (isLoading && milestones.isEmpty()) {
                LoadingIndicator(message = "Loading milestones…")
            } else if (milestones.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.TaskAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No milestones yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap + to create your first milestone and start planning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            } else {
                // Milestone list with cascading issues
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        items = milestones,
                        key = { it.number },
                    ) { milestone ->
                        val isExpanded = expandedMilestone == milestone.number
                        val milestoneIssues = issues[milestone.number] ?: emptyList()

                        Column {
                            // Milestone header
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedMilestone = if (isExpanded) null else milestone.number
                                    },
                                color = if (isExpanded)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (isExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = milestone.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (milestone.description != null) {
                                            Text(
                                                text = milestone.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusChip(
                                        text = "${milestone.openIssues}/${milestone.totalIssues}",
                                        isOnline = !milestone.isClosed,
                                    )
                                }
                            }

                            // Expanded issues list
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 24.dp),
                                ) {
                                    if (milestoneIssues.isEmpty() && !isLoading) {
                                        Text(
                                            "No issues in this milestone",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(16.dp),
                                        )
                                    }
                                    milestoneIssues.forEach { issue ->
                                        IssueRow(
                                            issue = issue,
                                            isSelected = selectedIssue?.number == issue.number,
                                            onClick = {
                                                selectedIssue = if (selectedIssue?.number == issue.number) null else issue
                                            },
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }
    }

    // Issue detail bottom sheet / dialog
    selectedIssue?.let { issue ->
        IssueDetailDialog(
            issue = issue,
            onDismiss = { selectedIssue = null },
            onStateChange = { newState ->
                scope.launch {
                    repository.updateIssueState(issue.number, newState)
                    selectedIssue = null
                }
            },
        )
    }
}

@Composable
private fun IssueRow(
    issue: Issue,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (issue.isClosed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (issue.isClosed)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = issue.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (issue.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = issue.labels.first().name,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueDetailDialog(
    issue: Issue,
    onDismiss: () -> Unit,
    onStateChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "#${issue.number} — ${issue.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                if (!issue.body.isNullOrBlank()) {
                    Text(
                        text = issue.body,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = "No description provided.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(
                        text = if (issue.isClosed) "Closed" else "Open",
                        isOnline = !issue.isClosed,
                    )
                    if (issue.comments > 0) {
                        StatusChip(
                            text = "${issue.comments} comment${if (issue.comments != 1) "s" else ""}",
                            isOnline = true,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (issue.isClosed) {
                TextButton(onClick = { onStateChange("open") }) {
                    Text("Reopen")
                }
            } else {
                TextButton(onClick = { onStateChange("closed") }) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}
