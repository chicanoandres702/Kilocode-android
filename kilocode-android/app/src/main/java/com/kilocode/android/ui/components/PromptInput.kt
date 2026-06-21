package com.kilocode.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.model.Agent
import com.kilocode.android.data.model.Message
import com.kilocode.android.data.model.ModelOption

private const val MAX_CHARS = 4000

@Composable
fun PromptInput(
    onSend: (String) -> Unit,
    onContinue: () -> Unit = {},
    isLoading: Boolean,
    agents: List<Agent> = emptyList(),
    selectedAgent: Agent? = null,
    onAgentSelected: (Agent?) -> Unit = {},
    models: List<ModelOption> = emptyList(),
    selectedModel: ModelOption? = null,
    onModelSelected: (ModelOption?) -> Unit = {},
    autonomousMode: Boolean = false,
    onAutonomousModeChanged: (Boolean) -> Unit = {},
    messages: List<Message> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    var agentMenuExpanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank() && !isLoading
    val showAgentChip = agents.isNotEmpty()
    val showModelChip = models.isNotEmpty()
    val charCount = text.length
    val nearLimit = charCount > MAX_CHARS * 0.85f
    val modelGroups = models.groupBy { it.category.ifBlank { "Models" } }

    val sendScale by animateFloatAsState(
        targetValue = if (canSend || autonomousMode) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "sendScale",
    )

    val fieldElevation by animateDpAsState(
        targetValue = if (text.isNotEmpty() || autonomousMode) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "fieldElevation",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = fieldElevation,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = if (showAgentChip || showModelChip) 6.dp else 14.dp,
                                end = 10.dp,
                                top = 9.dp,
                                bottom = 9.dp,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showAgentChip) {
                                Box {
                                    AgentChip(
                                        agent = selectedAgent,
                                        expanded = agentMenuExpanded,
                                        onExpand = { agentMenuExpanded = true },
                                        onDismiss = { agentMenuExpanded = false },
                                        agents = agents,
                                        onAgentSelected = {
                                            onAgentSelected(it)
                                            agentMenuExpanded = false
                                        },
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            if (showModelChip) {
                                Box {
                                    ModelChip(
                                        model = selectedModel,
                                        expanded = modelMenuExpanded,
                                        onExpand = { modelMenuExpanded = true },
                                        onDismiss = { modelMenuExpanded = false },
                                        modelGroups = modelGroups,
                                        onModelSelected = {
                                            onModelSelected(it)
                                            modelMenuExpanded = false
                                        },
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = text,
                                    onValueChange = { text = it.take(MAX_CHARS) },
                                    enabled = !isLoading,
                                    maxLines = 6,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (canSend) {
                                            onSend(text.trim())
                                            text = ""
                                        }
                                    }),
                                    decorationBox = { inner ->
                                        if (text.isEmpty()) {
                                            Text(
                                                text = if (autonomousMode) "Autonomous mode is sending continue…" else "Ask Kilo anything…",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            )
                                        }
                                        inner()
                                    },
                                )
                            }

                            if (nearLimit) {
                                Text(
                                    text = "${MAX_CHARS - charCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (charCount >= MAX_CHARS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 6.dp),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }

                    FilterChip(
                        selected = autonomousMode,
                        onClick = { onAutonomousModeChanged(!autonomousMode) },
                        label = { Text("Auto", fontSize = 11.sp) },
                        leadingIcon = if (autonomousMode) {
                            {
                                Icon(
                                    Icons.Rounded.AutoMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        enabled = messages.isNotEmpty(),
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(tween(150), 0.7f)) togetherWith
                                (fadeOut(tween(100)) + scaleOut(tween(100)))
                        },
                        label = "sendBtn",
                    ) { loading ->
                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(sendScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend || autonomousMode) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                IconButton(
                                    onClick = {
                                        if (autonomousMode) {
                                            onContinue()
                                        } else if (canSend) {
                                            onSend(text.trim())
                                            text = ""
                                        }
                                    },
                                    enabled = canSend || autonomousMode,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Icon(
                                        imageVector = if (autonomousMode) Icons.Rounded.Pause else Icons.Rounded.ArrowUpward,
                                        contentDescription = if (autonomousMode) "Pause autonomous" else "Send",
                                        tint = if (canSend || autonomousMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentChip(
    agent: Agent?,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    agents: List<Agent>,
    onAgentSelected: (Agent?) -> Unit,
) {
    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        modifier = Modifier.height(26.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(Icons.Rounded.SmartToy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
            Text(
                text = agent?.name?.take(16) ?: "Agent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 10.sp,
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Default agent") },
            onClick = { onAgentSelected(null) },
            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(15.dp)) },
        )
        agents.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.name) },
                onClick = { onAgentSelected(item) },
                leadingIcon = { Icon(Icons.Rounded.SmartToy, null, Modifier.size(15.dp)) },
            )
        }
    }
}

@Composable
private fun ModelChip(
    model: ModelOption?,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    modelGroups: Map<String, List<ModelOption>>,
    onModelSelected: (ModelOption?) -> Unit,
) {
    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
        modifier = Modifier.height(26.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(Icons.Rounded.ModelTraining, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(11.dp))
            Text(
                text = model?.displayName?.take(18) ?: "Model",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 10.sp,
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Default model") },
            onClick = { onModelSelected(null) },
            leadingIcon = { Icon(Icons.Rounded.AutoFixHigh, null, Modifier.size(15.dp)) },
        )
        modelGroups.forEach { (category, models) ->
            DropdownMenuItem(
                text = { Text(category) },
                onClick = {},
                enabled = false,
                leadingIcon = { Icon(Icons.Rounded.Category, null, Modifier.size(15.dp)) },
            )
            models.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.displayName) },
                    onClick = { onModelSelected(item) },
                    leadingIcon = { Icon(Icons.Rounded.ModelTraining, null, Modifier.size(15.dp)) },
                )
            }
            HorizontalDivider()
        }
    }
}
