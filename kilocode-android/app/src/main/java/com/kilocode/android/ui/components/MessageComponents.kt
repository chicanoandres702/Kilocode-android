package com.kilocode.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.model.Part
import com.kilocode.android.ui.theme.*

// ── Message bubble ────────────────────────────────────────────────────────────
@Composable
fun MessageBubble(
    isUser: Boolean,
    parts: List<Part>,
    agent: String? = null,
    modifier: Modifier = Modifier,
) {
    val bubbleBg   = if (isUser) BubbleUser else BubbleAssistant
    val alignment  = if (isUser) Alignment.End else Alignment.Start
    val displayName = if (isUser) "You" else agent ?: "Kilo"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = alignment,
    ) {
        // Sender row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
        ) {
            if (!isUser) {
                // Avatar for assistant
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Brand.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Brand,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Bubble
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart    = if (isUser) 20.dp else 4.dp,
                topEnd      = if (isUser) 4.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd   = 20.dp,
            ),
            color = bubbleBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                parts.forEach { part ->
                    when (part.type) {
                        "text"      -> TextPartView(text = part.text.orEmpty())
                        "tool"      -> ToolPartView(part = part)
                        "reasoning" -> ReasoningPartView(part = part)
                        else        -> TextPartView(text = part.text.orEmpty())
                    }
                }
            }
        }
    }
}

// ── Plain text part ───────────────────────────────────────────────────────────
@Composable
private fun TextPartView(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 24.sp,
    )
}

// ── Tool part ─────────────────────────────────────────────────────────────────
@Composable
fun ToolPartView(part: Part, modifier: Modifier = Modifier) {
    val state = part.state ?: return

    data class StatusSpec(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val tint: Color,
        val bg: Color,
        val label: String,
    )

    val spec = when (state.status) {
        "pending"   -> StatusSpec(Icons.Rounded.HourglassEmpty, MaterialTheme.colorScheme.primary,      ToolRunning, "Pending")
        "running"   -> StatusSpec(Icons.Rounded.PlayArrow,      MaterialTheme.colorScheme.primary,      ToolRunning, "Running")
        "completed" -> StatusSpec(Icons.Rounded.CheckCircle,    SemanticSuccess,                        ToolSuccess, "Done")
        "error"     -> StatusSpec(Icons.Rounded.ErrorOutline,   SemanticError,                          ToolError,   "Failed")
        else        -> StatusSpec(Icons.Rounded.HelpOutline,    MaterialTheme.colorScheme.onSurfaceVariant, ToolRunning, "?")
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = spec.bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector     = spec.icon,
                contentDescription = spec.label,
                tint            = spec.tint,
                modifier        = Modifier
                    .size(16.dp)
                    .padding(top = 1.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = state.title ?: part.tool ?: "Tool call",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = spec.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = spec.tint.copy(alpha = 0.85f),
                    )
                }
                val detail = when {
                    state.status == "completed" && state.output != null ->
                        state.output.trim().take(240)
                    state.status == "error" && state.error != null ->
                        state.error
                    else -> null
                }
                if (detail != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text       = detail,
                        style      = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color      = if (state.status == "error") SemanticError.copy(alpha = 0.9f)
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines   = 4,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

// ── Reasoning part ────────────────────────────────────────────────────────────
@Composable
fun ReasoningPartView(part: Part, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val hasText  = !part.text.isNullOrBlank()
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (hasText) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { expanded = !expanded }
                )
                           else Modifier,
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Psychology,
                    contentDescription = "Thinking",
                    tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier           = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text   = "Thinking…",
                    style  = MaterialTheme.typography.labelSmall,
                    color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                if (hasText) {
                    Icon(
                        imageVector        = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier           = Modifier.size(14.dp),
                    )
                }
            }
            if (expanded && hasText) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text       = part.text!!,
                    style      = MaterialTheme.typography.bodySmall,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

