package com.kilocode.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kilocode.android.ui.viewmodel.SettingsViewModel

// ── Settings Screen ───────────────────────────────────────────────────────────
// NOTE: This screen replaces your existing SettingsScreen.kt.
// Wire up your actual saved-state / DataStore logic where the TODO comments are.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    defaultServerUrl: String,
    defaultApiServerUrl: String,
    sharedSecret: String = "",
    autonomousMode: Boolean = false,
    onServerUrlChanged: (String, String) -> Unit,
    onAutonomousModeChanged: (Boolean) -> Unit,
    onSharedSecretChanged: (String) -> Unit,
    onApiServerUrlChanged: (String) -> Unit = {},
    onSave: (url: String, secret: String) -> Unit = { url, secret ->
        onServerUrlChanged(url, secret)
        onSharedSecretChanged(secret)
    },
    viewModel: SettingsViewModel = viewModel(),
) {
    val serverUrlState by viewModel.serverUrl.collectAsState(initial = defaultServerUrl)
    val apiServerUrlState by viewModel.apiServerUrl.collectAsState(initial = defaultApiServerUrl)
    val secretState by viewModel.sharedSecret.collectAsState(initial = null)
    val autonomousState by viewModel.autonomousMode.collectAsState(initial = autonomousMode)
    val connectionStatus by viewModel.connectionStatus.collectAsState(initial = null)

    var urlInput by remember(serverUrlState) { mutableStateOf(serverUrlState ?: defaultServerUrl) }
    var apiServerUrlInput by remember(apiServerUrlState) { mutableStateOf(apiServerUrlState ?: defaultApiServerUrl) }
    var secretInput by remember(secretState) { mutableStateOf(secretState ?: sharedSecret) }
    var secretVisible by remember { mutableStateOf(false) }
    var autonomousInput by remember(autonomousState) { mutableStateOf(autonomousState) }

    LaunchedEffect(secretState) {
        secretInput = secretState ?: sharedSecret
    }

    fun saveSettings() {
        val normalizedUrl = urlInput.trim().ifBlank { defaultServerUrl }
        val normalizedApiUrl = apiServerUrlInput.trim().ifBlank { defaultApiServerUrl }
        viewModel.saveServerUrl(normalizedUrl)
        viewModel.saveSharedSecret(secretInput)
        viewModel.saveAutonomousMode(autonomousInput)
        viewModel.saveApiServerUrl(normalizedApiUrl)
        onSave(normalizedUrl, secretInput)
        onApiServerUrlChanged(normalizedApiUrl)
        onAutonomousModeChanged(autonomousInput)
    }

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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = ::saveSettings,
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
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Server URL (e.g., http://18.191.142.105:4096)") },
                    placeholder = { Text("http://18.191.142.105:4096") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiServerUrlInput,
                    onValueChange = { apiServerUrlInput = it },
                    label = { Text("Planning API URL (e.g., http://10.0.2.2:3001)") },
                    placeholder = { Text("http://10.0.2.2:3001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = secretInput,
                    onValueChange = { secretInput = it },
                    label = { Text("Shared secret") },
                    placeholder = { Text("Optional authentication token") },
                    singleLine = true,
                    visualTransformation = if (secretVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { secretVisible = !secretVisible }) {
                            Icon(
                                imageVector = if (secretVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (secretVisible) "Hide secret" else "Show secret",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                SwitchSetting(
                    title = "Autonomous mode",
                    description = "Start the binary with --auto and ask the agent to continue without waiting for confirmation.",
                    icon = Icons.Rounded.AutoAwesome,
                    checked = autonomousInput,
                    onCheckedChange = { autonomousInput = it },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { viewModel.testConnection(urlInput, secretInput) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Sync, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Test")
                    }
                    Button(
                        onClick = ::saveSettings,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
                connectionStatus?.let {
                    val color = when (it) {
                        "Connected" -> MaterialTheme.colorScheme.primary
                        "Failed" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = it,
                        color = color,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
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
                value = "Kilo Code",
            )
            }
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────
@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

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
