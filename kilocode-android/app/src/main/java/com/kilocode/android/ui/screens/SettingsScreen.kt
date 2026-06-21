package com.kilocode.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Settings Screen ───────────────────────────────────────────────────────────
// NOTE: This screen replaces your existing SettingsScreen.kt.
// Wire up your actual saved-state / DataStore logic where the TODO comments are.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    defaultServerUrl: String,
    onServerUrlChanged: (String) -> Unit,
    onAutonomousModeChanged: (Boolean) -> Unit,
    serverUrl: String          = defaultServerUrl,
    sharedSecret: String       = "",
    onSave: (url: String, secret: String) -> Unit = { url, _ -> onServerUrlChanged(url) },
) {
    var urlInput    by remember { mutableStateOf(serverUrl) }
    var secretInput by remember { mutableStateOf(sharedSecret) }
    var secretVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Settings",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier           = Modifier.size(22.dp),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(urlInput, secretInput) },
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Server section ─────────────────────────────────────────────
            SettingsSection(title = "Server") {
                SettingsTextField(
                    label        = "Server URL",
                    value        = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder  = "http://localhost:3000",
                    leadingIcon  = Icons.Rounded.Language,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsTextField(
                    label         = "Shared secret",
                    value         = secretInput,
                    onValueChange = { secretInput = it },
                    placeholder   = "Optional authentication token",
                    leadingIcon   = Icons.Rounded.Lock,
                    isPassword    = true,
                    passwordVisible = secretVisible,
                    onTogglePassword = { secretVisible = !secretVisible },
                )
            }

            // ── About section ──────────────────────────────────────────────
            SettingsSection(title = "About") {
                SettingsInfoRow(
                    icon  = Icons.Rounded.Info,
                    label = "Version",
                    value = "1.0.0",
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 2.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                SettingsInfoRow(
                    icon  = Icons.Rounded.Code,
                    label = "Built with",
                    value = "Kilo Code · Anthropic",
                )
            }
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content,
            )
        }
    }
}

// ── Text field row ────────────────────────────────────────────────────────────
@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
) {
    Column {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, fontSize = 13.sp) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            textStyle     = MaterialTheme.typography.bodyMedium,
            visualTransformation = if (isPassword && !passwordVisible)
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            else
                androidx.compose.ui.text.input.VisualTransformation.None,
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            },
            trailingIcon = if (isPassword && onTogglePassword != null) ({
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff
                                      else                Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Hide" else "Show",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }) else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            ),
        )
    }
}

// ── Info row ──────────────────────────────────────────────────────────────────
@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier           = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
    }
}
