package com.kilocode.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kilocode.android.BuildConfig
import com.kilocode.android.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    serverUrl: String,
    onBack: () -> Unit,
    onServerUrlChanged: (String) -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    var serverUrlText by remember { mutableStateOf(serverUrl) }
    val sharedSecret by viewModel.sharedSecret.collectAsState(initial = "")
    var sharedSecretText by remember(sharedSecret) { mutableStateOf(sharedSecret ?: "") }
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var showSaveConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Kilo Server Management
            Text(
                text = "Kilo Server Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRunning = com.kilocode.android.data.BinaryManager.isServerRunning.value
                val statusColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = statusColor
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Server Running" else "Server Stopped",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isRunning = com.kilocode.android.data.BinaryManager.isServerRunning.value
                Button(
                    onClick = {
                        com.kilocode.android.data.BinaryManager.startServer(context, serverUrlText)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning
                ) {
                    Text("Start Server")
                }
                OutlinedButton(
                    onClick = {
                        com.kilocode.android.data.BinaryManager.stopServer()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isRunning
                ) {
                    Text("Stop Server")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Server Logs
            Text(
                text = "Server Logs",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                items(com.kilocode.android.data.BinaryManager.logs.size) { index ->
                    Text(
                        text = com.kilocode.android.data.BinaryManager.logs[index],
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = serverUrlText,
                onValueChange = { serverUrlText = it },
                label = { Text("Server URL") },
                placeholder = { Text("http://localhost:4096") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = sharedSecretText.ifEmpty { sharedSecret ?: "" },
                onValueChange = { sharedSecretText = it },
                label = { Text("Shared Secret") },
                placeholder = { Text("Enter API Secret") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                    )
                },
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter the URL of your running Kilo Code server. Default is http://localhost:4096",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onServerUrlChanged(serverUrlText)
                    viewModel.saveSharedSecret(sharedSecretText)
                    showSaveConfirmation = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    viewModel.testConnection(serverUrlText, sharedSecretText.ifEmpty { sharedSecret ?: "" })
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test Connection")
            }

            connectionStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.startsWith("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            if (showSaveConfirmation) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Server URL saved and applied successfully.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Connection Info
            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    InfoRow(label = "Server URL", value = serverUrlText)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(
                        label = "How to connect",
                        value = "Run 'kilo serve' in your project directory",
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(
                        label = "Default port",
                        value = "4096",
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    InfoRow(label = "App Version", value = BuildConfig.VERSION_NAME)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "Build", value = BuildConfig.BUILD_TYPE)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "Kilo Code", value = "Android Client")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(
                        label = "License",
                        value = "MIT",
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kilo.ai/docs"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Documentation")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kilo-Org/kilocode/issues"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report an Issue")
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
