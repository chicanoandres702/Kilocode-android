package com.kilocode.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.ui.navigation.KiloCodeNavHost
import com.kilocode.android.ui.theme.KiloCodeTheme

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
                    var serverUrl by remember { mutableStateOf(BuildConfig.DEFAULT_SERVER_URL) }

                    LaunchedEffect(Unit) {
                        com.kilocode.android.data.BinaryManager.startServer(context, serverUrl)
                    }

                    KiloCodeNavHost(
                        navController = navController,
                        serverUrl = serverUrl,
                        onServerUrlChanged = { newUrl ->
                            serverUrl = newUrl
                            ApiClient.updateBaseUrl(newUrl)
                            com.kilocode.android.data.BinaryManager.stopServer()
                            com.kilocode.android.data.BinaryManager.startServer(context, newUrl)
                        },
                    )
                }
            }
        }
    }
}
