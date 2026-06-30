# Test Thoroughness Plan

## Overview
The user requested thorough testing of the application including unit tests and UI-based functional testing using adb, UI hierarchy dumps, and screenshots.

## Steps
1.  Execute existing unit tests (`AuthPreferencesRepositoryTest.kt`, `SessionRepositoryTest.kt`).
2.  Execute existing UI/instrumentation tests (`NavigationTest.kt`).
3.  Perform adb-based manual/smoke testing:
    - Install the application.
    - Launch application via adb.
    - Dump UI hierarchy (xml) using `uiautomator dump`.
    - Capture screenshot using `screencap`.
    - Perform basic touch interaction using `input tap`.
4.  Analyze test results and report findings.

## Execution
- Unit Tests: `./gradlew :app:testDebugUnitTest`
- UI Tests: `./gradlew :app:connectedDebugAndroidTest`
- Adb commands: `adb shell uiautomator dump`, `adb shell screencap`, `adb shell input tap x y`
