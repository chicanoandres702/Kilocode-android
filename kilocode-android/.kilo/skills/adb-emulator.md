---
name: adb-emulator
description: ADB emulator testing commands for the Kilocode Android app. Use when installing APKs, simulating touches, dumping UI hierarchy, taking screenshots, monitoring logs, or running UI automation against the emulator.
triggers:
  - emulator
  - adb
  - install apk
  - tap
  - touch
  - dump ui
  - uiautomator
  - screencap
  - logcat
---

# ADB Emulator Testing

## Device

All commands target `localhost:5555` — pass `-s localhost:5555` to every `adb` invocation.

## Commands

### Install APK

```bash
adb -s localhost:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch App

```bash
adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity
```

### Force Stop App

```bash
adb -s localhost:5555 shell am force-stop com.kilocode.android
```

### Dump UI Hierarchy (XML)

```bash
adb -s localhost:5555 shell uiautomator dump /sdcard/ui.xml && adb -s localhost:5555 pull /sdcard/ui.xml /tmp/ui.xml
```

Then read `/tmp/ui.xml` to inspect element bounds, text, and resource-ids.

### Take Screenshot

```bash
adb -s localhost:5555 exec-out screencap -p > /tmp/screenshot.png
```

### Simulate Tap

Given element bounds `[left,top][right,bottom]` from UI dump:

```bash
adb -s localhost:5555 shell input tap <x> <y>
```

Where `x = (left + right) // 2` and `y = (top + bottom) // 2`.

### Simulate Swipe

```bash
adb -s localhost:5555 shell input swipe <startX> <startY> <endX> <endY> <durationMs>
```

### Simulate Text Input

```bash
adb -s localhost:5555 shell input text "<text>"
```

### Press Key

```bash
adb -s localhost:5555 shell input keyevent <keycode>
```

Common keycodes: `KEYCODE_BACK = 4`, `KEYCODE_ENTER = 66`, `KEYCODE_HOME = 3`.

### Monitor Logcat

```bash
adb -s localhost:5555 logcat -s "AndroidRuntime:E" "System.err:W"
```

### Clear Logcat

```bash
adb -s localhost:5555 logcat -c
```

### Pull File from Device

```bash
adb -s localhost:5555 pull /sdcard/<path> /tmp/<dest>
```

### Push File to Device

```bash
adb -s localhost:5555 push /tmp/<src> /sdcard/<dest>
```

### List Installed Packages

```bash
adb -s localhost:5555 shell pm list packages | grep kilo
```

## Typical Workflow

1. Build: `./gradlew :app:assembleDebug`
2. Install: `adb -s localhost:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
3. Launch: `adb -s localhost:5555 shell am start -n com.kilocode.android/.MainActivity`
4. Wait 3 seconds for app to load
5. Dump UI: `adb -s localhost:5555 shell uiautomator dump /sdcard/ui.xml && adb -s localhost:5555 pull /sdcard/ui.xml /tmp/ui.xml`
6. Read `/tmp/ui.xml` to find target element bounds
7. Tap: `adb -s localhost:5555 shell input tap <x> <y>`
8. Monitor logs: `adb -s localhost:5555 logcat -s "AndroidRuntime:E" "System.err:W"`
