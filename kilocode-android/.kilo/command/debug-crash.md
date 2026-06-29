---
description: Debug a crash or runtime error on the Android emulator
agent: android-dev
subtask: true
---
Debug a crash or runtime error on the Android emulator. Use $ARGUMENTS to describe the symptom or paste stack trace.

## Steps

1. **Clear logcat** to get fresh logs:
   ```bash
   adb -s localhost:5555 logcat -c
   ```

2. **Reproduce the issue** — launch the app and perform the action that triggers the crash:
   ```bash
   adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity
   ```

3. **Capture crash logs**:
   ```bash
   adb -s localhost:5555 logcat -s "AndroidRuntime:E" "System.err:W" "FATAL:E"
   ```

4. **Analyze the stack trace** — identify:
   - Exception type (NullPointerException, IllegalStateException, etc.)
   - File and line number in the stack trace
   - Root cause (missing DataStore read, wrong StateFlow scope, etc.)

5. **Read the source file** at the identified line.

6. **Fix the bug** following project patterns.

7. **Compile check**:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:compileDebugKotlin
   ```

8. **Rebuild, install, verify**:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:assembleDebug
   adb -s localhost:5555 install -r app/build/outputs/apk/debug/app-debug.apk
   adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity
   ```

9. **Confirm fix** — verify crash no longer occurs in logcat.

10. **Commit** with message: `Fixes: #<issue> $ARGUMENTS`
