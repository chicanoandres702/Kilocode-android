---
description: Test agent persistence flow — select agent, kill app, relaunch, verify restoration
agent: android-dev
subtask: true
---
Test that agent selection persists across app restarts via DataStore.

## Steps

1. **Build and install** (if not already installed):
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:compileDebugKotlin
   ```
   If compilation fails, fix errors first. Then:
   ```bash
   adb -s localhost:5555 install -r /home/ubuntu/Kilocode-android/kilocode-android/app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Launch app**:
   ```bash
   adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity
   ```

3. **Wait 3 seconds** for app to load.

4. **Dump UI** to find the agent selector:
   ```bash
   adb -s localhost:5555 shell uiautomator dump /sdcard/ui.xml && adb -s localhost:5555 pull /sdcard/ui.xml /tmp/ui.xml
   ```
   Read `/tmp/ui.xml` and find the agent selector element bounds.

5. **Tap agent selector** and select a non-default agent (e.g., `claude-sonnet` if available).

6. **Force stop app**:
   ```bash
   adb -s localhost:5555 shell am force-stop com.kilocode.android
   ```

7. **Relaunch app**:
   ```bash
   adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity
   ```

8. **Wait 3 seconds**, then dump UI again:
   ```bash
   adb -s localhost:5555 shell uiautomator dump /sdcard/ui.xml && adb -s localhost:5555 pull /sdcard/ui.xml /tmp/ui2.xml
   ```
   Read `/tmp/ui2.xml` and verify the previously selected agent is still shown as selected.

9. **Report** — confirm whether agent persistence works or identify the failure.
