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
import com.kilocode.android.ui.screens.PlanningScreen
import com.kilocode.android.ui.screens.PlanningWizardScreen
import com.kilocode.android.ui.screens.RepoScreen
import com.kilocode.android.ui.screens.SessionScreen
import com.kilocode.android.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home?directory={directory}") {
        fun createRoute(directory: String) = "home?directory=${java.net.URLEncoder.encode(directory, "UTF-8")}"
    }
    data object Session : Screen("session/{sessionId}") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
    data object Settings : Screen("settings")
    data object Repos : Screen("repos")
    data object Planning : Screen("planning")
    data object PlanningWizard : Screen("planning/wizard")
}

@Composable
fun KiloCodeNavHost(
    navController: NavHostController,
    serverUrl: String,
    apiServerUrl: String,
    sharedSecret: String?,
    autonomousMode: Boolean,
    onServerUrlChanged: (String, String) -> Unit,
    onApiServerUrlChanged: (String) -> Unit,
    onAutonomousModeChanged: (Boolean) -> Unit,
    onSharedSecretChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val authPreferencesRepository = remember { AuthPreferencesRepository(context) }

    NavHost(
        navController = navController,
        startDestination = Screen.Repos.route,
    ) {
         composable(
             route = Screen.Home.route,
             arguments = listOf(
                 navArgument("directory") {
                     type = NavType.StringType
                     defaultValue = "/"
                 }
             ),
         ) { backStackEntry ->
             val directory = java.net.URLDecoder.decode(
                 backStackEntry.arguments?.getString("directory") ?: "/",
                 "UTF-8"
             )
              HomeScreen(
                  serverUrl = serverUrl,
                  sharedSecret = sharedSecret,
                  initialDirectory = directory,
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
                  onNavigateToPlanning = {
                      navController.navigate(Screen.Planning.route) {
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
                defaultApiServerUrl = apiServerUrl,
                sharedSecret = sharedSecret ?: "",
                autonomousMode = autonomousMode,
                onBack = { navController.popBackStack() },
                onServerUrlChanged = onServerUrlChanged,
                onAutonomousModeChanged = onAutonomousModeChanged,
                onSharedSecretChanged = onSharedSecretChanged,
                onApiServerUrlChanged = onApiServerUrlChanged,
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
                    onRepoSelected = { repoName, repoPath ->
                        navController.navigate(Screen.Home.createRoute(repoPath)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Screen.Planning.route) {
                PlanningScreen(
                    serverUrl = serverUrl,
                    apiServerUrl = apiServerUrl,
                    sharedSecret = sharedSecret,
                    onBack = { navController.popBackStack() },
                    onNavigateToWizard = {
                        navController.navigate(Screen.PlanningWizard.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Screen.PlanningWizard.route) {
                PlanningWizardScreen(
                    serverUrl = serverUrl,
                    apiServerUrl = apiServerUrl,
                    sharedSecret = sharedSecret,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() },
                )
            }
    }
}
