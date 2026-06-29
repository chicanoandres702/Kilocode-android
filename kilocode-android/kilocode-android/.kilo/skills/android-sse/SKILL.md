---
name: android-sse
description: Guide for implementing and debugging SSE (Server-Sent Events) streaming in the Kilocode Android app. Use when working on real-time message streaming, event handling, session lifecycle events, or OkHttp SSE connection management in the Android project.
---

# Android SSE Streaming Skill

This skill provides guidance for working with the SSE (Server-Sent Events) implementation in the Kilocode Android application.

## Architecture Overview

The SSE streaming system connects to a Kilo/OpenCode server to receive real-time events about sessions, messages, and parts.

### Key Components

| Component | File | Responsibility |
|-----------|------|----------------|
| `SessionRepository` | `data/repository/SessionRepository.kt` | SSE subscription, event parsing, state management |
| `ApiClient` | `data/api/ApiClient.kt` | OkHttp client, stream call creation, auth headers |
| `KiloCodeApi` | `data/api/KiloCodeApi.kt` | Retrofit interface (REST endpoints only — no SSE) |
| `AuthInterceptor` | `data/api/AuthInterceptor.kt` | Adds `X-Kilo-Auth` header to all requests |
| `Models` | `data/model/Models.kt` | Data classes: `Session`, `Message`, `Part`, `Agent`, `ModelOption` |

## SSE Connection Flow

```
SessionRepository.connectSse(sessionId, directory)
  → disconnectSse() (cancel existing)
  → Encode directory parameter
  → Launch coroutine with exponential backoff retry loop
  → apiClient.createStreamCall("global/event?directory=...")
  → call.execute() → response.body.source()
  → Read lines: "event:<type>" then "data:<json>"
  → handleSseEvent(type, data)
```

### Connection Details

- **Endpoint**: `GET global/event?directory=<encoded>` (global events, not session-scoped)
- **Accept header**: `text/event-stream`
- **Auth**: `X-Kilo-Auth` header added by `AuthInterceptor` with shared secret
- **Retry**: Exponential backoff starting at 1s, max 30s
- **Auth failures**: 401/403 stop retries immediately

## Event Envelope Format

Events follow this structure:

```json
{
  "id": "string",
  "type": "<event-type>",
  "properties": { ... },
  "additionalProperties": false
}
```

For `global/event` endpoint, events are wrapped:

```json
{
  "directory": "...",
  "project": "...",
  "workspace": "...",
  "payload": {
    "type": "<actual-event-type>",
    "properties": { ... }
  }
}
```

## Event Types Handled

### Session Lifecycle
| Event Type | Action |
|------------|--------|
| `session.status` | Update `_isConnected` and `_isLoading` based on idle state |
| `session.idle` | (handled via session.status) |
| `session.created` | Logged only |
| `session.updated` | Update `_currentSession` and `_sessions` list |
| `session.deleted` | Logged only |
| `session.error` | Set `_error` with formatted error message |
| `session.turn.open` | Logged only |
| `session.turn.close` | Logged only |
| `session.diff` | Logged only |
| `session.compacted` | Logged only |

### Message/Part Events
| Event Type | Action |
|------------|--------|
| `message.updated` | Upsert message into `_messages` |
| `message.removed` | Remove message by ID from `_messages` |
| `message.part.updated` | Upsert part into `_parts[messageID]` |
| `message.part.removed` | Remove part by ID from `_parts[messageID]` |

### Other Events
| Event Type | Action |
|------------|--------|
| `server.heartbeat` | Logged — connection alive |
| `permission.asked` | Logged for future UI handling |
| `permission.replied` | Logged |
| `server.instance.disposed` | Set `_isConnected = false` |
| `installation.updated` | Logged |
| `lsp.client.diagnostics` | Logged |
| `provider.updated` | Logged |
| `provider.auth_provider.updated` | Logged |

## State Management

`SessionRepository` exposes these `StateFlow`s for Compose UI:

```kotlin
val sessions: StateFlow<List<Session>>
val currentSession: StateFlow<Session?>
val messages: StateFlow<List<Message>>
val parts: StateFlow<Map<String, List<Part>>>  // keyed by messageID
val agents: StateFlow<List<Agent>>
val models: StateFlow<List<ModelOption>>
val selectedAgent: StateFlow<Agent?>
val selectedModel: StateFlow<ModelOption?>
val project: StateFlow<Project?>
val files: StateFlow<List<FileNode>>
val isLoading: StateFlow<Boolean>
val isConnected: StateFlow<Boolean>
val error: StateFlow<String?>
```

### Upsert Patterns

- **Messages**: Replace by `id` in list, or append if new
- **Parts**: Replace by `id` within message's part list, or append if new
- **Sessions**: Replace by `id` in list

## Common Patterns

### Adding a New Event Type

1. Add a new `case` in `handleSseEvent` `when` block (line ~333)
2. Extract properties from the `properties` map
3. Parse nested objects using `GSON.fromJson(GSON.toJsonTree(data), TargetClass::class.java)`
4. Update the appropriate `StateFlow`

### Debugging SSE Issues

```bash
# Monitor SSE connection and events
adb -s localhost:5555 logcat -s "SessionRepo:D"

# Check for auth failures
adb -s localhost:5555 logcat | grep -E "(401|403|X-Kilo-Auth)"

# Monitor network requests
adb -s localhost:5555 logcat | grep -E "(SSE|EventSource|stream)"
```

### Testing SSE on Emulator

1. Build: `./gradlew :app:assembleDebug`
2. Install: `adb -s localhost:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
3. Launch: `adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity`
4. Monitor: `adb -s localhost:5555 logcat -s "SessionRepo:D"`

## Known Issues & Gotchas

1. **Global vs Session Events**: Current implementation uses `global/event` not session-scoped `/event`. This means all sessions' events are received — filtering by session ID must be done in the app if needed.

2. **Event Type Extraction**: The `handleSseEvent` method handles both direct events and `global/event` payloads by checking if `type == "message"` and extracting the actual type from `payload.type`.

3. **No SSE in Retrofit Interface**: SSE endpoints are NOT in `KiloCodeApi.kt`. They use raw OkHttp calls via `ApiClient.createStreamCall()`.

4. **Model Injection**: `listModels()` injects `kilo-auto/free` `ModelOption` if absent from API response.

5. **Message ID Generation**: Optimistic messages use `msg_<UUID>` format. Server messages use their own IDs.

6. **Part Identification**: Parts without an `id` are matched by `type` and `text` for upsert.

## File Reference

| File | Lines | Purpose |
|------|-------|---------|
| `SessionRepository.kt` | 472 | Full SSE implementation |
| `ApiClient.kt` | 135 | OkHttp client, stream calls |
| `KiloCodeApi.kt` | 116 | REST API interface |
| `Models.kt` | 217 | All data classes |
| `AuthInterceptor.kt` | — | Auth header injection |
| `AuthPreferencesRepository.kt` | — | DataStore persistence |
