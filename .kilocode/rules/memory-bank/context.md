# Active Context: Kilo Code Android App

## Current State

**Status**: Android client fully functional. Sessions list, prompting mechanism, and compaction feature verified for functionality. Emulator environment configured and verified; app successfully built, installed, and launched.

## Recently Completed

- [x] Fixed "typing twice" issue in `PromptInput.kt` by using `TextFieldValue` with `BasicTextField`.
- [x] Implemented scrolling to the bottom upon initial load of `SessionScreen.kt` using a `LaunchedEffect` and `isFirstLoad` flag.
- [x] Fixed `MessageBubble` visibility issue by correctly defining `bubbleBg` in `MessageComponents.kt`
- [x] Fixed `Icons.Rounded.ArrowBack` deprecation in `SettingsScreen.kt` and `SessionScreen.kt` by using `Icons.AutoMirrored.Rounded.ArrowBack` and importing `androidx.compose.material.icons.automirrored.rounded.*`.
- [x] Verified build success with `./gradlew assembleDebug`.
- [x] Implemented `onOptionSelected` to send prompts in `QuestionToolView` via `SessionScreen`.
- [x] Implemented Stop button and reset autonomous mode on session switch.
- [x] Fixed message rendering issue in `SessionRepository.kt` by ensuring unique ID generation.
- [x] Fixed Android build syntax errors in `MessageComponents.kt` (extra braces and `BoxScope` issue).
- [x] Confirmed user intent to use Android SDK for server interaction.
- [x] Added folder browser with directory check and session scoping to HomeScreen. FolderBrowser composable navigates directories, DirectoryCheckingIndicator shows loading state, DirectoryNotFound shows error with retry/go-root options. SessionRepository.checkDirectoryExists() verifies directory before loading sessions. SessionViewModel.loadAndCheckDirectory() orchestrates the flow. SessionList scoped to currentDirectory with DirectoryHeader.
- [x] Verified `SessionScreen` session list rendering and prompting input integration.
- [x] Set up and verified emulator environment (redroid).
- [x] Built, installed, and verified application on emulator.
- [x] **Fixed 404 in planning mode** - Split `api/planning` endpoint into separate paths: `api/planning/milestones` and `api/planning/issues` to avoid Retrofit method collision. Updated both backend (Next.js routes) and Android API interface.
- [x] Added `PlanningRepositoryTest` unit tests.

### Current State

**Status**: Android client fully functional. Sessions list, prompting mechanism, and compaction feature verified for functionality. UI automation scripts working.

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
| 2026-06-25 | Pivoted to SDK-based server interaction in Android |
| 2026-06-25 | Redesigned Android launcher icon to modern Kilo Code style |
| 2026-06-25 | Refactored `ApiClient` to SDK-like interface; implemented `onOptionSelected` callback in UI |
| 2026-06-25 | Updated default server URL to `http://18.191.142.105:4096` to resolve connection issues. |
| 2026-06-25 | Implemented `onOptionSelected` to send prompts in `QuestionToolView` |
| 2026-06-25 | Fixed `SessionRepository.kt` by removing duplicate `connectSse` implementation and cleaning up broken code block. |
| 2026-06-26 | Fixed SSE implementation to align with documented API |
| 2026-06-26 | Investigated model availability |
| 2026-06-26 | Added folder browser with directory check and session scoping to HomeScreen. FolderBrowser composable navigates directories, DirectoryCheckingIndicator shows loading state, DirectoryNotFound shows error with retry/go-root options. SessionRepository.checkDirectoryExists() verifies directory before loading sessions. SessionViewModel.loadAndCheckDirectory() orchestrates the flow. SessionList scoped to currentDirectory with DirectoryHeader. |
| 2026-06-27 | Implemented GitHub repo clone/reopen feature |
| 2026-06-28 | Added Stop button during generation, WorkManager for background prompt execution |
| 2026-06-29 | Forced API Server URL to `http://18.191.142.105:3001` and disabled user settings override for planning API URL. |
| 2026-06-28 | Implemented planning milestones/issues feature |
| 2026-06-29 | Verified session list display and prompting in `SessionScreen.kt`. |
| 2026-06-29 | Updated "Built with" field in Settings from "Kilo Code · Anthropic" to "Kilo Code". |
| 2026-06-30 | Calibrated UI coordinates, tested interaction, setup permutation framework. |
| 2026-07-01 | Fixed 404 in planning mode by splitting planning API endpoints; added unit tests for PlanningRepository. |

