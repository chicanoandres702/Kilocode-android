package com.kilocode.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.kilocode.android.ui.navigation.KiloCodeNavHost
import com.kilocode.android.ui.theme.KiloCodeTheme
import com.kilocode.android.data.repository.AuthPreferencesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KiloCodeTheme {
                val scope = rememberCoroutineScope()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val context = LocalContext.current
                    val authRepo = remember { AuthPreferencesRepository(context) }
                    var serverUrl by remember { mutableStateOf(BuildConfig.DEFAULT_SERVER_URL) }
                    var apiServerUrl by remember { mutableStateOf(BuildConfig.API_SERVER_URL) }
                    var sharedSecret by remember { mutableStateOf("") }
                    var autonomousMode by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        serverUrl = authRepo.serverUrlFlow.first() ?: BuildConfig.DEFAULT_SERVER_URL
                        apiServerUrl = BuildConfig.API_SERVER_URL
                        sharedSecret = authRepo.sharedSecretFlow.first() ?: ""
                        autonomousMode = authRepo.autonomousModeFlow.first() ?: false

                        com.kilocode.android.data.BinaryManager.startServer(context, serverUrl, autonomousMode)
                    }

                    val navController = rememberNavController()

                    KiloCodeNavHost(
                        navController = navController,
                        serverUrl = serverUrl,
                        apiServerUrl = apiServerUrl,
                        sharedSecret = sharedSecret,
                        autonomousMode = autonomousMode,
                        onServerUrlChanged = { newUrl, newSecret ->
                            val normalizedUrl = newUrl.trim().ifBlank { BuildConfig.DEFAULT_SERVER_URL }
                            serverUrl = normalizedUrl

                            // Persist the new URL and shared secret together.
                            scope.launch {
                                authRepo.saveServerUrl(normalizedUrl)
                                authRepo.saveSharedSecret(newSecret)
                            }

                            // ApiClient update will be handled by the screens when they recompose due to serverUrl/sharedSecret change
                            com.kilocode.android.data.BinaryManager.stopServer()
                            com.kilocode.android.data.BinaryManager.startServer(context, normalizedUrl, autonomousMode)
                        },
                        onApiServerUrlChanged = { _ ->
                            // Ignore user settings, enforced via BuildConfig
                            apiServerUrl = BuildConfig.API_SERVER_URL
                        },
                        onAutonomousModeChanged = { enabled ->
                            scope.launch {
                                authRepo.saveAutonomousMode(enabled)
                            }
                            if (com.kilocode.android.data.BinaryManager.isServerRunning.value) {
                                com.kilocode.android.data.BinaryManager.stopServer()
                                com.kilocode.android.data.BinaryManager.startServer(context, serverUrl, enabled)
                            }
                        },
                        onSharedSecretChanged = { newSecret ->
                            scope.launch {
                                authRepo.saveSharedSecret(newSecret)
                            }
                        },
                    )
                }
            }
        }
    }
}
