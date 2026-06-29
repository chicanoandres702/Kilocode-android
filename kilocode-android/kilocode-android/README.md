# Kilo Code Android

Native Android client for the Kilo Code AI coding assistant.

## Overview

This Android app connects to a running Kilo Code server (`kilo serve`) and provides a mobile-optimized interface for AI-powered coding assistance. The app communicates with the server via HTTP + SSE (Server-Sent Events) for real-time updates.

## Architecture

```
┌─────────────────────┐     HTTP + SSE      ┌─────────────────────┐
│   Android App       │ ◄──────────────────► │   Kilo Code Server  │
│   (Kotlin/Compose)  │                      │   (kilo serve)      │
│                     │                      │   Port 4096         │
│  - Session List     │                      │                     │
│  - Chat Interface   │                      │  - AI Agent Runtime │
│  - File Browser     │                      │  - Session Mgmt     │
│  - Settings         │                      │  - Provider Routing │
└─────────────────────┘                      └─────────────────────┘
```

## Features

- **Session Management** - Create, view, and delete coding sessions
- **Real-time Chat** - Communicate with AI agent with live updates via SSE
- **Tool Execution View** - See tool calls and their results in real-time
- **Message History** - Browse conversation history with parts (text, tool, reasoning)
- **Server Configuration** - Connect to any running Kilo Code server
- **Dark Theme** - Modern dark UI matching the Kilo Code aesthetic

## Requirements

- Android SDK 26+ (Android 8.0)
- A running Kilo Code server (`kilo serve`)

## Getting Started

### 1. Start the Kilo Code Server

```bash
# Install the CLI
npm install -g @kilocode/cli

# Start the server in your project directory
kilo serve --port 4096
```

### 2. Build the Android App

```bash
cd kilocode-android
./gradlew assembleDebug
```

### 3. Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Configure Server URL

In the app's Settings, set the server URL to your machine's IP address:

```
http://<your-machine-ip>:4096
```

For Android Emulator, use `http://10.0.2.2:4096` (default).

## Project Structure

```
app/src/main/java/com/kilocode/android/
├── KiloCodeApplication.kt      # Application class
├── MainActivity.kt              # Main entry point
├── data/
│   ├── api/
│   │   ├── ApiClient.kt         # Retrofit HTTP client
│   │   ├── KiloCodeApi.kt       # API interface definitions
│   │   └── SseEventListener.kt  # SSE event handling
│   ├── model/
│   │   └── Models.kt            # Data models
│   └── repository/
│       └── SessionRepository.kt # Session data management
├── ui/
│   ├── components/
│   │   ├── CommonComponents.kt  # Shared UI components
│   │   ├── MessageComponents.kt # Chat message rendering
│   │   ├── PromptInput.kt       # Text input component
│   │   └── SessionList.kt       # Session list UI
│   ├── navigation/
│   │   └── Navigation.kt        # Navigation graph
│   ├── screens/
│   │   ├── HomeScreen.kt        # Session list screen
│   │   ├── SessionScreen.kt     # Chat session screen
│   │   └── SettingsScreen.kt    # Settings screen
│   └── theme/
│       ├── Color.kt             # Color definitions
│       └── Theme.kt             # Material theme
```

## API Endpoints

The app communicates with the following Kilo Code server endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/session` | GET | List all sessions |
| `/session` | POST | Create new session |
| `/session/:id` | GET | Get session details |
| `/session/:id` | DELETE | Delete session |
| `/session/:id/message` | GET | List session messages |
| `/session/:id/message/:id/part` | GET | Get message parts |
| `/session/:id/prompt` | POST | Send a prompt |
| `/session/:id/abort` | POST | Abort current operation |
| `/session/status` | GET | Get session statuses |
| `/provider` | GET | List AI providers |
| `/config` | GET | Get configuration |

## Technology Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern declarative UI toolkit
- **Material 3** - Design system
- **Retrofit** - HTTP client
- **OkHttp** - Networking + SSE support
- **Gson** - JSON serialization
- **Navigation Compose** - Screen navigation
- **Coroutines** - Async programming
- **DataStore** - Preferences storage

## License

MIT License - See [LICENSE](../kilocode/LICENSE) for details.
