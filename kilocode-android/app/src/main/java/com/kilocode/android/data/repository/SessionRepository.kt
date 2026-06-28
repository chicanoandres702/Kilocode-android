package com.kilocode.android.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import okio.BufferedSource

class SessionRepository(private val apiClient: ApiClient) {
    private fun logD(tag: String, msg: String) {
        if (System.getProperty("java.vm.name")?.contains("Android") == true) Log.d(tag, msg) else println("D/$tag: $msg")
    }
    private fun logE(tag: String, msg: String, e: Throwable? = null) {
        if (System.getProperty("java.vm.name")?.contains("Android") == true) Log.e(tag, msg, e) else System.err.println("E/$tag: $msg ${e?.message ?: ""}")
    }
    private fun logW(tag: String, msg: String) {
        if (System.getProperty("java.vm.name")?.contains("Android") == true) Log.w(tag, msg) else println("W/$tag: $msg")
    }
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _parts = MutableStateFlow<Map<String, List<Part>>>(emptyMap())
    val parts: StateFlow<Map<String, List<Part>>> = _parts

    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents

    private val _models = MutableStateFlow<List<ModelOption>>(emptyList())
    val models: StateFlow<List<ModelOption>> = _models

    private val _selectedAgent = MutableStateFlow<Agent?>(null)
    val selectedAgent: StateFlow<Agent?> = _selectedAgent
    fun setSelectedAgent(agent: Agent?) { _selectedAgent.value = agent }

    private val _selectedModel = MutableStateFlow<ModelOption?>(null)
    val selectedModel: StateFlow<ModelOption?> = _selectedModel
    fun setSelectedModel(model: ModelOption?) { _selectedModel.value = model }

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project

    private val _files = MutableStateFlow<List<FileNode>>(emptyList())
    val files: StateFlow<List<FileNode>> = _files

    private val _folders = MutableStateFlow<List<FileNode>>(emptyList())
    val folders: StateFlow<List<FileNode>> = _folders

    private val _currentDirectory = MutableStateFlow("/")
    val currentDirectory: StateFlow<String> = _currentDirectory

    private val _isLoadingFolders = MutableStateFlow(false)
    val isLoadingFolders: StateFlow<Boolean> = _isLoadingFolders

    private val _directoryExists = MutableStateFlow<Boolean?>(null)
    val directoryExists: StateFlow<Boolean?> = _directoryExists

    private val _isCheckingDirectory = MutableStateFlow(false)
    val isCheckingDirectory: StateFlow<Boolean> = _isCheckingDirectory

     private val _isLoading = MutableStateFlow(false)
     val isLoading: StateFlow<Boolean> = _isLoading

     private val _isConnected = MutableStateFlow(false)
     val isConnected: StateFlow<Boolean> = _isConnected

