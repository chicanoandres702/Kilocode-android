package com.kilocode.android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.model.Agent

// ── Prompt input ──────────────────────────────────────────────────────────────
// Single-row design: [Agent chip | text field | send button]
// Agent chip is inline — no separate row above the field.
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
    val showAgentChip = agents.isNotEmpty()

    Surface(
        modifier       = modifier.fillMaxWidth(),
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // ── Text field surface (contains agent chip + input) ───────────
                Surface(
                    shape    = RoundedCornerShape(26.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier          = Modifier.padding(start = if (showAgentChip) 6.dp else 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Inline agent chip — only if agents are available
                        if (showAgentChip) {
                            Box {
                                Surface(
                                    onClick  = { agentMenuExpanded = true },
                                    shape    = RoundedCornerShape(18.dp),
                                    color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.height(30.dp),
                                ) {
                                    Row(
                                        modifier          = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint               = MaterialTheme.colorScheme.primary,
                                            modifier           = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text       = selectedAgent?.take(10) ?: "Agent",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines   = 1,
                                        )
                                        Icon(
                                            imageVector        = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Change agent",
                                            tint               = MaterialTheme.colorScheme.primary,
                                            modifier           = Modifier.size(12.dp),
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded         = agentMenuExpanded,
                                    onDismissRequest = { agentMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text        = { Text("Default agent") },
                                        onClick     = { onAgentSelected(null); agentMenuExpanded = false },
                                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp)) },
                                    )
                                    agents.forEach { agent ->
                                        DropdownMenuItem(
                                            text        = { Text(agent.name) },
                                            onClick     = { onAgentSelected(agent.name); agentMenuExpanded = false },
                                            leadingIcon = { Icon(Icons.Rounded.SmartToy, null, Modifier.size(16.dp)) },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Text input
                        BasicTextField(
                            value           = text,
                            onValueChange   = { text = it },
                            enabled         = !isLoading,
                            maxLines        = 5,
                            modifier        = Modifier.weight(1f),
                            textStyle       = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (canSend) { onSend(text.trim()); text = "" }
                            }),
                            decorationBox   = { inner ->
                                Box {
                                    if (text.isEmpty()) {
                                        Text(
                                            text  = "Ask Kilo anything…",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── Send / loading button ─────────────────────────────────────
                IconButton(
                    onClick  = { if (canSend) { onSend(text.trim()); text = "" } },
                    enabled  = canSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) MaterialTheme.colorScheme.primary
                            else         MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        ),
                ) {
                    AnimatedContent(
                        targetState  = isLoading,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label        = "send",
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Rounded.ArrowUpward,
                                contentDescription = "Send",
                                tint               = if (canSend) MaterialTheme.colorScheme.onPrimary
                                                     else         MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier           = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
