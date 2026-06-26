---
description: Scaffold a new feature for the Kilocode Android app
agent: android-dev
subtask: true
---
Scaffold a new feature for the Kilocode Android app. Use $ARGUMENTS to describe the feature.

## Steps

1. **Read architecture files** to understand current patterns:
   - `kilocode-android/app/src/main/java/com/kilocode/android/data/model/Models.kt`
   - `kilocode-android/app/src/main/java/com/kilocode/android/data/repository/SessionRepository.kt`
   - `kilocode-android/app/src/main/java/com/kilocode/android/ui/screens/SessionScreen.kt`
   - `kilocode-android/app/src/main/java/com/kilocode/android/ui/navigation/Navigation.kt`

2. **Plan the feature** — output a brief plan:
   - What new data models are needed?
   - What new API endpoints or repository methods?
   - What UI changes (new screen, new component, or modification)?
   - Does it need DataStore persistence?

3. **Implement** following existing patterns:
   - DataStore for persistence (never raw SharedPreferences)
   - StateFlow + collectAsState for reactive UI
   - Retrofit for API calls
   - Jetpack Compose for UI

4. **Compile check**:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:compileDebugKotlin
   ```

5. **Build and install**:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:assembleDebug
   adb -s localhost:5555 install -r app/build/outputs/apk/debug/app-debug.apk
   ```

6. **Verify on emulator** — launch, dump UI, confirm expected behavior.

7. **Commit** with message: `Resolves: #<issue> $ARGUMENTS`

## Constraints

- Always read `AuthPreferencesRepository.kt` before adding persistence
- Always check `SessionRepository.kt` for existing StateFlows before adding new state
- No direct commits to `main` — work on `feature/<issue>-<short-description>` branch
- Commit messages must include `Resolves: #ID`, `Fixes: #ID`, or `Relates: #ID`
