package com.kilocode.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.repository.PlanningRepository
import com.kilocode.android.ui.components.ErrorCard
import com.kilocode.android.ui.components.LoadingIndicator
import kotlinx.coroutines.launch

/**
 * Planning wizard — guided questions to create milestones and issues.
 * Step 1: What are you building? (project description)
 * Step 2: Milestone title + description
 * Step 3: Issues for this milestone (add multiple)
 * Step 4: Review and create
 * Follows AIDDE protocol: issue #44.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningWizardScreen(
    serverUrl: String,
    apiServerUrl: String,
    sharedSecret: String?,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val apiClient = remember(apiServerUrl, sharedSecret) {
        ApiClient.getInstance(apiServerUrl, sharedSecret ?: "")
    }
    val repository = remember(apiClient) { PlanningRepository(apiClient) }
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    // Step 1: Project description
    var projectDescription by remember { mutableStateOf("") }

    // Step 2: Milestone
    var milestoneTitle by remember { mutableStateOf("") }
    var milestoneDescription by remember { mutableStateOf("") }

    // Step 3: Issues
    var issues by remember { mutableStateOf(listOf<IssueDraft>()) }
    var newIssueTitle by remember { mutableStateOf("") }
    var newIssueBody by remember { mutableStateOf("") }

    // Step 4: Review
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    fun createAll() {
        isCreating = true
        createError = null
        scope.launch {
            try {
                val milestone = repository.createMilestone(
                    title = milestoneTitle,
                    description = milestoneDescription.ifBlank { null },
                )
                if (milestone == null) {
                    createError = "Failed to create milestone"
                    isCreating = false
                    return@launch
                }
                issues.forEach { draft ->
                    repository.createIssue(
                        title = draft.title,
                        body = draft.body.ifBlank { null },
                        milestoneNumber = milestone.number,
                    )
                }
                isCreating = false
                onComplete()
            } catch (e: Exception) {
                createError = e.message ?: "Failed to create plan"
                isCreating = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Planning Wizard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Step $step of $totalSteps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) step-- else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { step / totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                1 -> Step1ProjectDescription(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    onNext = { step = 2 },
                )
                2 -> Step2Milestone(
                    title = milestoneTitle,
                    onTitleChange = { milestoneTitle = it },
                    description = milestoneDescription,
                    onDescriptionChange = { milestoneDescription = it },
                    onNext = { step = 3 },
                )
                3 -> Step3Issues(
                    issues = issues,
                    onAddIssue = { title, body ->
                        if (title.isNotBlank()) {
                            issues = issues + IssueDraft(title, body)
                            newIssueTitle = ""
                            newIssueBody = ""
                        }
                    },
                    onRemoveIssue = { index ->
                        issues = issues.toMutableList().apply { removeAt(index) }
                    },
                    onNext = { step = 4 },
                )
                4 -> Step4Review(
                    milestoneTitle = milestoneTitle,
                    milestoneDescription = milestoneDescription,
                    issues = issues,
                    onConfirm = { createAll() },
                )
            }

            // Error display
            createError?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                ErrorCard(
                    message = err,
                    onRetry = {
                        createError = null
                        createAll()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // Creating overlay
            if (isCreating) {
                Spacer(modifier = Modifier.height(16.dp))
                LoadingIndicator(message = "Creating plan…")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private data class IssueDraft(
    val title: String,
    val body: String = "",
)

@Composable
private fun WizardStepCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        content()
    }
}

@Composable
private fun Step1ProjectDescription(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    WizardStepCard(
        title = "What are you building?",
        subtitle = "Describe your project in a few sentences",
        icon = Icons.Rounded.Lightbulb,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Project description") },
            placeholder = { Text("e.g., A task management app with AI-powered prioritization...") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next: Create milestone")
        }
    }
}

@Composable
private fun Step2Milestone(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    WizardStepCard(
        title = "Define a milestone",
        subtitle = "Group related tasks into a deliverable milestone",
        icon = Icons.Rounded.Flag,
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Milestone title *") },
            placeholder = { Text("e.g., Project scaffolding & auth") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (optional)") },
            placeholder = { Text("What does this milestone accomplish?") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onNext,
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next: Add issues")
        }
    }
}

@Composable
private fun Step3Issues(
    issues: List<IssueDraft>,
    onAddIssue: (String, String) -> Unit,
    onRemoveIssue: (Int) -> Unit,
    onNext: () -> Unit,
) {
    var issueTitle by remember { mutableStateOf("") }
    var issueBody by remember { mutableStateOf("") }

    WizardStepCard(
        title = "Add tasks / issues",
        subtitle = "Break the milestone into actionable items",
        icon = Icons.Rounded.TaskAlt,
    ) {
        // Existing issues list
        issues.forEachIndexed { index, issue ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = issue.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemoveIssue(index) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Add new issue input
        OutlinedTextField(
            value = issueTitle,
            onValueChange = { issueTitle = it },
            label = { Text("Issue title") },
            placeholder = { Text("e.g., Set up Retrofit with OkHttp") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = issueBody,
            onValueChange = { issueBody = it },
            label = { Text("Details (optional)") },
            placeholder = { Text("Implementation notes...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (issueTitle.isNotBlank()) {
                    onAddIssue(issueTitle, issueBody)
                    issueTitle = ""
                    issueBody = ""
                }
            },
            enabled = issueTitle.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add issue")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next: Review")
        }
    }
}

@Composable
private fun Step4Review(
    milestoneTitle: String,
    milestoneDescription: String,
    issues: List<IssueDraft>,
    onConfirm: () -> Unit,
) {
    WizardStepCard(
        title = "Review & create",
        subtitle = "Confirm before sending to GitHub",
        icon = Icons.Rounded.Verified,
    ) {
        // Milestone preview
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Milestone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = milestoneTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (milestoneDescription.isNotBlank()) {
                    Text(
                        text = milestoneDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Issues (${issues.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        issues.forEach { issue ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.LabelImportant,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create plan on GitHub")
        }
    }
}
