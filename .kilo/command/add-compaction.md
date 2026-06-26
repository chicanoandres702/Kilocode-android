---
description: Implement the session compaction feature end-to-end
agent: android-dev
subtask: true
---
Implement session compaction by wiring the existing `compactSession` API endpoint into the repository and UI.

## Context

`KiloCodeApi.kt` already declares `@POST("session/{sessionID}/compact") suspend fun compactSession(@Path("sessionID") sessionID: String): Response<JsonObject>` but it is never called.

## Steps

1. **Read current state** of these files:
   - `kilocode-android/app/src/main/java/com/kilocode/android/data/api/KiloCodeApi.kt`
   - `kilocode-android/app/src/main/java/com/kilocode/android/data/repository/SessionRepository.kt`
   - `kilocode-android/app/src/main/java/com/kilocode/android/ui/screens/SessionScreen.kt`

2. **Add to `SessionRepository`**:
   - Add a `compactSession(sessionID: String)` suspend function that calls `api.compactSession(sessionID)`
   - Add a `_compactionStatus: MutableStateFlow<String?>` (or similar) for tracking state
   - Expose `compactionStatus: StateFlow<String?>`

3. **Add to `SessionScreen`**:
   - Add a compaction button or auto-trigger (based on user preference)
   - Collect `compactionStatus` and show feedback (e.g., snackbar or indicator)
   - Call `sessionRepository.compactSession(sessionID)` on trigger

4. **Compile check**:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:compileDebugKotlin
   ```
   Fix any errors.

5. **Build and install**:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:assembleDebug
   adb -s localhost:5555 install -r app/build/outputs/apk/debug/app-debug.apk
   ```

6. **Verify on emulator** — launch app, confirm UI shows compaction trigger.

7. **Commit** with message: `Resolves: #<issue> implement session compaction`

## Constraints

- Do NOT modify the API interface — `compactSession` already exists
- Follow existing StateFlow patterns in `SessionScreen`
- Use `LaunchedEffect` or `rememberCoroutineScope` for triggering compaction from UI
