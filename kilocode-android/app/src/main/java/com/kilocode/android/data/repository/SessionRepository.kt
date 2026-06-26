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

    suspend fun listSessions() {
        _isLoading.value = true
        try {
            val response = apiClient.api.listSessions()
            _sessions.value = response.body() ?: emptyList()
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading sessions", e)
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createSession(directory: String): Session? {
        return try {
            _isLoading.value = true
            val response = apiClient.api.createSession(mapOf("directory" to directory))
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
            val response = apiClient.api.getSession(sessionId)
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
            val apiModels = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            val injected = ModelOption(
                providerID = "kilo-auto",
                modelID = "free",
                displayName = "Kilo Auto (Free)",
                category = "local"
            )
            val models = if (apiModels.any { it.providerID == "kilo-auto" && it.modelID == "free" }) apiModels
            else apiModels + injected
            _models.value = models
            logD("SessionRepo", "Models loaded: ${models.size}")
            models
        } catch (e: Exception) {
            logE("SessionRepo", "Error listing models", e)
            emptyList()
        }
    }

    suspend fun sendPrompt(sessionId: String, prompt: String, agent: String? = null, model: ModelOption? = null): MessageWithParts? {
        val messageId = generateMessageId()
        // Optimistic update
        val optimisticMessage = Message(id = messageId, sessionID = sessionId, role = "user", agent = agent)
        upsertMessage(optimisticMessage)
        _parts.update { current ->
            current + (messageId to listOf(Part(id = "part_${UUID.randomUUID()}", text = prompt, type = "text", messageID = messageId)))
        }
        _isLoading.value = true
        return try {
            val modelInfo = model?.let { ModelInfo(it.providerID, it.modelID) }
            val result = apiClient.sendPrompt(sessionId, prompt, agent, modelInfo)
            result
        } catch (e: Exception) {
            logE("SessionRepo", "Error sending prompt", e)
            _error.value = "Failed to send prompt: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }

     fun abortSession(sessionId: String) {
         repositoryScope.launch {
             try {
                 apiClient.api.abortSession(sessionId)
             } catch (e: Exception) {
                 logE("SessionRepo", "Error aborting session", e)
             }
         }
     }

     fun compactSession(sessionId: String) {
         repositoryScope.launch {
             try {
                 logD("SessionRepo", "Compacting session: $sessionId")
                 val response = apiClient.api.compactSession(sessionId)
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
            apiClient.api.deleteSession(sessionId)
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
                val messagesWithParts = apiClient.getMessages(sessionId)
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
         val encodedDirectory = URLEncoder.encode(directory ?: "", StandardCharsets.UTF_8.toString())
         val encodedWorkspace = URLEncoder.encode(workspace ?: "", StandardCharsets.UTF_8.toString())
         
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
                     // Compaction complete — messages list may have changed
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
