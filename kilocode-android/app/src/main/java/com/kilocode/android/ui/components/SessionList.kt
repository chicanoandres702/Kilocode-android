package com.kilocode.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.model.Session
import com.kilocode.android.ui.util.staggerDelay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionList(
    sessions: List<Session>,
    onSessionClick: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) {
        EmptySessionList(onNewSession = onNewSession, modifier = modifier)
        return
    }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(end = 5.dp),
            state          = listState,
            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
        ) {
            itemsIndexed(
                items = sessions,
                key   = { _, s -> s.id?.takeIf(String::isNotEmpty) ?: s.hashCode() },
            ) { index, session ->
                SessionListItem(
                    session  = session,
                    onClick  = { session.id?.let(onSessionClick) },
                    onDelete = { session.id?.let(onDeleteSession) },
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
                    ),
                )
            }
        }

        // Scrollbar
        AndroidScrollbar(
            listState = listState,
            modifier  = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .width(4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListItem(
    session: Session,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat by remember { mutableStateOf(SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())) }
    val date              = Date(session.time?.updated ?: 0)
    var showDeleteDialog  by remember { mutableStateOf(false) }

    // Swipe-to-delete
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            false // don't actually dismiss until confirmed
        },
        positionalThreshold = { it * 0.38f },
    )

    SwipeToDismissBox(
        state            = dismissState,
        modifier         = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val fraction = dismissState.progress
            DeleteBackground(fraction = fraction)
        },
    ) {
        // Row content
        Surface(
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        modifier           = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = session.title.orEmpty().ifEmpty { "New session" },
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                        Text(
                            text     = dateFormat.format(date),
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                        )
                    }
                    Icon(
                        imageVector        = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        modifier           = Modifier.size(16.dp),
                    )
                }
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 39.dp, end = 0.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete session?") },
            text   = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
fun EmptySessionList(
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
            modifier = Modifier.size(60.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    modifier           = Modifier.size(26.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text("No sessions yet", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Tap + to start coding with AI",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onNewSession,
            shape   = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("New session", fontWeight = FontWeight.SemiBold)
        }
    }
}
