package com.kilocode.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Lightweight Android scrollbar — fades in on scroll activity, fades out after idle.
 * No Accompanist dependency required.
 */
@Composable
fun AndroidScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    minThumbFraction: Float = 0.06f,
) {
    val needsScrollbar by remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size
        }
    }
    if (!needsScrollbar) return

    // Fade in when scrolling, fade out after 1.2 s idle
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 150 else 500),
        label         = "scrollbarAlpha",
    )

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            visible = true
        } else {
            delay(1200)
            visible = false
        }
    }

    val thumbFraction by remember {
        derivedStateOf {
            val info    = listState.layoutInfo
            val total   = info.totalItemsCount.coerceAtLeast(1)
            val visible = info.visibleItemsInfo.size
            (visible.toFloat() / total).coerceIn(minThumbFraction, 1f)
        }
    }
    val thumbOffset by remember {
        derivedStateOf {
            val info  = listState.layoutInfo
            val total = info.totalItemsCount.coerceAtLeast(1)
            val first = listState.firstVisibleItemIndex
            (first.toFloat() / total).coerceIn(0f, 1f - thumbFraction)
        }
    }

    BoxWithConstraints(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    ) {
        val track  = maxHeight
        val thumb  = track * thumbFraction
        val offset = track * thumbOffset

        Box(
            modifier = Modifier
                .padding(top = offset)
                .height(thumb)
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
        )
    }
}
