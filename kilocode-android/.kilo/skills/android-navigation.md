---
name: android-navigation
description: Navigation and screen architecture for the Kilocode Android app. Use when adding new screens, modifying navigation routes, or understanding the NavHost structure.
triggers:
  - navigation
  - screen
  - route
  - NavHost
  - NavGraph
  - composable route
---

# Android Navigation

## Entry Point

`MainActivity.kt` → calls `setContent { KiloTheme { Navigation() } }`

## Navigation File

`ui/navigation/Navigation.kt` — defines all routes via `NavHost`.

## Current Routes

| Route | Screen | Purpose |
|-------|--------|---------|
| `sessionList` | `SessionListScreen` | List of past sessions |
| `session/{sessionID}` | `SessionScreen` | Chat with a specific session |
| `settings` | `SettingsScreen` | App settings |

## Adding a New Screen

1. Create composable in `ui/screens/`
2. Add route in `Navigation.kt`:
   ```kotlin
   composable("route_name") { backStackEntry ->
       YourScreen(...)
   }
   ```
3. Navigate via `navController.navigate("route_name")`

## Passing Arguments

```kotlin
composable("session/{sessionID}") { backStackEntry ->
    val sessionID = backStackEntry.arguments?.getString("sessionID") ?: return@composable
    SessionScreen(sessionID = sessionID, ...)
}
```

## Current Architecture

- `Navigation.kt` creates `AuthPreferencesRepository` via `remember { AuthPreferencesRepository(context) }` and passes it to screens
- `SessionScreen` accepts `authPreferencesRepository` for agent persistence
- `SessionListScreen` accepts `authPreferencesRepository` for displaying session metadata
