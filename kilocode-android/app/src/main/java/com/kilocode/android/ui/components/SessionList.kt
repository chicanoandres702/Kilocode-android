package com.kilocode.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilocode.android.data.model.Session
import java.text.SimpleDateFormat
import java.util.*

// ── Session list ──────────────────────────────────────────────────────────────
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
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            items(
                items = sessions,
                key   = { it.id?.takeIf(String::isNotEmpty) ?: sessions.indexOf(it) },
            ) { session ->
                SessionListItem(
                    session  = session,
                    onClick  = { session.id?.let(onSessionClick) },
                    onDelete = { session.id?.let(onDeleteSession) },
                )
            }
        }
    }
}

// ── Session item ──────────────────────────────────────────────────────────────
@Composable
fun SessionListItem(
    session: Session,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat by remember { mutableStateOf(SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())) }
    val date = Date(session.time?.updated ?: 0)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading icon
        Surface(
            shape  = RoundedCornerShape(12.dp),
            color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text      = session.title.orEmpty().ifEmpty { "New session" },
                style     = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color     = MaterialTheme.colorScheme.onSurface,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = dateFormat.format(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontSize = 11.sp,
            )
        }

        // Delete
        IconButton(
            onClick  = { showDeleteDialog = true },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector        = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete session",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier           = Modifier.size(18.dp),
            )
        }
    }

    // Subtle row divider
    HorizontalDivider(
        modifier  = Modifier.padding(start = 68.dp, end = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete session?") },
            text   = { Text("This session and all its messages will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
fun EmptySessionList(
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape  = RoundedCornerShape(24.dp),
            color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier           = Modifier.size(36.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text   = "No sessions yet",
            style  = MaterialTheme.typography.titleMedium,
            color  = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text   = "Start a new session to code with AI",
            style  = MaterialTheme.typography.bodyMedium,
            color  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onNewSession,
            shape   = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector        = Icons.Rounded.Add,
                contentDescription = null,
                modifier           = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("New session", fontWeight = FontWeight.SemiBold)
        }
    }
}
