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
import com.kilocode.android.BuildConfig
import com.kilocode.android.ui.navigation.KiloCodeNavHost
import com.kilocode.android.ui.theme.KiloCodeTheme

import androidx.compose.runtime.collectAsState
import com.kilocode.android.data.repository.AuthPreferencesRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KiloCodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val authRepo = remember { AuthPreferencesRepository(context) }
                    val sharedSecret by authRepo.sharedSecretFlow.collectAsState(initial = BuildConfig.KILO_SHARED_SECRET)
                    val savedServerUrl by authRepo.serverUrlFlow.collectAsState(initial = null)
                    val autonomousMode by authRepo.autonomousModeFlow.collectAsState(initial = false)
                    var serverUrl by remember(savedServerUrl) { mutableStateOf(savedServerUrl ?: BuildConfig.DEFAULT_SERVER_URL) }

                    LaunchedEffect(savedServerUrl) {
                        savedServerUrl?.let {
                            serverUrl = it
                        }
                    }

                    LaunchedEffect(Unit) {
                        com.kilocode.android.data.BinaryManager.startServer(context, serverUrl, autonomousMode)
                    }

                    KiloCodeNavHost(
                        navController = navController,
                        serverUrl = serverUrl,
                        sharedSecret = sharedSecret,
                        onServerUrlChanged = { newUrl ->
                            val normalizedUrl = newUrl.trim().ifBlank { BuildConfig.DEFAULT_SERVER_URL }
                            serverUrl = normalizedUrl

                            // Persist the new URL
                            kotlinx.coroutines.GlobalScope.launch {
                                authRepo.saveServerUrl(normalizedUrl)
                            }

                            // ApiClient update will be handled by the screens when they recompose due to serverUrl/sharedSecret change
                            com.kilocode.android.data.BinaryManager.stopServer()
                            com.kilocode.android.data.BinaryManager.startServer(context, normalizedUrl, autonomousMode)
                        },
                        onAutonomousModeChanged = { enabled ->
                            if (com.kilocode.android.data.BinaryManager.isServerRunning.value) {
                                com.kilocode.android.data.BinaryManager.stopServer()
                                com.kilocode.android.data.BinaryManager.startServer(context, serverUrl, enabled)
                            }
                        },
                    )
                }
            }
        }
    }
}
