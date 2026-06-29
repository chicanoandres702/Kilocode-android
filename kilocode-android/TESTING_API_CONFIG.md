# Testing Plan: API Server URL Configuration

## Objective
Verify that the Android application can successfully configure, persist, and use a custom API Server URL for connecting to the Next.js planning backend.

## Test Cases

### 1. Settings UI Functionality
- **Action**: Navigate to `SettingsScreen`.
- **Expected**: An `OutlinedTextField` is visible, labeled "API Server URL", and pre-populated with the current value (or default).
- **Action**: Enter a new URL (e.g., `http://10.0.2.2:3001`).
- **Action**: Tap "Save".
- **Expected**: Settings UI confirms save, or navigates back.

### 2. Persistence Verification
- **Action**: Close and reopen the app.
- **Action**: Navigate back to `SettingsScreen`.
- **Expected**: The "API Server URL" field shows the previously saved URL.

### 3. Backend Connectivity
- **Action**: Start the Next.js backend on the host machine (`next start -p 3001` or similar).
- **Action**: Ensure emulator can reach `10.0.2.2:3001`.
- **Action**: In `SettingsScreen`, update the API Server URL to `http://10.0.2.2:3001`.
- **Action**: Perform a task that triggers a network request to the Planning API (e.g., loading milestones).
- **Expected**: Application successfully retrieves and displays data from the backend.

### 4. Default Value Verification
- **Action**: Clear app data/cache (fresh install).
- **Action**: Navigate to `SettingsScreen`.
- **Expected**: The "API Server URL" field defaults to `http://10.0.2.2:3001` as configured in `build.gradle.kts`.

## Automation
- Manual testing is currently required as instrumented tests for DataStore are pending implementation.
