# Product Context: Kilo Code Android App

## Why This App Exists

Mobile client for the Kilo Code AI-assisted development platform, enabling developers to interact with AI assistants, manage coding sessions, and handle autonomous planning workflows directly from Android devices.

## Problems It Solves

1. **Mobile Access**: Access AI-assisted coding from anywhere on mobile
2. **Session Management**: Seamless session creation and continuation
3. **Planning Automation**: AI-powered feature generation and task breakdown
4. **Background Execution**: Reliable task tracking via WorkManager
5. **Branch Management**: Automated git branch handling for autonomous workflows

## How It Should Work (User Flow)

1. User opens app and connects to Kilo Code server
2. User creates or selects a session
3. User can:
   - Chat with AI assistant via SSE streaming
   - Enter Planning Wizard for autonomous feature generation
   - Generate features via AI with agent selection
   - Review and refine generated tasks
   - Monitor background tasks in Task Manager
4. App handles branch creation automatically for generated features

## Key User Experience Goals

- **Zero-friction Start**: Connect to server and start coding immediately
- **AI-Powered Planning**: Generate features from project descriptions
- **Reliable Background Work**: Tasks persist and track via WorkManager
- **Clean Navigation**: Intuitive navigation between sessions, planning, and tasks

## What This App Provides

1. **Session Management**: SSE-based chat with AI assistants
2. **Planning Wizard**: AI-powered feature generation with task breakdown
3. **Task Manager**: View, cancel, retry, and delete background tasks
4. **Branch Management**: Automatic branch naming and creation
5. **Agent Selection**: Choose different AI agents for different tasks

## Integration Points

- **Backend API**: RESTful endpoints for planning and session management
- **SSE Streaming**: Real-time AI responses
- **WorkManager**: Background task execution and tracking
- **Git**: Branch management via backend API