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
import com.kilocode.android.data.repository.PlanningRepository
import com.kilocode.android.ui.components.ErrorCard
import com.kilocode.android.ui.components.LoadingIndicator
import kotlinx.coroutines.launch

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

    var projectDescription by remember { mutableStateOf("") }
    var generatedFeatures by remember { mutableStateOf(listOf<GeneratedFeature>()) }
    var isGeneratingFeatures by remember { mutableStateOf(false) }
    var featureError by remember { mutableStateOf<String?>(null) }
    var selectedFeatures by remember { mutableStateOf(mapOf<Int, SelectedFeature>()) }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

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

    fun updateFeatureDescription(id: Int, description: String) {
        selectedFeatures = selectedFeatures.toMutableMap().apply {
            this[id] = (this[id] ?: SelectedFeature(id)).copy(description = description)
        }
    }

    fun updateFeatureTasks(id: Int, tasks: List<String>) {
        selectedFeatures = selectedFeatures.toMutableMap().apply {
            this[id] = (this[id] ?: SelectedFeature(id)).copy(tasks = tasks)
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
                    repository.createIssue(
                        title = feature.title,
                        body = buildString {
                            append(feature.description)
                            if (feature.tasks.isNotEmpty()) {
                                append("\n\n## Tasks\n")
                                feature.tasks.forEach { task -> append("- [ ] $task\n") }
                            }
                        },
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
                    onNext = { step = 3 },
                    onBack = { step = 1 },
                )
                3 -> Step3FeatureDetails(
                    selectedFeatures = selectedFeatures,
                    onUpdateDescription = { id, desc -> updateFeatureDescription(id, desc) },
                    onUpdateTasks = { id, tasks -> updateFeatureTasks(id, tasks) },
                    onBack = { step = 2 },
                    onNext = { step = 4 },
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

@Composable
private fun Step2FeatureSelection(
    features: List<GeneratedFeature>,
    selectedFeatures: Map<Int, SelectedFeature>,
    onToggle: (Int, Boolean) -> Unit,
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
        Text("Choose which features to include", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(12.dp))
        
        features.forEach { feature ->
            val selected = selectedFeatures[feature.id]?.selected ?: true
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selected, onCheckedChange = { onToggle(feature.id, it) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feature.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
private fun Step3FeatureDetails(
    selectedFeatures: Map<Int, SelectedFeature>,
    onUpdateDescription: (Int, String) -> Unit,
    onUpdateTasks: (Int, List<String>) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
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
                        value = feature.tasks.joinToString("\n"),
                        onValueChange = { onUpdateTasks(feature.id, it.split("\n").map { it.trim() }.filter { it.isNotEmpty() }) },
                        label = { Text("Tasks (one per line)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
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