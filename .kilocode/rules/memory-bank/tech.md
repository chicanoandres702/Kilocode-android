# Technical Context: Kilo Code Android App

## Technology Stack

| Technology   | Version | Purpose                         |
| ------------ | ------- | ------------------------------- |
| Android SDK  | 35      | Android platform                |
| Kotlin       | 2.0     | Language for Android development |
| Jetpack Compose | Latest | UI toolkit |
| ViewModel    | 2.9.0   | UI state holder |
| WorkManager  | 2.10.0  | Background task scheduling |
| Navigation   | 2.8.9   | Navigation component |
| Retrofit     | 2.11.0  | HTTP client |
| OkHttp       | 4.12.0  | Networking |
| Gson         | 2.11.0  | JSON serialization |

## Development Environment

### Prerequisites

- Android SDK 35
- Kotlin 2.0
- Android Studio (for emulator)

### Commands

```bash
./gradlew :app:compileDebugKotlin  # Compile Kotlin
./gradlew :app:lint                 # Run lint
./gradlew :app:assembleDebug        # Build debug APK
```

## Project Configuration

### Android Config (`build.gradle.kts`)

- Namespace: `com.kilocode.android`
- Compile SDK: 35
- Min SDK: 26
- Target SDK: 35

### Key Dependencies

```kotlin
// Core
implementation("androidx.core:core-ktx:1.16.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
implementation("androidx.activity:activity-compose:1.10.1")

// Compose
implementation(platform("androidx.compose:compose-bom:2025.03.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.10.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
```

## File Structure

```
app/src/main/java/com/kilocode/android/
├── ui/
│   ├── screens/          # Compose screens
│   ├── components/       # Reusable components
│   ├── navigation/       # Navigation setup
│   └── viewmodel/        # ViewModels
├── data/
│   ├── model/            # Data classes
│   ├── repository/       # Repositories
│   └── api/              # API interfaces
├── worker/               # WorkManager workers
└── di/                   # DI (if needed)
```

## Technical Constraints

### Android Requirements

- Min SDK: 26 (Android 8.0)
- Target SDK: 35
- JVM Target: 17

### Background Tasks

- Use WorkManager for reliable background execution
- Tag work requests for filtering (`prompt`, `branch_creation`)
- Use coroutine workers for async operations

### Networking

- Retrofit for API calls
- OkHttp SSE for streaming responses
- Session management with optimistic updates