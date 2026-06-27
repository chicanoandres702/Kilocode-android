# Fix: Android App SSE Endpoint Path Wrong

## Problem
The Android app's `SessionRepository.connectSse()` connects to `session/{sessionID}/event` which **does not exist** in the Kilo server API. The server only exposes:
- `GET /event` → global event stream (all sessions, all events)
- `GET /global/event` → same, global scope

Because the SSE endpoint is wrong, the app never receives `message.part.delta` events (or any other SSE events). This means:
- No streaming AI response appears in the UI
- `session.turn.open`, `session.turn.close`, `session.idle` events are never received
- `isLoading` stays `true` forever after sending a prompt (UI freezes)
- The send button shows a spinner indefinitely

## Root Cause
`SessionRepository.kt` line 366:
```kotlin
val path = "session/$sessionId/event?directory=$encodedDirectory&workspace=$encodedWorkspace"
```
This path doesn't match any server endpoint. The OkHttp call likely gets a 404, the retry loop kicks in with exponential backoff, and the user never sees a response.

## Fix

### Step 1: Change SSE path in `SessionRepository.connectSse()`
**File**: `kilocode-android/app/src/main/java/com/kilocode/android/data/repository/SessionRepository.kt`

Change line 366 from:
```kotlin
val path = "session/$sessionId/event?directory=$encodedDirectory&workspace=$encodedWorkspace"
```
to:
```kotlin
val path = "event"
```

Remove the now-unused `directory` and `workspace` parameters from `connectSse()` (lines 358-366). The method signature becomes:
```kotlin
fun connectSse(sessionId: String) {
```

### Step 2: Add sessionID filtering in `handleSseEvent()`
Since `/event` streams events for ALL sessions, the client must filter by the current sessionID. 

In `handleSseEvent()`, after extracting `properties`, check if the event belongs to the current session. For events that carry `sessionID` in properties (most do), compare it against the current session's ID. Skip events from other sessions.

Add a helper:
```kotlin
private fun isForCurrentSession(properties: Map<String, Any>?): Boolean {
    val eventSessionId = properties?.get("sessionID") as? String
    val currentId = _currentSession.value?.id
    return eventSessionId == null || currentId == null || eventSessionId == currentId
}
```

Call it at the top of `handleSseEvent()` after extracting `properties`:
```kotlin
if (!isForCurrentSession(properties)) return
```

### Step 3: Update `connectSse` call site in `SessionScreen.kt`
**File**: `kilocode-android/app/src/main/java/com/kilocode/android/ui/screens/SessionScreen.kt`

Line 130-133: Remove the `directory` argument:
```kotlin
LaunchedEffect(sessionId) {
    autonomousMode = false
    repository.selectSession(sessionId)
    repository.connectSse(sessionId)
}
```

### Step 4: Rebuild APK
```bash
cd kilocode-android && ./gradlew assembleDebug
```

## Verification
- APK builds without errors
- After install, sending a prompt should show streaming response (text appears progressively)
- `isLoading` resets to `false` after the turn completes
- SSE connection status shows "Live" in the UI
