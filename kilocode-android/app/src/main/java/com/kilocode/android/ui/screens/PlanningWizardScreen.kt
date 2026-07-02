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
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.GeneratedFeature
import com.kilocode.android.data.model.SelectedFeature
import com.kilocode.android.data.model.Task
import com.kilocode.android.data.repository.PlanningRepository
import com.kilocode.android.data.repository.SessionRepository
import com.kilocode.android.data.model.Message
import com.kilocode.android.data.model.Part
import com.kilocode.android.ui.components.ErrorCard
import com.kilocode.android.ui.components.LoadingIndicator
import androidx.compose.ui.platform.LocalContext
import com.kilocode.android.ui.util.BranchManager
import com.kilocode.android.worker.BranchWorker
import kotlinx.coroutines.*

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
    val sessionRepository = remember(apiClient) { SessionRepository(apiClient) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    var projectDescription by remember { mutableStateOf("") }
    var generatedFeatures by remember { mutableStateOf(listOf<GeneratedFeature>()) }
    var isGeneratingFeatures by remember { mutableStateOf(false) }
    var featureError by remember { mutableStateOf<String?>(null) }
    var selectedFeatures by remember { mutableStateOf(mapOf<Int, SelectedFeature>()) }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var aiQuestion by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isAskingAI by remember { mutableStateOf(false) }
    var agents by remember { mutableStateOf(listOf<com.kilocode.android.data.model.Agent>()) }
    var selectedAgent by remember { mutableStateOf<com.kilocode.android.data.model.Agent?>(null) }

    LaunchedEffect(apiClient) {
        agents = sessionRepository.listAgents()
        selectedAgent = agents.find { it.name == "aidde-Main" } ?: agents.firstOrNull()
    }

    fun generateFeatures() {
        isGeneratingFeatures = true
        featureError = null
        scope.launch {
            try {
                val features = repository.generateFeatures(projectDescription)
                generatedFeatures = features
                selectedFeatures = features.associate { it.id to SelectedFeature(it.id, it.title, it.description, it.tasks) }
                step = 3
            } catch (e: Exception) {
                featureError = e.message ?: "Failed to generate features"
            } finally {
                isGeneratingFeatures = false
            }
        }
    }

    fun toggleFeature(id: Int, selected: Boolean) {
        selectedFeatures = selectedFeatures.toMutableMap().apply {
            this[id] = (this[id] ?: generatedFeatures.find { it.id == id }?.let { SelectedFeature(it.id, it.title, it.description, it.tasks) }
                ?: SelectedFeature(id))
                .copy(selected = selected)
        }
    }

    fun deleteFeature(id: Int) {
        selectedFeatures = selectedFeatures.toMutableMap().apply {
            remove(id)
        }
        generatedFeatures = generatedFeatures.filter { it.id != id }
    }

    fun updateFeatureDescription(id: Int, description: String) {
        selectedFeatures = selectedFeatures.toMutableMap().apply {
            this[id] = (this[id] ?: SelectedFeature(id)).copy(description = description)
        }
    }

    fun updateFeatureTasks(id: Int, tasks: List<Task>) {
        selectedFeatures = selectedFeatures.toMutableMap().apply {
            this[id] = (this[id] ?: SelectedFeature(id)).copy(tasks = tasks)
        }
    }

    fun askAI(question: String, agent: com.kilocode.android.data.model.Agent?) {
        if (question.isBlank()) return
        isAskingAI = true
        aiResponse = null
        scope.launch {
            try {
                // We need an active session. If none, create one.
                val session = sessionRepository.currentSession.value 
                    ?: sessionRepository.createSession(directory = "/")
                
                if (session?.id != null) {
                    sessionRepository.sendPrompt(
                        sessionId = session.id,
                        prompt = question,
                        agent = agent?.name
                    )
                    // SSE event handling is asynchronous in SessionRepository. 
                    // This is just a prototype.
                    aiResponse = "Prompt sent to ${agent?.name ?: "default agent"}"
                } else {
                    aiResponse = "Failed to create/get session"
                }
            } catch (e: Exception) {
                aiResponse = "Failed to ask AI: ${e.message}"
            } finally {
                isAskingAI = false
            }
        }
    }

    fun createAll() {
        val featuresToCreate = selectedFeatures.values.filter { it.selected }
        if (featuresToCreate.isEmpty()) {
            createError = "Please select at least one feature"
            return
        }
        
        isCreating = true
        createError = null
        scope.launch {
            try {
                val milestone = repository.createMilestone(
                    title = projectDescription.take(50),
                    description = projectDescription,
                )
                if (milestone == null) {
                    createError = "Failed to create milestone"
                    isCreating = false
                    return@launch
                }
                featuresToCreate.forEach { feature ->
                    val issue = repository.createIssue(
                        title = feature.title,
                        body = buildString {
                            append(feature.description)
                            if (feature.tasks.isNotEmpty()) {
                                append("\n\n## Tasks\n")
                                feature.tasks.forEach { task -> append("- [ ] ${task.title}\n") }
                            }
                        },
                        milestoneNumber = milestone.number,
                    )
                    
                    if (issue != null) {
                        BranchWorker.enqueue(context, issue.number, feature.title)
                    }
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
                        Text("Planning Wizard", fontWeight = FontWeight.Bold)
                        Text("Step $step of $totalSteps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else onBack() }) {
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
            LinearProgressIndicator(
                progress = { step / totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                1 -> Step1ProjectDescription(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    onNext = { generateFeatures() },
                    enabled = projectDescription.isNotBlank() && !isGeneratingFeatures,
                )
                2 -> Step2FeatureSelection(
                    features = generatedFeatures,
                    selectedFeatures = selectedFeatures,
                    onToggle = { id, selected -> toggleFeature(id, selected) },
                    onDelete = { deleteFeature(it) },
                    onNext = { step = 3 },
                    onBack = { step = 1 },
                )
                3 -> Step3FeatureDetails(
                    selectedFeatures = selectedFeatures,
                    onUpdateDescription = { id, desc -> updateFeatureDescription(id, desc) },
                    onUpdateTasks = { id, tasks -> updateFeatureTasks(id, tasks) },
                    onBack = { step = 2 },
                    onNext = { step = 4 },
                    aiQuestion = aiQuestion,
                    onAiQuestionChange = { aiQuestion = it },
                    onAskAI = { askAI(aiQuestion, selectedAgent) },
                    aiResponse = aiResponse,
                    isAskingAI = isAskingAI,
                    agents = agents,
                    selectedAgent = selectedAgent,
                    onAgentSelected = { selectedAgent = it }
                )


                4 -> Step4Review(
                    projectDescription = projectDescription,
                    selectedFeatures = selectedFeatures,
                    onConfirm = { createAll() },
                    onBack = { step = 3 },
                )
            }

            featureError?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                ErrorCard(message = err, onRetry = { featureError = null }, modifier = Modifier.padding(horizontal = 16.dp))
            }

            createError?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                ErrorCard(message = err, onRetry = { createError = null }, modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (isGeneratingFeatures || isCreating) {
                Spacer(modifier = Modifier.height(16.dp))
                LoadingIndicator(message = if (isGeneratingFeatures) "Generating features..." else "Creating plan…")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Step1ProjectDescription(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    enabled: Boolean,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("What are you building?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Describe your project - AI will generate features", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(12.dp))
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
        Button(onClick = onNext, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Generate Features")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2FeatureSelection(
    features: List<GeneratedFeature>,
    selectedFeatures: Map<Int, SelectedFeature>,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Select Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Toggle to include, swipe to delete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(12.dp))
        
        features.forEach { feature ->
            val selected = selectedFeatures[feature.id]?.selected ?: true
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(feature.id)
                        true
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                    Surface(color = color, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                },
                content = {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selected, onCheckedChange = { onToggle(feature.id, it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feature.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onNext, modifier = Modifier.weight(1f)) { Text("Next") }
        }
    }
}

@Composable
private fun Step3FeatureDetails(
    selectedFeatures: Map<Int, SelectedFeature>,
    onUpdateDescription: (Int, String) -> Unit,
    onUpdateTasks: (Int, List<Task>) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    aiQuestion: String,
    onAiQuestionChange: (String) -> Unit,
    onAskAI: () -> Unit,
    aiResponse: String?,
    isAskingAI: Boolean,
    agents: List<com.kilocode.android.data.model.Agent>,
    selectedAgent: com.kilocode.android.data.model.Agent?,
    onAgentSelected: (com.kilocode.android.data.model.Agent) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Edit Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Refine descriptions and task lists", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(12.dp))

        // Agent Selector
        if (agents.isNotEmpty()) {
            Text("Select Agent:", style = MaterialTheme.typography.labelSmall)
            // Simplified selector: just show agent names in a Row for now or use a Dropdown
            // For simplicity, let's just use a row of chips
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                agents.forEach { agent ->
                    FilterChip(
                        selected = selectedAgent == agent,
                        onClick = { onAgentSelected(agent) },
                        label = { Text(agent.name) }
                    )
                }
            }
        }

        selectedFeatures.values.filter { it.selected }.forEach { feature ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(feature.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feature.description,
                        onValueChange = { onUpdateDescription(feature.id, it) },
                        label = { Text("Description") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feature.tasks.joinToString("\n") { it.title },
                        onValueChange = { 
                            onUpdateTasks(feature.id, it.split("\n").map { title -> Task(title = title.trim()) }.filter { it.title.isNotEmpty() }) 
                        },
                        label = { Text("Tasks (one per line)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // AI Prompting
                    OutlinedTextField(
                        value = aiQuestion,
                        onValueChange = onAiQuestionChange,
                        label = { Text("Ask ${selectedAgent?.name ?: "AI"} about this feature") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = onAskAI, enabled = aiQuestion.isNotBlank() && !isAskingAI) {
                                Icon(Icons.Rounded.Send, contentDescription = "Ask AI")
                            }
                        }
                    )
                    
                    if (isAskingAI) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(8.dp))
                    }
                    
                    aiResponse?.let { response ->
                        Text("AI: $response", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onNext, modifier = Modifier.weight(1f)) { Text("Next") }
        }
    }
}

@Composable
private fun Step4Review(
    projectDescription: String,
    selectedFeatures: Map<Int, SelectedFeature>,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val selected = selectedFeatures.values.filter { it.selected }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Review & Create", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Confirm before sending to GitHub", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(12.dp))

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(projectDescription.take(50), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (projectDescription.length > 50) {
                    Text("... (${projectDescription.length} chars)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Features (${selected.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        selected.forEach { feature ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(feature.title, style = MaterialTheme.typography.bodyMedium)
                if (feature.tasks.isNotEmpty()) {
                    Text("${feature.tasks.size} tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onConfirm, modifier = Modifier.weight(1f)) { Text("Create Plan") }
        }
    }
}