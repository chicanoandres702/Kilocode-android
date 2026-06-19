/*
 * [Parent Feature/Milestone] Phase 2: Chat Interface
 * [Child Task/Issue] #2
 * [Subtask] Scaffold Chat Message Bubble
 * [Upstream] SessionViewModel -> [Downstream] SessionScreen
 * [Law Check] 60 lines | Passed Do It Check
 */
package com.kilocode.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kilocode.android.data.model.Part

@Composable
fun MessageBubble(
    isUser: Boolean,
    parts: List<Part>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                for (part in parts) {
                    Text(
                        text = part.text ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
