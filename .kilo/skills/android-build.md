---
name: android-build
description: Build and compile commands for the Kilocode Android project. Use when building debug APKs, running Kotlin compilation checks, linting, or cleaning the Gradle build.
triggers:
  - build
  - compile
  - assembleDebug
  - compileDebugKotlin
  - lint
  - gradle
  - clean
---

# Android Build Commands

## Project Root

All Gradle commands run from the `kilocode-android/` subdirectory:

```bash
cd /home/ubuntu/Kilocode-android/kilocode-android
```

## Commands

### Kotlin Compilation Check (fast)

```bash
cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:compileDebugKotlin
```

Use this to verify code compiles without producing a full APK. Fastest feedback loop.

### Build Debug APK

```bash
cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:assembleDebug
```

Produces `app/build/outputs/apk/debug/app-debug.apk`.

### Run Lint

```bash
cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew :app:lint
```

### Clean Build

```bash
cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew clean
```

### Full Clean + Build

```bash
cd /home/ubuntu/Kilocode-android/kilocode-android && ./gradlew clean :app:assembleDebug
```

## Verification Workflow

After every code change:

1. Run `./gradlew :app:compileDebugKotlin` — must pass before building APK
2. If compilation passes, run `./gradlew :app:assembleDebug`
3. Install on emulator: `adb -s localhost:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
4. Launch and verify via UI dump
