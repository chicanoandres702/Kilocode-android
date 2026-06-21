package com.kilocode.android.ui.util

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

// ── Spring presets ────────────────────────────────────────────────────────────
val SpringSnappy   = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
val SpringBouncy   = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)
val SpringOvershoot= spring<Float>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)

// ── Tween presets ─────────────────────────────────────────────────────────────
val TweenFast   = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)
val TweenMedium = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
val TweenSlow   = tween<Float>(durationMillis = 420, easing = FastOutSlowInEasing)

// ── Press-scale modifier — gives a tactile "squish" on touch ─────────────────
fun Modifier.pressScale(
    pressedScale: Float = 0.95f,
    onTap: (() -> Unit)? = null,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue  = if (pressed) pressedScale else 1f,
        animationSpec = SpringSnappy,
        label        = "pressScale",
    )
    this
        .scale(scale)
        .pointerInput(onTap) {
            detectTapGestures(
                onPress  = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap    = { onTap?.invoke() },
            )
        }
}

// ── Slide-fade enter for messages ─────────────────────────────────────────────
fun userMessageEnter(): EnterTransition =
    fadeIn(tween(200)) + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 3 }

fun assistantMessageEnter(): EnterTransition =
    fadeIn(tween(200)) + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 3 }

// ── Generic slide-up enter (for bottom sheet contents, FABs, etc.) ────────────
fun slideUpEnter(delayMs: Int = 0): EnterTransition =
    fadeIn(tween(220, delayMillis = delayMs)) +
    slideInVertically(tween(280, delayMillis = delayMs, easing = FastOutSlowInEasing)) { it / 4 }

fun slideUpExit(): ExitTransition =
    fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 4 }

// ── Stagger helper — call with item index to get a per-item delay ─────────────
fun staggerDelay(index: Int, perItemMs: Int = 40, maxMs: Int = 300): Int =
    (index * perItemMs).coerceAtMost(maxMs)

// ── graphicsLayer alpha shorthand ─────────────────────────────────────────────
fun Modifier.alpha(value: Float): Modifier = graphicsLayer { alpha = value }
