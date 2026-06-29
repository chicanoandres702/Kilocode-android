package com.kilocode.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.model.Part
import com.kilocode.android.ui.theme.*
import com.kilocode.android.ui.util.pressScale
// import com.jeziellago.compose.markdown.Markdown

// ── Message bubble ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    isUser: Boolean,
    parts: List<Part>,
    agent: String? = null,
    sessionId: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleBg = if (isUser) BubbleUser else BubbleAssistant
    val clipboard = LocalClipboardManager.current
    var showCopyButton by remember { mutableStateOf(false) }

    // Bubble — long-press reveals copy button
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 296.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = if (isUser) 18.dp else 4.dp,
                        topEnd      = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd   = 18.dp,
                    )
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = { showCopyButton = false },
                    onLongClick       = { showCopyButton = !showCopyButton },
                ),
            color           = bubbleBg,
            tonalElevation  = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier            = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                parts.forEach { part ->
                    android.util.Log.d("MessageBubble", "Rendering part: type=${part.type}, text=${part.text}, tool=${part.tool}")
                    when (part.type) {
                        "text"      -> TextPartView(text = part.text.orEmpty())
                        "tool"      -> ToolPartView(part = part, sessionId = part.messageID ?: "", onOptionSelected = onOptionSelected)
                        "reasoning" -> ReasoningPartView(part = part)
                        "step-start", "step-finish", "compaction" -> { /* No-op — compaction is triggered via button, not rendered */ }
                        else        -> {
                            if (!part.text.isNullOrBlank()) {
                                TextPartView(text = part.text)
                            } else {
                                // Render something visible for debugging if the part has no content but is present
                                TextPartView(text = "[Empty Part: type=${part.type}]")
                            }
                        }
                    }
                }
            }

            // Copy button — appears on long-press, floats above bubble
            if (showCopyButton) {
                val allText = parts.mapNotNull { it.text }.joinToString("\n")
                Box(
                    modifier = Modifier.fillMaxSize().wrapContentSize(if (isUser) Alignment.TopStart else Alignment.TopEnd)
                ) {
                    Surface(
                        onClick = {
                            clipboard.setText(AnnotatedString(allText))
                            showCopyButton = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .offset(x = if (isUser) (-6).dp else 6.dp, y = (-10).dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                "Copy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Plain text ─────────────────────────────────────────────────────────────────
@Composable
private fun TextPartView(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.bodyMedium,
        color      = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp,
    )
}

// ── Tool pill — single-line, taps to expand with spring ──────────────────────
@Composable
fun ToolPartView(part: Part, sessionId: String, onOptionSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    if (part.tool == "question") {
        QuestionToolView(part, sessionId, onOptionSelected, modifier)
        return
    }
    val state = part.state ?: return
    var expanded by remember { mutableStateOf(false) }

    data class Spec(val icon: ImageVector, val tint: Color, val bg: Color, val label: String)
    val spec = when (state.status) {
        "pending"   -> Spec(Icons.Rounded.HourglassEmpty, MaterialTheme.colorScheme.primary, ToolRunning, "Pending")
        "running"   -> Spec(Icons.Rounded.PlayArrow,      MaterialTheme.colorScheme.primary, ToolRunning, "Running")
        "completed" -> Spec(Icons.Rounded.CheckCircle,    SemanticSuccess,                   ToolSuccess, "Done")
        "error"     -> Spec(Icons.Rounded.ErrorOutline,   SemanticError,                     ToolError,   "Failed")
        else        -> Spec(Icons.AutoMirrored.Rounded.HelpOutline,    MaterialTheme.colorScheme.onSurfaceVariant, ToolRunning, "?")
    }

    // Spin animation for "running" tools
    val spin = rememberInfiniteTransition(label = "spin")
    val spinAngle by spin.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label         = "angle",
    )

    val hasDetail = (state.status == "completed" && !state.output.isNullOrBlank()) ||
                   (state.status == "error"     && !state.error.isNullOrBlank())

    // Chevron rotation with spring
    val chevronRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label         = "chevron",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .pressScale(pressedScale = 0.97f, onTap = if (hasDetail) ({ expanded = !expanded }) else null),
        shape = RoundedCornerShape(10.dp),
        color = spec.bg,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = spec.icon,
                    contentDescription = spec.label,
                    tint               = spec.tint,
                    modifier           = Modifier
                        .size(13.dp)
                        .then(
                            if (state.status == "running")
                                Modifier.graphicsLayer { rotationZ = spinAngle }
                            else Modifier
                        ),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text       = state.title ?: part.tool ?: "Tool",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    modifier   = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text  = spec.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.tint.copy(alpha = 0.85f),
                )
                if (hasDetail) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector        = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier           = Modifier
                            .size(14.dp)
                            .graphicsLayer { rotationZ = chevronRotation },
                    )
                }
            }

            if (expanded && hasDetail) {
                val detail = when {
                    state.status == "completed" && !state.output.isNullOrBlank() -> state.output!!.trim().take(500)
                    state.status == "error"     && !state.error.isNullOrBlank()  -> state.error!!
                    else -> null
                }
                if (detail != null) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text       = detail,
                            style      = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color      = if (state.status == "error") SemanticError.copy(alpha = 0.85f)
                                         else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionToolView(
    part: Part, 
    sessionId: String, 
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier, 
) {
    val input = part.state?.input as? Map<String, Any> ?: return
    val questions = input["questions"] as? List<Map<String, Any>> ?: return
    var expanded by remember { mutableStateOf(true) }
    
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (expanded) {
            questions.forEach { questionMap ->
                val header = questionMap["header"] as? String ?: "Question"
                val question = questionMap["question"] as? String ?: ""
                val options = questionMap["options"] as? List<Map<String, String>> ?: emptyList()
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(header, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { expanded = false }) {
                                Icon(Icons.Rounded.Compress, contentDescription = "Compact")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(question, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        options.forEach { option ->
                            val label = option["label"] ?: ""
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOptionSelected(label) },
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(label, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().clickable { expanded = true }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Expand, contentDescription = "Expand")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Question", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

// ── Reasoning — collapsed, spring-opens ──────────────────────────────────────
@Composable
fun ReasoningPartView(part: Part, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val hasText  = !part.text.isNullOrBlank()
    val chevron  by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label         = "chevron",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .pressScale(pressedScale = 0.97f, onTap = if (hasText) ({ expanded = !expanded }) else null),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    modifier           = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text     = "Thinking…",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f),
                )
                if (hasText) {
                    Icon(
                        imageVector        = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier           = Modifier
                            .size(13.dp)
                            .graphicsLayer { rotationZ = chevron },
                    )
                }
            }

            if (expanded && hasText) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text       = part.text!!,
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }
}
