package com.kilocode.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RefreshRounded
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Loading ──────────────────────────────────────────────────────────────────
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Loading…",
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Error card ───────────────────────────────────────────────────────────────
@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connection error",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.RefreshRounded,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry", fontSize = 12.sp)
            }
        }
    }
}

// ── Status chip ──────────────────────────────────────────────────────────────
@Composable
fun StatusChip(
    text: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = if (isOnline) MaterialTheme.colorScheme.secondary
                   else         MaterialTheme.colorScheme.error
    val textColor = if (isOnline) MaterialTheme.colorScheme.secondary
                    else          MaterialTheme.colorScheme.error
    val bgColor   = if (isOnline) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    else          MaterialTheme.colorScheme.error.copy(alpha = 0.12f)

    // Pulse animation for the live dot
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotScale by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot",
    )

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(if (isOnline) dotScale else 1f)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Typing indicator ─────────────────────────────────────────────────────────
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue  = -5f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0f    at 0
                        -5f   at 200
                        0f    at 400
                        0f    at 1000
                    },
                    initialStartOffset = StartOffset(index * 120),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    ),
            )
        }
    }
}
