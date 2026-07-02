# System Patterns: Kilo Code Android App

## Architecture Overview

```
app/
├── src/main/java/com/kilocode/android/
│   ├── ui/
│   │   ├── screens/          # Compose screens (Home, Planning, TaskManager)
│   │   ├── components/       # Reusable UI components
│   │   ├── navigation/       # Navigation graph and routes
│   │   └── viewmodel/        # ViewModel factories
│   ├── data/
│   │   ├── model/            # Data classes (Message, Task, Agent)
│   │   ├── repository/       # Data sources (SessionRepository, TaskManagerRepository)
│   │   └── api/              # API client and interfaces
│   ├── worker/               # WorkManager workers (BranchWorker, PromptWorker)
│   ├── ui/util/              # Utility classes (BranchManager)
│   └── di/                   # Dependency injection
```

## Key Design Patterns

### 1. Navigation Pattern

Uses Jetpack Navigation Compose with sealed routes:
```kotlin
sealed class Screen(val route: String) {
    data object Home : Screen("home?directory={directory}")
    data object Session : Screen("session/{sessionId}")
    data object Planning : Screen("planning")
    data object PlanningWizard : Screen("planning/wizard")
    data object TaskManager : Screen("task-manager")
}
```

### 2. Repository Pattern

Data abstraction layer for API calls and local state:
- `SessionRepository`: Manages SSE sessions, prompts, and agent communication
- `TaskManagerRepository`: Manages WorkManager tasks for background execution
- `PlanningRepository`: Handles feature generation and issue creation

### 3. ViewModel Pattern

UI state holders with coroutine scopes:
- `SessionViewModel`: Session state and message management
- `TaskManagerViewModel`: Background task state and operations

### 4. WorkManager Integration

Background task management:
- `BranchWorker`: Creates branches from generated issues
- `PromptWorker`: Sends prompts to AI agents
- Tagged work requests for filtering and tracking

## UI Patterns

### State Management

- `remember` and `mutableStateOf` for local screen state
- `collectAsState()` for ViewModel flows
- `LaunchedEffect` for side effects

### Compose Conventions

- Surface components for cards and containers
- IconButton for actions
- FilterChip for selection
- LazyColumn for lists

## File Naming Conventions

- Screens: PascalCase (`HomeScreen.kt`, `PlanningWizardScreen.kt`)
- ViewModels: PascalCase with `ViewModel` suffix
- Repositories: PascalCase with `Repository` suffix
- Workers: PascalCase with `Worker` suffix
- Utilities: PascalCase (`BranchManager.kt`)