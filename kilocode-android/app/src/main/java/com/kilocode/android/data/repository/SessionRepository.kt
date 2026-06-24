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

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project

    private val _files = MutableStateFlow<List<FileNode>>(emptyList())
    val files: StateFlow<List<FileNode>> = _files

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

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
        try {
            _messages.value = emptyList()
            _parts.value = emptyMap()
            _error.value = null
            val response = apiClient.api.getSession(sessionId)
            if (response.isSuccessful) {
                _currentSession.value = response.body()
                loadMessages(sessionId)
            } else {
                _error.value = "Failed to load session: ${response.code()}"
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error selecting session", e)
            _error.value = "Connection error: ${e.message}"
        }
    }

    suspend fun loadMessages(sessionId: String) {
        try {
            val response = apiClient.api.listMessages(sessionId)
            if (response.isSuccessful) {
                val messagesWithParts = response.body() ?: emptyList()
                _messages.value = messagesWithParts.mapNotNull { it.info }
                _parts.value = messagesWithParts
                    .mapNotNull { messageWithParts -> messageWithParts.info?.id?.let { it to messageWithParts.parts } }
                    .toMap()
            } else {
                _error.value = "Failed to load messages: ${response.code()}"
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading messages", e)
            _error.value = "Connection error: ${e.message}"
        }
    }

    suspend fun listAgents() {
        try {
            val response = apiClient.api.listAgents()
            _agents.value = response.takeIf { it.isSuccessful }?.body().orEmpty()
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading agents", e)
        }
    }

    suspend fun listModels() {
        try {
            val response = apiClient.api.listProviders()
            val providerResponse = response.takeIf { it.isSuccessful }?.body()
            val connectedProviders = providerResponse?.connected.orEmpty()
            val providers = providerResponse?.all.orEmpty()
                .filter { connectedProviders.isEmpty() || it.id in connectedProviders }
            val options = buildList {
                providers.forEach { provider ->
                    provider.models.values.forEach { model ->
                        add(
                            ModelOption(
                                providerID = provider.id,
                                modelID = model.id,
                                displayName = model.name.ifBlank { model.id },
                                category = provider.name.ifBlank { provider.id },
                            )
                        )
                    }
                }
                if (isEmpty()) {
                    apiClient.api.getConfig().takeIf { it.isSuccessful }?.body()?.models.orEmpty().forEach { id ->
                        add(ModelOption("default", id, id, "Default"))
                    }
                }
            }
            _models.value = options.sortedWith(compareBy<ModelOption> { it.category }.thenBy { it.displayName })
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading models", e)
        }
    }

    suspend fun loadProject() {
        try {
            val response = apiClient.api.getProject()
            _project.value = response.takeIf { it.isSuccessful }?.body()
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading project", e)
        }
    }

    suspend fun listFiles(directory: String? = null) {
        try {
            val response = apiClient.api.listFiles(directory)
            _files.value = response.takeIf { it.isSuccessful }?.body().orEmpty()
        } catch (e: Exception) {
            logE("SessionRepo", "Error loading files", e)
        }
    }

    suspend fun readFile(path: String): String? {
        return try {
            val body = apiClient.api.readFile(path).takeIf { it.isSuccessful }?.body()
            body?.get("content") ?: body?.get("text")
        } catch (e: Exception) {
            logE("SessionRepo", "Error reading file", e)
            null
        }
    }

    suspend fun sendPrompt(
        sessionId: String,
        text: String,
        agent: String? = null,
        model: ModelOption? = null,
    ): Boolean {
        return try {
            val messageID = generateMessageId()
            val request = PromptRequest(
                messageID = messageID,
                parts = listOf(PartRequest(type = "text", text = text)),
                agent = agent,
                model = model?.let { ModelInfo(it.providerID, it.modelID) }
            )
            
            // Optimistic update
            val optimisticMessage = Message(id = messageID, sessionID = sessionId, role = "user")
            upsertMessage(optimisticMessage)
            // Add initial part for the user prompt text so it displays immediately
            _parts.value = _parts.value + (messageID to listOf(Part(text = text, type = "text", messageID = messageID)))
            
            // Explicitly set isLoading to true for the UI to show activity
            _isLoading.value = true

            val response = apiClient.api.sendPrompt(sessionId, request)
            if (response.isSuccessful) {
                // Do NOT call loadMessages(sessionId) here, trust the SSE stream to update the state.
                // We have already upserted the user message optimistically.
                true
            } else {
                _error.value = "Failed to send prompt: ${response.code()}"
                false
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error sending prompt", e)
            _error.value = "Connection error: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun abortSession(sessionId: String) {
        try {
            val response = apiClient.api.abortSession(sessionId)
            if (!response.isSuccessful) logE("SessionRepo", "Failed to abort session: ${response.code()}")
        } catch (e: Exception) {
            logE("SessionRepo", "Error aborting session", e)
        }
    }

    suspend fun compactSession(sessionId: String) {
        try {
            val response = apiClient.api.compactSession(sessionId)
            if (!response.isSuccessful) logE("SessionRepo", "Failed to compact session: ${response.code()}")
        } catch (e: Exception) {
            logE("SessionRepo", "Error compacting session", e)
        }
    }

    suspend fun deleteSession(sessionId: String) {
        try {
            val response = apiClient.api.deleteSession(sessionId)
            if (response.isSuccessful) {
                _sessions.value = _sessions.value.filter { it.id != sessionId }
                if (_currentSession.value?.id == sessionId) {
                    _currentSession.value = null
                    _messages.value = emptyList()
                    _parts.value = emptyMap()
                }
            } else {
                _error.value = "Failed to delete session: ${response.code()}"
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error deleting session", e)
            _error.value = "Connection error: ${e.message}"
        }
    }

    fun connectSse(sessionId: String, directory: String? = null) {
        disconnectSse()
        val encodedDirectory = URLEncoder.encode(directory ?: "", StandardCharsets.UTF_8.toString())
        
        sseJob = repositoryScope.launch {
            try {
                val call = apiClient.createStreamCall("global/event?directory=$encodedDirectory")
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        logE("SessionRepo", "SSE connection failed: ${response.code}")
                        return@launch
                    }

                    _isConnected.value = true
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
            } catch (e: Exception) {
                if (isActive) {
                    logE("SessionRepo", "SSE failed", e)
                }
            } finally {
                _isConnected.value = false
            }
        }
    }

    private fun handleSseEvent(type: String?, data: String) {
        logD("SessionRepo", "SSE event received: type=$type, data=$data")
        try {
            val event: Map<String, Any> = GSON.fromJson(data, MAP_TYPE)
            
            // Extract the actual event type from the payload if it's a generic "message" event
            val payload = event["payload"] as? Map<String, Any>
            val actualType = if (type == "message") {
                payload?.get("type") as? String
            } else {
                type
            }
            
            val properties = if (type == "message") {
                payload?.get("properties") as? Map<String, Any>
            } else {
                event["properties"] as? Map<String, Any>
            }
            
            if (actualType == null || properties == null) {
                logW("SessionRepo", "Skipping event: actualType=$actualType, properties=$properties")
                return
            }
            
            logD("SessionRepo", "Processing event: actualType=$actualType")

            when (actualType) {
                "message.updated" -> {
                    val info = properties["info"] as? Map<String, Any> ?: return
                    upsertMessage(GSON.fromJson(GSON.toJsonTree(info), Message::class.java))
                }
                "message.removed" -> {
                    val messageID = properties["messageID"] as? String ?: return
                    _messages.value = _messages.value.filter { it.id != messageID }
                }
                "message.part.updated" -> {
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
                    val messageId = properties["messageID"] as? String ?: return
                    val partId = properties["partID"] as? String ?: return
                    removePart(messageId, partId)
                }
                "session.status" -> {
                    val status = properties["status"] as? Map<String, Any>
                    val isIdle = status?.get("type") == "idle"
                    _isConnected.value = !isIdle
                    _isLoading.value = !isIdle
                }
                "session.error" -> {
                    val error = properties["error"] as? Map<String, Any>
                    _error.value = error?.let {
                        val name = it["name"] as? String ?: "Session error"
                        val data = it["data"] as? Map<String, Any>
                        val message = data?.get("message") as? String
                        "$name${message?.let { ": $it" }.orEmpty()}"
                    }
                }
            }
        } catch (e: Exception) {
            logE("SessionRepo", "Error handling SSE event", e)
        }
    }

    private fun upsertMessage(message: Message) {
        logD("SessionRepo", "upsertMessage: $message")
        val messageId = message.id ?: return
        _messages.update { current ->
            val mutableList = current.toMutableList()
            val index = mutableList.indexOfFirst { it.id == messageId }
            if (index >= 0) mutableList[index] = message else mutableList.add(message)
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
