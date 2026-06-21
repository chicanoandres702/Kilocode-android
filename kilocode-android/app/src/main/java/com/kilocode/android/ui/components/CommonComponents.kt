package com.kilocode.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Shimmer skeleton — replaces the spinner on first load ────────────────────
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue   = -300f,
        targetValue    = 1000f,
        animationSpec  = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label          = "shimmerX",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant,
        ),
        start = Offset(shimmerX, 0f),
        end   = Offset(shimmerX + 300f, 100f),
    )
    Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
fun SessionListSkeleton() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        repeat(7) { i ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBlock(modifier = Modifier.size(16.dp), shape = CircleShape)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBlock(
                        modifier = Modifier
                            .fillMaxWidth((0.5f + (i % 3) * 0.15f).coerceAtMost(0.9f))
                            .height(13.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    ShimmerBlock(modifier = Modifier.width(80.dp).height(10.dp))
                }
            }
            HorizontalDivider(
                modifier  = Modifier.padding(start = 40.dp, end = 12.dp),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
            )
        }
    }
}

// ── Generic loading indicator (for non-list contexts) ────────────────────────
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
            color        = MaterialTheme.colorScheme.primary,
            strokeWidth  = 2.dp,
            modifier     = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

// ── Error card ────────────────────────────────────────────────────────────────
@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color  = MaterialTheme.colorScheme.errorContainer,
        shape  = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Rounded.WifiOff,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.error,
                modifier           = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text     = message,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick        = onRetry,
                colors         = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier       = Modifier.height(30.dp),
            ) {
                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Status chip with pulsing live dot ────────────────────────────────────────
@Composable
fun StatusChip(
    text: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isOnline) MaterialTheme.colorScheme.secondary
                else          MaterialTheme.colorScheme.error

    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotScale by pulse.animateFloat(
        initialValue  = 0.8f,
        targetValue   = 1.25f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "dot",
    )

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color    = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .scale(if (isOnline) dotScale else 1f)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text       = text,
                style      = MaterialTheme.typography.labelSmall,
                color      = color,
                fontWeight = FontWeight.Medium,
                fontSize   = 10.sp,
            )
        }
    }
}

// ── Typing indicator — staggered bounce ──────────────────────────────────────
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier          = modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val y by transition.animateFloat(
                initialValue  = 0f,
                targetValue   = -6f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0f  at 0   with FastOutSlowInEasing
                        -6f at 200 with FastOutSlowInEasing
                        0f  at 400
                        0f  at 900
                    },
                    initialStartOffset = StartOffset(i * 110),
                    repeatMode         = RepeatMode.Restart,
                ),
                label = "dot$i",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = y.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
            )
        }
    }
}

// ── Swipe-to-reveal delete background ────────────────────────────────────────
@Composable
fun DeleteBackground(fraction: Float, modifier: Modifier = Modifier) {
    val alpha = (fraction * 3f).coerceIn(0f, 1f)
    Box(
        modifier          = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f + alpha * 0.18f))
            .padding(end = 20.dp),
        contentAlignment  = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector        = Icons.Rounded.DeleteOutline,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.error.copy(alpha = alpha),
            modifier           = Modifier
                .size(20.dp)
                .scale(0.8f + alpha * 0.2f),
        )
    }
}
