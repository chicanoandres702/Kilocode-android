package com.kilocode.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.model.Part
import com.kilocode.android.ui.theme.*

@Composable
fun MessageBubble(
    isUser: Boolean,
    parts: List<Part>,
    agent: String? = null,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isUser) UserMessageBg else AssistantMessageBg
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val displayName = if (isUser) {
        "You"
    } else {
        agent ?: "Kilo"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = alignment,
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 6.dp,
                        topEnd = if (isUser) 6.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp,
                    )
                ),
            color = bgColor,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                parts.forEach { part ->
                    when (part.type) {
                        "text" -> {
                            Text(
                                text = part.text.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp,
                            )
                        }
                        "tool" -> {
                            ToolPartView(part = part)
                        }
                        "reasoning" -> {
                            ReasoningPartView(part = part)
                        }
                        else -> {
                            Text(
                                text = part.text.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolPartView(part: Part, modifier: Modifier = Modifier) {
    val state = part.state ?: return
    val (icon, bgColor, statusText) = when (state.status) {
        "pending" -> Triple(Icons.Default.HourglassEmpty, ToolRunningBg, "Pending")
        "running" -> Triple(Icons.Default.PlayArrow, ToolRunningBg, "Running")
        "completed" -> Triple(Icons.Default.Check, ToolSuccessBg, "Completed")
        "error" -> Triple(Icons.Default.Error, ToolErrorBg, "Error")
        else -> Triple(Icons.Default.Help, ToolRunningBg, "Unknown")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = statusText,
                tint = when (state.status) {
                    "completed" -> SuccessGreen
                    "error" -> Error
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title ?: part.tool ?: "Tool",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.status == "completed" && state.output != null) {
                    Text(
                        text = state.output.take(200),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                    )
                }
                if (state.status == "error" && state.error != null) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
fun ReasoningPartView(part: Part, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Reasoning",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!part.text.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = part.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                )
            }
        }
    }
}
