package com.kilocode.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kilocode.android.data.model.Agent

@Composable
fun PromptInput(
    onSend: (String) -> Unit,
    isLoading: Boolean,
    agents: List<Agent> = emptyList(),
    selectedAgent: String? = null,
    onAgentSelected: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    var agentMenuExpanded by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank() && !isLoading

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    OutlinedButton(
                        onClick = { agentMenuExpanded = true },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedAgent ?: "Agent",
                            maxLines = 1,
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = agentMenuExpanded,
                        onDismissRequest = { agentMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Default Agent") },
                            onClick = {
                                onAgentSelected(null)
                                agentMenuExpanded = false
                            },
                        )
                        agents.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent.name) },
                                onClick = {
                                    onAgentSelected(agent.name)
                                    agentMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Kilo anything...") },
                    enabled = canSend,
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onSend(text.trim())
                                text = ""
                            }
                        },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(48.dp),
                    ) {
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    onSend(text.trim())
                                    text = ""
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
