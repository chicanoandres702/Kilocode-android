package com.kilocode.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kilocode.android.ui.screens.HomeScreen
import com.kilocode.android.ui.screens.SessionScreen
import com.kilocode.android.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Session : Screen("session/{sessionId}") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun KiloCodeNavHost(
    navController: NavHostController,
    serverUrl: String,
    sharedSecret: String?,
    onServerUrlChanged: (String) -> Unit,
    onAutonomousModeChanged: (Boolean) -> Unit,
) {
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
                    onBack = { navController.popBackStack() },
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                defaultServerUrl = serverUrl,
                onBack = { navController.popBackStack() },
                onServerUrlChanged = onServerUrlChanged,
                onAutonomousModeChanged = onAutonomousModeChanged,
            )
        }
    }
}
