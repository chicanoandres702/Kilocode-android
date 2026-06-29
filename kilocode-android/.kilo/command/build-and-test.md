---
description: Build the Android app, install on emulator, and verify UI
agent: android-dev
subtask: true
---
Build the Kilocode Android app, install it on the emulator, launch it, and verify the UI via uiautomator dump.

Use $ARGUMENTS to pass additional Gradle flags or a specific test scenario.

## Steps

1. **Compile check** — verify Kotlin compiles:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:compileDebugKotlin
   ```
   If compilation fails, read the error, fix the source, and re-run. Do NOT proceed until this passes.

2. **Build debug APK**:
   ```bash
   cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:assembleDebug $ARGUMENTS
   ```

3. **Install on emulator**:
   ```bash
   adb -s localhost:5555 install -r /home/ubuntu/Kilocode-android/kilocode-android/app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Launch app**:
   ```bash
   adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity
   ```

5. **Wait for load** — wait 3 seconds for the app to fully render.

6. **Dump UI hierarchy**:
   ```bash
   adb -s localhost:5555 shell uiautomator dump /sdcard/ui.xml && adb -s localhost:5555 pull /sdcard/ui.xml /tmp/ui.xml
   ```
   Then read `/tmp/ui.xml` to verify expected elements are present.

7. **Monitor logs** — check for crashes:
   ```bash
   adb -s localhost:5555 logcat -s "AndroidRuntime:E" "System.err:W"
   ```

8. **Report** — summarize: build result, install success, UI elements found, any crashes in logcat.
