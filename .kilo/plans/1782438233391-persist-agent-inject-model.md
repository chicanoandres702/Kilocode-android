# Plan: Persist Selected Agent + Inject `kilo-auto/free` Model

## Context

The Android app currently:
- Has no persistence for the selected agent — it resets to default (first agent with `mode == "primary"` or `"all"`) on every app restart
- Hardcodes the default model selection to `kilo/nex-agi/nex-n2-pro:free` via string match in `SessionScreen.kt`
- Does not inject `kilo-auto/free` into the models list — it only exists if the server returns it

## Changes

### 1. Add agent persistence to `AuthPreferencesRepository`

**File:** `app/src/main/java/com/kilocode/android/data/repository/AuthPreferencesRepository.kt`

- Add `SELECTED_AGENT_NAME_KEY = stringPreferencesKey("selected_agent_name")`
- Add `selectedAgentNameFlow: Flow<String?>`
- Add `suspend fun saveSelectedAgentName(name: String?)`

This follows the existing DataStore pattern already in the file.

### 2. Inject `kilo-auto/free` into models list in `SessionRepository`

**File:** `app/src/main/java/com/kilocode/android/data/repository/SessionRepository.kt`

- In `listModels()`, after fetching models from API, check if any model has `modelID == "kilo-auto/free"` (or `providerID == "kilo-auto"`)
- If not present, append a synthetic `ModelOption(providerID = "kilo-auto", modelID = "free", displayName = "Kilo Auto (Free)", category = "Models")` to the returned list
- This ensures the model always appears in the dropdown even if the server doesn't return it

### 3. Restore selected agent on launch in `SessionScreen`

**File:** `app/src/main/java/com/kilocode/android/ui/screens/SessionScreen.kt`

- Accept an `AgentPreferencesRepository` (or the existing `AuthPreferencesRepository`) via the `SessionScreen` composable parameters — or obtain it from `LocalContext.current` inside the composable
- In the `LaunchedEffect(agents)` block, before falling back to default agent selection, read the persisted agent name from DataStore and attempt to find a match in the loaded agents list by `name`
- If found, set it as selected; otherwise fall back to existing default logic
- When `onAgentSelected` is called, persist the agent name (or `null` for default) to DataStore

### 4. Wire agent persistence into `PromptInput` callbacks

**File:** `app/src/main/java/com/kilocode/android/ui/screens/SessionScreen.kt`

- Wrap the `onAgentSelected = { repository.setSelectedAgent(it) }` callback to also persist the agent name via a coroutine scope launch into DataStore
- The `onAgentSelected` in `PromptInput` stays unchanged — persistence happens at the `SessionScreen` level where we have access to both the repository and the DataStore

### 5. Update default model selection to prefer `kilo-auto/free`

**File:** `app/src/main/java/com/kilocode/android/ui/screens/SessionScreen.kt`

- Change the `LaunchedEffect(models)` block to prefer `kilo-auto/free` as the default model instead of `kilo/nex-agi/nex-n2-pro:free`
- This ensures that after the injection in step 2, the auto model is pre-selected on first launch

## Files Modified

1. `app/src/main/java/com/kilocode/android/data/repository/AuthPreferencesRepository.kt` — add agent name persistence
2. `app/src/main/java/com/kilocode/android/data/repository/SessionRepository.kt` — inject `kilo-auto/free` into models list
3. `app/src/main/java/com/kilocode/android/ui/screens/SessionScreen.kt` — restore agent on launch, persist on selection, change default model

## Verification

- `bun typecheck` (or `./gradlew compileDebugKotlin` if available in sandbox)
- `bun lint` (or `./gradlew lint`)
- Manual: select an agent → kill app → reopen → verify agent chip shows the previously selected agent
- Manual: verify `Kilo Auto (Free)` appears in model dropdown even when server doesn't return it
