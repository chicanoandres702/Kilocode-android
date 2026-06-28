package com.kilocode.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kilocode.android.data.repository.AuthPreferencesRepository
import com.kilocode.android.ui.screens.HomeScreen
import com.kilocode.android.ui.screens.RepoScreen
import com.kilocode.android.ui.screens.SessionScreen
import com.kilocode.android.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Session : Screen("session/{sessionId}") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
    data object Settings : Screen("settings")
    data object Repos : Screen("repos")
}

@Composable
fun KiloCodeNavHost(
    navController: NavHostController,
    serverUrl: String,
    apiServerUrl: String,
    sharedSecret: String?,
    autonomousMode: Boolean,
    onServerUrlChanged: (String, String) -> Unit,
    onAutonomousModeChanged: (Boolean) -> Unit,
    onSharedSecretChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val authPreferencesRepository = remember { AuthPreferencesRepository(context) }
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                serverUrl = serverUrl,
                sharedSecret = sharedSecret,
                onNavigateToSession = { sessionId ->
                    navController.navigate(Screen.Session.createRoute(sessionId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToRepos = {
                    navController.navigate(Screen.Repos.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Screen.Session.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            if (sessionId != null) {
                 SessionScreen(
                     serverUrl = serverUrl,
                     sharedSecret = sharedSecret,
                     sessionId = sessionId,
                     authPreferencesRepository = authPreferencesRepository,
                     onBack = { navController.popBackStack() },
                 )
            } else {
                navController.popBackStack()
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                defaultServerUrl = serverUrl,
                sharedSecret = sharedSecret ?: "",
                autonomousMode = autonomousMode,
                onBack = { navController.popBackStack() },
                onServerUrlChanged = onServerUrlChanged,
                onAutonomousModeChanged = onAutonomousModeChanged,
                onSharedSecretChanged = onSharedSecretChanged,
                onSave = { url, secret ->
                    onServerUrlChanged(url, secret)
                    onSharedSecretChanged(secret)
                }
            )
        }

        composable(Screen.Repos.route) {
            RepoScreen(
                serverUrl = serverUrl,
                apiServerUrl = apiServerUrl,
                sharedSecret = sharedSecret,
                onBack = { navController.popBackStack() },
                onRepoSelected = { repoName ->
                    // Navigate to home with the selected repo as working directory
                    navController.popBackStack()
                },
            )
        }
    }
}
