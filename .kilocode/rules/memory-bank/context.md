# Active Context: Kilo Code Android App

## Current State

**Status**: Android client fully reviewed, fixed, and APK exported

## Recently Completed

- Fixed "typing twice" issue in `PromptInput.kt` by using `TextFieldValue` with `BasicTextField`.
- Implemented scrolling to the bottom upon initial load of `SessionScreen.kt` using a `LaunchedEffect` and `isFirstLoad` flag.
- [x] Fixed `Icons.Rounded.ArrowBack` deprecation in `SettingsScreen.kt` and `SessionScreen.kt` by using `Icons.AutoMirrored.Rounded.ArrowBack` and importing `androidx.compose.material.icons.automirrored.rounded.*`.
- [x] Verified build success with `./gradlew assembleDebug`.

### Current State

**Status**: Android client fully reviewed, fixed, and APK exported. Message rendering, animation, and lint deprecations resolved.

### Session History

| Date | Changes |
|------|---------|
| 2026-03-30 | Created Android client app |
| 2026-03-30 | Code review round 1 - fixed 18 issues |
| 2026-03-30 | Code review round 2 - fixed 32 issues, APK release build |
| 2026-06-18 | Updated Android CI workflow and committed the workflow file; push is blocked by missing GitHub HTTPS credentials |
| 2026-06-20 | Added `18.227.97.23` to Android network security cleartext domain allowlist and base cleartext config |
| 2026-06-20 | Fixed Android session opening against current Kilo server message/event API and added server URL persistence |
| 2026-06-20 | Added Android autonomous mode toggle persisted in Settings and passed to `kilo serve --auto` |
| 2026-06-20 | Added remote Kilo agent listing, agent selection in chat, prompt sending fix, and polished chat UI |
| 2026-06-24 | Created release 1.0.4 |
| 2026-06-25 | Fixed `Icons.Rounded.ArrowBack` deprecation and verified build/typecheck/lint |