     private val _sessionBusy = MutableStateFlow(false)
     val sessionBusy: StateFlow<Boolean> = _sessionBusy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var sseJob: Job? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun listSessions(directory: String? = null) {
        _isLoading.value = true
        try {
            val dir = directory ?: _currentDirectory.value
            val response = apiClient.api.listSessions(directory = dir)
            _sessions.value = response.body() ?: emptyList()
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading sessions", e)
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Check if a directory exists on the server by attempting to list files.
     * Sets _directoryExists to true/false based on the result.
     */
    suspend fun checkDirectoryExists(path: String) {
        _isCheckingDirectory.value = true
        _directoryExists.value = null
        try {
            val response = apiClient.api.listFiles(path = path)
            if (response.isSuccessful) {
                // Directory exists — also update folders state
                val files = response.body() ?: emptyList()
                _folders.value = files
                _currentDirectory.value = path
                _directoryExists.value = true
                logD("SessionRepo", "Directory exists: $path (${files.size} entries)")
            } else {
                logE("SessionRepo", "Directory check failed: ${response.code()} for $path")
                _directoryExists.value = false
                _error.value = "Directory not found: $path"
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error checking directory: $path", e)
            _directoryExists.value = false
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isCheckingDirectory.value = false
        }
    }

    suspend fun loadFolders(path: String) {
        _isLoadingFolders.value = true
        try {
            val response = apiClient.api.listFiles(path = path)
            if (response.isSuccessful) {
                _folders.value = response.body() ?: emptyList()
                _currentDirectory.value = path
                logD("SessionRepo", "Folders loaded: ${_folders.value.size} at $path")
            } else {
                logE("SessionRepo", "Failed to load folders: ${response.code()}")
                _error.value = "Failed to load folders: ${response.code()}"
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading folders", e)
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoadingFolders.value = false
        }
    }

    fun navigateToFolder(path: String) {
        _currentDirectory.value = path
    }

    fun navigateUp(): String? {
        val current = _currentDirectory.value
        if (current == "/" || current.isBlank()) return null
        val parent = current.trimEnd('/').substringBeforeLast('/')
        val parentPath = if (parent.isBlank()) "/" else parent
        _currentDirectory.value = parentPath
        return parentPath
    }

    suspend fun createSession(directory: String): Session? {
        return try {
            _isLoading.value = true
            val response = apiClient.api.createSession(directory = directory)
            if (response.isSuccessful) {
                val session = response.body()
                if (session != null) {
                    _sessions.value = listOf(session) + _sessions.value
                    _currentSession.value = session
                    session.id?.let { loadMessages(it) }
                }
                session
            } else {
                _error.value = "Failed to create session: ${response.code()}"
                null
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error creating session", e)
            _error.value = "Connection error: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun selectSession(sessionId: String) {
        logD("SessionRepo", "Selecting session: $sessionId")
        _isLoading.value = true
        try {
            _messages.value = emptyList()
            _parts.value = emptyMap()
            _error.value = null
            val existingDir = _currentSession.value?.directory
            val response = apiClient.api.getSession(sessionId, directory = existingDir)
            if (response.isSuccessful) {
                _currentSession.value = response.body()
                logD("SessionRepo", "Session selected, loading messages: $sessionId")
                loadMessages(sessionId)
            } else {
                logE("SessionRepo", "Failed to load session: ${response.code()}")
                _error.value = "Failed to load session: ${response.code()}"
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error selecting session", e)
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun listAgents(): List<Agent> {
        return try {
            val agents = apiClient.listAgents()
            _agents.value = agents
            logD("SessionRepo", "Agents loaded: ${agents.size}")
            agents
        } catch (e: Exception) {
            logE("SessionRepo", "Error listing agents", e)
            emptyList()
        }
    }

    suspend fun listModels(): List<ModelOption> {
        return try {
            val response = apiClient.api.listModels()
            val raw = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            val models = raw.map { m ->
                val free = m.modelID.endsWith(":free") || m.modelID.endsWith("/free")
                val cat = if (free) "Free Models" else (m.category ?: "Models")
                // Strip :free suffix from display name for cleaner UI
                val cleanName = m.displayName.removeSuffix(":free").removeSuffix("/free")
                m.copy(isFree = free, category = cat, displayName = cleanName)
            }
            // Sort: kilo-auto/free first (only working model), then other free, then by name
            val sorted = models.sortedWith(
                compareByDescending<ModelOption> { it.modelID == "kilo-auto/free" }
                    .thenByDescending<ModelOption> { it.isFree }
                    .thenBy { it.category }
                    .thenBy { it.displayName }
            )
            _models.value = sorted
            logD("SessionRepo", "Models loaded: ${sorted.size}, free: ${sorted.count { it.isFree }}")
            sorted
        } catch (e: Exception) {
            logE("SessionRepo", "Error listing models", e)
            emptyList()
        }
    }

    suspend fun sendPrompt(sessionId: String, prompt: String, agent: String? = null, model: ModelOption? = null, directory: String? = null) {
        val messageId = generateMessageId()
        // Optimistic update for user message
        val optimisticMessage = Message(id = messageId, sessionID = sessionId, role = "user", agent = agent)
        upsertMessage(optimisticMessage)
        _parts.update { current ->
            current + (messageId to listOf(Part(id = "part_${UUID.randomUUID()}", text = prompt, type = "text", messageID = messageId)))
        }
        _isLoading.value = true
        _sessionBusy.value = true
        try {
            val modelInfo = model?.let {
                ModelInfo(it.providerID, it.modelID)
            }
            logD("SessionRepo", "Sending async prompt to $sessionId: ${prompt.take(80)}")
            // Use prompt_async (fire-and-forget) — response comes via SSE events
            val response = apiClient.api.sendPrompt(
                sessionId,
                directory = directory,
                request = PromptRequest(
                    messageID = messageId,
                    parts = listOf(PartRequest(type = "text", text = prompt)),
                    agent = agent,
                    model = modelInfo
                )
            )
            if (response.isSuccessful) {
                logD("SessionRepo", "Async prompt accepted for $sessionId — waiting for SSE events")
                // Do NOT clear loading/busy here — SSE events (session.turn.close, session.idle) will do that
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                logE("SessionRepo", "Prompt failed: HTTP ${response.code()} $errorBody")
                _error.value = "Failed to send prompt: HTTP ${response.code()}"
                _isLoading.value = false
                _sessionBusy.value = false
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error sending prompt", e)
            _error.value = "Failed to send prompt: ${e.message}"
            _isLoading.value = false
            _sessionBusy.value = false
        }
    }

     fun abortSession(sessionId: String) {
         repositoryScope.launch {
             try {
                 logD("SessionRepo", "Aborting session: $sessionId")
                 val dir = _currentSession.value?.directory
                 apiClient.api.abortSession(sessionId, directory = dir)
                 // Clear loading state immediately for responsive UI
                 // SSE events will confirm the abort
                 _isLoading.value = false
                 _sessionBusy.value = false
             } catch (e: Exception) {
                 logE("SessionRepo", "Error aborting session", e)
                 _isLoading.value = false
                 _sessionBusy.value = false
             }
         }
     }

     fun compactSession(sessionId: String) {
         repositoryScope.launch {
             try {
                 logD("SessionRepo", "Compacting session: $sessionId")
                 val dir = _currentSession.value?.directory
                 val response = apiClient.api.compactSession(sessionId, directory = dir)
                 if (response.isSuccessful) {
                     logD("SessionRepo", "Session compacted: $sessionId, result=${response.body()}")
                 } else {
                     logE("SessionRepo", "Compact failed: ${response.code()}")
                     _error.value = "Failed to compact: ${response.code()}"
                 }
             } catch (e: Exception) {
                 logE("SessionRepo", "Error compacting session", e)
                 _error.value = "Failed to compact: ${e.message}"
             }
         }
     }

    suspend fun deleteSession(sessionId: String) {
        try {
            val dir = _currentSession.value?.directory
            apiClient.api.deleteSession(sessionId, directory = dir)
            _sessions.value = _sessions.value.filter { it.id != sessionId }
            if (_currentSession.value?.id == sessionId) {
                _currentSession.value = null
                _messages.value = emptyList()
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error deleting session", e)
            _error.value = "Failed to delete session: ${e.message}"
        }
    }

    private fun loadMessages(sessionId: String) {
        repositoryScope.launch {
            try {
                val dir = _currentSession.value?.directory
                val messagesWithParts = apiClient.getMessages(sessionId, directory = dir)
                _messages.value = messagesWithParts.mapNotNull { it.info }
                _parts.value = messagesWithParts
                    .mapNotNull { messageWithParts ->
                        val id = messageWithParts.info?.id ?: return@mapNotNull null
                        id to messageWithParts.parts
                    }
                    .toMap()
            } catch (e: Exception) {
                logE("SessionRepo", "Error loading messages", e)
                _error.value = "Connection error: ${e.message}"
            }
        }
    }

      fun connectSse(sessionId: String, directory: String? = null, workspace: String? = null) {
           logD("SessionRepo", "connectSse called for: $sessionId, directory: $directory, workspace: $workspace")
           disconnectSse()
           val dir = directory ?: "/"
           val ws = workspace ?: ""
           val encodedDirectory = URLEncoder.encode(dir, StandardCharsets.UTF_8.toString())
           val encodedWorkspace = URLEncoder.encode(ws, StandardCharsets.UTF_8.toString())
           
           val path = "event?directory=$encodedDirectory&workspace=$encodedWorkspace"
           logD("SessionRepo", "Connecting SSE to: ${apiClient.baseUrl}$path")
         sseJob = repositoryScope.launch {
             var retryDelay = 1000L
             val maxRetryDelay = 30000L
             while (isActive) {
                 try {
                     val call = apiClient.createStreamCall(path)
                     logD("SessionRepo", "SSE call created: ${call.request().url}")
                     call.execute().use { response ->
                         if (!response.isSuccessful) {
                             logE("SessionRepo", "SSE connection failed: ${response.code}")
                             if (response.code == 401 || response.code == 403) {
                                 // Auth errors — don't retry
                                 return@launch
                             }
                         } else {
                             _isConnected.value = true
                             retryDelay = 1000L // Reset on success
                             logD("SessionRepo", "SSE connected")
                             val source = response.body?.source() ?: return@launch
                             var currentType = "message"
                             
                             while (isActive && !source.exhausted()) {
                                 val line = source.readUtf8Line() ?: break
                                 if (line.isEmpty()) continue
                                 
                                 if (line.startsWith("event:")) {
                                     currentType = line.substringAfter("event:").trim()
                                 } else if (line.startsWith("data:")) {
                                     val data = line.substringAfter("data:").trim()
                                     handleSseEvent(currentType, data)
                                 }
                             }
                         }
                     }
                 } catch (e: Exception) {
                     if (isActive) {
                         logE("SessionRepo", "SSE failed", e)
                     }
                 }
                 
                 // Reconnection delay with exponential backoff
                 if (isActive) {
                     logD("SessionRepo", "SSE reconnecting in ${retryDelay}ms")
                     _isConnected.value = false
                     delay(retryDelay)
                     retryDelay = (retryDelay * 2).coerceAtMost(maxRetryDelay)
                 }
             }
         }
     }


     @Suppress("UNCHECKED_CAST")
     private fun handleSseEvent(type: String?, data: String) {
         logD("SessionRepo", "SSE event received: type=$type, data=$data")
         try {
             val event: Map<String, Any> = GSON.fromJson(data, MAP_TYPE)
             
             // The /event endpoint sends Event objects: { id, type, properties }
             // The /global/event endpoint sends GlobalEvent: { directory, project, workspace, payload: { id, type, properties } }
             // Determine which envelope we're dealing with.
             val hasDirectType = event.containsKey("type") && event["type"] is String
             val hasPayload = event.containsKey("payload") && event["payload"] is Map<*, *>
             
             val actualType: String?
             val properties: Map<String, Any>?
             
             if (hasDirectType) {
                 // Direct Event envelope { id, type, properties }
                 actualType = event["type"] as? String
                 properties = event["properties"] as? Map<String, Any>
             } else if (hasPayload) {
                 // GlobalEvent envelope { directory, project, workspace, payload: { ... } }
                 val payload = event["payload"] as? Map<String, Any>
                 actualType = payload?.get("type") as? String
                 properties = payload?.get("properties") as? Map<String, Any>
             } else {
                 actualType = type
                 properties = event["properties"] as? Map<String, Any>
             }
             
             if (actualType == null) {
                 logW("SessionRepo", "Skipping event: actualType=null")
                 return
             }
             
             logD("SessionRepo", "Processing event: actualType=$actualType")
             
             when (actualType) {
                 "server.connected" -> {
                     logD("SessionRepo", "Server connected")
                     _isConnected.value = true
                 }
                  "server.heartbeat" -> {
                      logD("SessionRepo", "Heartbeat received — connection alive")
                      _isConnected.value = true
                  }
                  "server.instance.disposed" -> {
                      logD("SessionRepo", "Server instance disposed")
                      _isConnected.value = false
                  }
                  "global.disposed" -> {
                      logD("SessionRepo", "Global disposed")
                      _isConnected.value = false
                  }
                 "message.updated" -> {
                     if (properties == null) return
                     val info = properties["info"] as? Map<String, Any> ?: return
                     upsertMessage(GSON.fromJson(GSON.toJsonTree(info), Message::class.java))
                 }
                 "message.removed" -> {
                     if (properties == null) return
                     val messageID = properties["messageID"] as? String ?: return
                     _messages.value = _messages.value.filter { it.id != messageID }
                 }
                  "message.part.updated" -> {
                      if (properties == null) return
                      val partData = properties["part"] as? Map<String, Any> ?: run {
                          logW("SessionRepo", "Skipping message.part.updated: 'part' property missing. properties=$properties")
                          return
                      }
                      val part = GSON.fromJson(GSON.toJsonTree(partData), Part::class.java)
                      val messageId = part.messageID ?: properties["messageID"] as? String
                      if (messageId == null) {
                          logW("SessionRepo", "Skipping part update: messageID missing.")
                          return
                      }
                      upsertPart(messageId, part)
                  }
                  "message.part.delta" -> {
                      // Server streams text via delta events — append incremental text to the part
                      if (properties == null) return
                      val messageId = properties["messageID"] as? String ?: return
                      val partId = properties["partID"] as? String ?: return
                      val field = properties["field"] as? String
                      val delta = properties["delta"] as? String
                      if (delta == null) return
                      appendPartDelta(messageId, partId, field, delta)
                  }
                 "message.part.removed" -> {
                     if (properties == null) return
                     val messageId = properties["messageID"] as? String ?: return
                     val partId = properties["partID"] as? String ?: return
                     removePart(messageId, partId)
                 }
                 "session.updated" -> {
                     if (properties == null) return
                     val session = properties["session"] as? Map<String, Any> ?: return
                     val updated = GSON.fromJson(GSON.toJsonTree(session), Session::class.java)
                     _currentSession.value = updated
                     _sessions.value = _sessions.value.map {
                         if (it.id == updated.id) updated else it
                     }
                 }
                 "session.created" -> {
                     if (properties == null) return
                     val session = properties["session"] as? Map<String, Any> ?: return
                     val created = GSON.fromJson(GSON.toJsonTree(session), Session::class.java)
                     if (_sessions.value.none { it.id == created.id }) {
                         _sessions.value = _sessions.value + created
                     }
                 }
                 "session.deleted" -> {
                     if (properties == null) return
                     val sessionID = properties["sessionID"] as? String ?: return
                     _sessions.value = _sessions.value.filter { it.id != sessionID }
                     if (_currentSession.value?.id == sessionID) {
                         _currentSession.value = null
                         _messages.value = emptyList()
                     }
                 }
                 "session.status" -> {
                     if (properties == null) return
                     val status = properties["status"] as? Map<String, Any>
                     val statusType = status?.get("type") as? String
                     // Only track session busy/idle state, NOT connection state
                     _sessionBusy.value = statusType != "idle"
                 }
                 "session.idle" -> {
                     logD("SessionRepo", "Session idle")
                     _sessionBusy.value = false
                     _isLoading.value = false
                 }
                  "session.error" -> {
                      if (properties == null) return
                      val error = properties["error"] as? Map<String, Any>
                      _error.value = error?.let {
                          val name = it["name"] as? String ?: "Session error"
                          val errorData = it["data"] as? Map<String, Any>
                          val message = errorData?.get("message") as? String
                          "$name${message?.let { m -> ": $m" }.orEmpty()}"
                      }
                      // Clear loading state so the UI isn't stuck after an error
                      _sessionBusy.value = false
                      _isLoading.value = false
                  }
                 "session.turn.open" -> {
                     logD("SessionRepo", "Session turn started")
                     _sessionBusy.value = true
                 }
                 "session.turn.close" -> {
                     logD("SessionRepo", "Session turn ended")
                     _sessionBusy.value = false
                     _isLoading.value = false
                 }
                 "session.diff" -> {
                     logD("SessionRepo", "Session diff received: $properties")
                     // Diff events carry incremental updates; handled by message.part events
                 }
                   "session.compacted" -> {
                       logD("SessionRepo", "Session compacted")
                       // Compaction complete — clear busy/loading state so the UI doesn't get stuck
                       // and don't trigger autonomous "continue" since this was a manual action
                       _sessionBusy.value = false
                       _isLoading.value = false
                   }
                  "session.next.model.switched" -> {
                      if (properties == null) return
                      val model = properties["model"] as? String
                      logD("SessionRepo", "Session model switched to: $model")
                  }
                  "permission.asked" -> {
                     logD("SessionRepo", "Permission asked: $properties")
                 }
                 "permission.replied" -> {
                     logD("SessionRepo", "Permission replied: $properties")
                 }
                 "question.asked" -> {
                     logD("SessionRepo", "Question asked: $properties")
                 }
                 "question.replied" -> {
                     logD("SessionRepo", "Question replied: $properties")
                 }
                 "question.rejected" -> {
                     logD("SessionRepo", "Question rejected: $properties")
                 }
                 "suggestion.shown" -> {
                     logD("SessionRepo", "Suggestion shown: $properties")
                 }
                 "suggestion.accepted" -> {
                     logD("SessionRepo", "Suggestion accepted: $properties")
                 }
                 "suggestion.dismissed" -> {
                     logD("SessionRepo", "Suggestion dismissed: $properties")
                 }
                 "todo.updated" -> {
                     logD("SessionRepo", "Todo updated: $properties")
                 }
                 "workspace.status" -> {
                     logD("SessionRepo", "Workspace status: $properties")
                 }
                 "workspace.ready" -> {
                     logD("SessionRepo", "Workspace ready")
                 }
                 "workspace.failed" -> {
                     logD("SessionRepo", "Workspace failed: $properties")
                 }
                 "worktree.ready" -> {
                     logD("SessionRepo", "Worktree ready")
                 }
                 "worktree.failed" -> {
                     logD("SessionRepo", "Worktree failed: $properties")
                 }
                 "file.edited" -> {
                     logD("SessionRepo", "File edited: $properties")
                 }
                 "provider.updated" -> {
                     logD("SessionRepo", "Provider updated")
                 }
                 "installation.updated" -> {
                     logD("SessionRepo", "Installation updated")
                 }
                 "lsp.client.diagnostics" -> {
                     logD("SessionRepo", "LSP diagnostics: $properties")
                 }
                 "lsp.updated" -> {
                     logD("SessionRepo", "LSP updated")
                 }
                 "mcp.tools.changed" -> {
                     logD("SessionRepo", "MCP tools changed")
                 }
                 "mcp.browser.open.failed" -> {
                     logD("SessionRepo", "MCP browser open failed: $properties")
                 }
                 "background_process.updated" -> {
                     logD("SessionRepo", "Background process updated: $properties")
                 }
                 "background_process.deleted" -> {
                     logD("SessionRepo", "Background process deleted: $properties")
                 }
                 "indexing.status" -> {
                     logD("SessionRepo", "Indexing status: $properties")
                 }
                 "indexing.warning" -> {
                     logD("SessionRepo", "Indexing warning: $properties")
                 }
                 "command.executed" -> {
                     logD("SessionRepo", "Command executed: $properties")
                 }
                 "project.updated" -> {
                     logD("SessionRepo", "Project updated")
                 }
                 "kilocode.agent_manager.start" -> {
                     logD("SessionRepo", "Agent manager started")
                 }
                 "tui.prompt.append" -> {
                     logD("SessionRepo", "TUI prompt append: $properties")
                 }
                 "tui.command.execute" -> {
                     logD("SessionRepo", "TUI command execute: $properties")
                 }
                 "tui.toast.show" -> {
                     logD("SessionRepo", "TUI toast show: $properties")
                 }
                 "tui.session.select" -> {
                     logD("SessionRepo", "TUI session select: $properties")
                 }
                 else -> {
                     logD("SessionRepo", "Unknown event type: $actualType")
                 }
             }
         } catch (e: Exception) {
             logE("SessionRepo", "Error handling SSE event", e)
         }
     }

    private fun upsertMessage(message: Message) {
        logD("SessionRepo", "upsertMessage: $message")
        val messageId = message.id ?: generateMessageId()
        val messageWithId = message.copy(id = messageId)
        _messages.update { current ->
            val mutableList = current.toMutableList()
            val index = mutableList.indexOfFirst { it.id == messageId }
            if (index >= 0) mutableList[index] = messageWithId else mutableList.add(messageWithId)
            mutableList
        }
        logD("SessionRepo", "messages updated: ${_messages.value.size}")
    }

    private fun upsertPart(messageId: String, part: Part) {
        logD("SessionRepo", "upsertPart: messageId=$messageId, part=$part")
        _parts.update { currentParts ->
            val messageParts = currentParts[messageId]?.toMutableList() ?: mutableListOf()
            // Find existing part by id, or by other criteria if id is null
            val index = if (part.id != null) {
                messageParts.indexOfFirst { it.id == part.id }
            } else {
                messageParts.indexOfFirst { it.type == part.type && it.text == part.text }
            }
            if (index >= 0) messageParts[index] = part else messageParts.add(part)
            currentParts + (messageId to messageParts)
        }
        logD("SessionRepo", "parts updated for $messageId: ${_parts.value[messageId]?.size}")
    }

    private fun removePart(messageId: String, partId: String) {
        val currentParts = _parts.value.toMutableMap()
        currentParts[messageId] = currentParts[messageId]?.filter { it.id != partId }.orEmpty()
        _parts.value = currentParts
    }

    /**
     * Append a text delta from SSE `message.part.delta` events.
     * This is the primary streaming mechanism — the server sends incremental text fragments
     * as the model generates tokens.
     */
    private fun appendPartDelta(messageId: String, partId: String, field: String?, delta: String) {
        _parts.update { currentParts ->
            val messageParts = currentParts[messageId]?.toMutableList() ?: mutableListOf()
            val existingIndex = messageParts.indexOfFirst { it.id == partId }

            if (existingIndex >= 0) {
                // Append delta to existing part
                val existing = messageParts[existingIndex]
                val updated = when (field) {
                    "text" -> existing.copy(text = (existing.text ?: "") + delta)
                    else -> existing // Ignore unknown field deltas
                }
                messageParts[existingIndex] = updated
            } else {
                // Part doesn't exist yet — create a placeholder part.
                // The full part will arrive later via message.part.updated.
                val newPart = when (field) {
                    "text" -> Part(id = partId, messageID = messageId, type = "text", text = delta)
                    else -> Part(id = partId, messageID = messageId, type = field)
                }
                messageParts.add(newPart)
            }

            currentParts + (messageId to messageParts)
        }

        // Ensure a message entry exists for this messageId so the UI can render the parts.
        // Delta events may arrive before the message.updated event creates the message entry.
        if (_messages.value.none { it.id == messageId }) {
            upsertMessage(Message(id = messageId, role = "assistant"))
        }
    }

     fun disconnectSse() {
         sseJob?.cancel()
         sseJob = null
         _isConnected.value = false
         _sessionBusy.value = false
     }

    fun clearError() {
        _error.value = null
    }

    private fun generateMessageId(): String = "msg_${UUID.randomUUID()}"

    companion object {
        private val GSON = Gson()
        private val MAP_TYPE = object : TypeToken<Map<String, Any>>() {}.type
    }
}
