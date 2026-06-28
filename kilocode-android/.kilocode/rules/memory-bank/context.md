# Active Context: Kilo Code Android App

## Current State

**Status**: Android client fully functional. SSE streaming, message rendering, model selection, and prompt sending all work correctly. Server-side only `kilo-auto/free` model is operational; other models require provider API key configuration on the server. GitHub repo clone/reopen/create feature fully wired: selecting a repo navigates to HomeScreen with that repo as the working directory for session creation.

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
- 2026-06-20 | Added Android autonomous mode toggle persisted in Settings and passed to `kilo serve --auto` |
| 2026-06-20 | Added remote Kilo agent listing, agent selection in chat, prompt sending fix, and polished chat UI |
| 2026-06-24 | Created release 1.0.4 |
| 2026-06-25 | Fixed `Icons.Rounded.ArrowBack` deprecation and verified build/typecheck/lint |
| 2026-06-25 | Pivoted to SDK-based server interaction in Android |
| 2026-06-25 | Redesigned Android launcher icon to modern Kilo Code style |
| 2026-06-25 | Refactored `ApiClient` to SDK-like interface; implemented `onOptionSelected` callback in UI |
| 2026-06-25 | Updated default server URL to `http://18.191.142.105:4096` to resolve connection issues. |
| 2026-06-25 | Implemented `onOptionSelected` to send prompts in `QuestionToolView` |
| 2026-06-25 | Fixed `SessionRepository.kt` by removing duplicate `connectSse` implementation and cleaning up broken code block. |
| 2026-06-26 | Fixed SSE implementation to align with documented API: changed endpoint from `global/event` to `/event` (session-scoped), added `workspace` query param support, replaced non-existent `server.heartbeat` with `server.connected`, separated session busy state from connection state (`_sessionBusy`), added 30+ missing event handlers (session.idle, session.turn.*, session.diff, session.compacted, question.*, suggestion.*, todo.*, workspace.*, worktree.*, file.edited, provider.updated, lsp.*, mcp.*, background_process.*, indexing.*, command.executed, project.updated, kilocode.agent_manager.start, tui.*), and fixed event envelope parsing for both `/event` ({id, type, properties}) and `global/event` ({directory, project, workspace, payload}) formats. |
| 2026-06-26 | Investigated model availability: server only supports `kilo-auto/free`. All other models from `/api/model` return `ProviderModelNotFoundError` due to unconfigured provider API keys server-side. Free Models UI section is purely cosmetic and not the cause. Confirmed `prompt_async` endpoint works correctly with `kilo/kilo-auto/free`. |
| 2026-06-26 | Added folder browser with directory check and session scoping: FolderBrowser composable in HomeScreen, DirectoryCheckingIndicator, DirectoryNotFound with retry/go-root, checkDirectoryExists() in SessionRepository, loadAndCheckDirectory() in SessionViewModel, SessionList scoped to currentDirectory with DirectoryHeader. Verified on emulator — folder navigation, directory checking, up navigation, and session scoping all work correctly. |
| 2026-06-27 | Implemented GitHub repo clone/reopen feature: Next.js `POST /api/repo` route (clone/reopen actions via `gh` CLI), `GET /api/repo` for listing cloned repos, stored in `/tmp/kilo-repos`. Android: `RepoRepository`, `RepoScreen` with clone input + repo list UI, `cloneRepo`/`reopenRepo`/`listRepos` in `ApiClient`, new data models (`CloneRepoRequest`, `RepoOperationResponse`, `RepoEntry`, `RepoListResponse`). Added "Repositories" cloud icon to HomeScreen top bar navigating to RepoScreen. Build verified on emulator. |
| 2026-06-28 | Extended repo feature: added `create` action to backend, `RepoRepository.createRepo()`, dual Create/Clone buttons in RepoScreen. Wired repo selection to HomeScreen via `onRepoSelected: (String, String) -> Unit` (name, path). `KiloCodeNavHost` tracks `selectedRepoPath`, passes as `initialDirectory` to `HomeScreen`. Removed FolderBrowser/DirectoryNotFound from HomeScreen, replaced with path indicator. Fixed GitHub path separator bug (`_` not `/`). Moved Next.js from `src/app/` to `backend/` (Next.js 16 workspace root conflict). Dual-port: `DEFAULT_SERVER_URL` = port 4096, `API_SERVER_URL` = port 3001. All changes committed and pushed to `fix/29-fix-release-compile-error`. |
| 2026-06-28 | Fixed compaction UI and double-submit: suppress `compaction` parts in MessageComponents (no empty bubble), clear sessionBusy/isLoading on `session.compacted` SSE event, added `manualSendInProgress` flag to prevent autonomous "continue" loop from firing after manual prompt send. Built and installed on emulator. |
