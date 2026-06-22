package com.kilocode.android.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.DebugRepository
import com.kilocode.android.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
//...
class SessionRepository(private val apiClient: ApiClient) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //...
    override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
        // ...
        Log.e("SessionRepo", "SSE failed: $errorMessage, response: $response")
        _error.value = "SSE Connection failed: $errorMessage"
        
        // Auto-reconnect
        scope.launch {
            delay(5000)
            val directory = _currentSession.value?.directory
            connectSse(directory)
        }
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
            Log.e("SessionRepo", "Error creating session", e)
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
            Log.e("SessionRepo", "Error selecting session", e)
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
            Log.e("SessionRepo", "Error loading messages", e)
            _error.value = "Connection error: ${e.message}"
        }
    }

    suspend fun listAgents() {
        try {
            val response = apiClient.api.listAgents()
            _agents.value = response.takeIf { it.isSuccessful }?.body().orEmpty()
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error loading agents", e)
        }
    }

    suspend fun listModels() {
        try {
            val response = apiClient.api.listProviders()
            val providerResponse = response.takeIf { it.isSuccessful }?.body()
            val connectedProviders = providerResponse?.connected.orEmpty()
            val providers = providerResponse?.all.orEmpty().entries
                .filter { connectedProviders.isEmpty() || it.key in connectedProviders }
            val options = buildList {
                providers.forEach { (providerID, provider) ->
                    provider.models.values.forEach { model ->
                        add(
                            ModelOption(
                                providerID = providerID,
                                modelID = model.id,
                                displayName = model.name.ifBlank { model.id },
                                category = provider.name.ifBlank { providerID },
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
            Log.e("SessionRepo", "Error loading models", e)
        }
    }

    suspend fun loadProject() {
        try {
            val response = apiClient.api.getProject()
            _project.value = response.takeIf { it.isSuccessful }?.body()
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error loading project", e)
        }
    }

    suspend fun listFiles(directory: String? = null) {
        try {
            val response = apiClient.api.listFiles(directory)
            _files.value = response.takeIf { it.isSuccessful }?.body().orEmpty()
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error loading files", e)
        }
    }

    suspend fun readFile(path: String): String? {
        return try {
            val body = apiClient.api.readFile(path).takeIf { it.isSuccessful }?.body()
            body?.get("content") ?: body?.get("text")
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error reading file", e)
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
            expectedResponseId = messageID
            addDebugLog("sendPrompt started: messageID=$messageID")

            val request = PromptRequest(
                messageID = messageID,
                parts = listOf(PartRequest(type = "text", text = text)),
                agent = agent,
                model = model?.let { ModelInfo(it.providerID, it.modelID) }
            )
            
            // Optimistic update
            val optimisticMessage = Message(id = messageID, sessionID = sessionId, role = "user")
            upsertMessage(optimisticMessage)
            _parts.value = _parts.value + (messageID to listOf(Part(text = text, type = "text", messageID = messageID)))

            val directory = _currentSession.value?.directory ?: return false
            if (!sseConnected && !connectSse(directory)) {
                return false
            }

            val response = apiClient.api.sendPrompt(sessionId, request, directory)
            if (response.isSuccessful) {
                _error.value = null
                delay(1000)
                loadMessages(sessionId)
                true
            } else {
                _error.value = "Failed to send prompt: ${response.code()}"
                false
            }
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error sending prompt", e)
            _error.value = "Connection error: ${e.message}"
            false
        }
    }

    suspend fun abortSession(sessionId: String) {
        try {
            val response = apiClient.api.abortSession(sessionId)
            if (!response.isSuccessful) Log.e("SessionRepo", "Failed to abort session: ${response.code()}")
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error aborting session", e)
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
            Log.e("SessionRepo", "Error deleting session", e)
            _error.value = "Connection error: ${e.message}"
        }
    }

    suspend fun connectSse(
        directory: String? = null,
        timeoutMillis: Long = 10000L,
    ): Boolean {
        if (sseConnected) return true

        val existingOpening = sseOpening
        if (existingOpening != null) {
            return withTimeoutOrNull(timeoutMillis) { existingOpening.await() } == true
        }

        val opened = CompletableDeferred<Boolean>()
        sseOpening = opened
        try {
            closeEventSource()
            val encodedDirectory = URLEncoder.encode(directory ?: "", StandardCharsets.UTF_8.toString())
            eventSource = apiClient.createEventSource(
                "global/event?directory=$encodedDirectory",
                object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                        _isConnected.value = true
                        sseConnected = true
                        if (!opened.isCompleted) opened.complete(true)
                        Log.d("SessionRepo", "SSE opened")
                    }

                    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                        Log.d("SessionRepo", "SSE event received: id=$id type=$type")
                        handleSseEvent(type, data)
                    }

                    override fun onClosed(eventSource: EventSource) {
                        _isConnected.value = false
                        sseConnected = false
                        if (!opened.isCompleted) opened.complete(false)
                        Log.d("SessionRepo", "SSE closed")
                    }

                    override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                        _isConnected.value = false
                        sseConnected = false
                        if (!opened.isCompleted) opened.complete(false)
                        val errorMessage = t?.message ?: "Unknown SSE failure"
                        Log.e("SessionRepo", "SSE failed: $errorMessage, response: $response")
                        _error.value = "SSE Connection failed: $errorMessage"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error connecting SSE", e)
            _error.value = "SSE Connection failed: ${e.message}"
            return false
        } finally {
            if (sseOpening === opened) sseOpening = null
        }

        val openedWithinTimeout = withTimeoutOrNull(timeoutMillis) { opened.await() } == true
        if (!openedWithinTimeout) {
            Log.w("SessionRepo", "Timed out waiting for SSE to open")
            _error.value = "SSE Connection timed out"
            closeEventSource()
            return false
        }
        return true
    }

    private fun handleSseEvent(type: String?, data: String) {
        Log.d("SessionRepo", "SSE event received: type=$type, data=$data")
        try {
            val event: Map<String, Any> = GSON.fromJson(data, MAP_TYPE)
            val properties = event["properties"] as? Map<String, Any> ?: return
            val sessionId = _currentSession.value?.id
            Log.d("SessionRepo", "SSE event properties: $properties")

            when (type) {
                "message.updated" -> {
                    val info = properties["info"] as? Map<String, Any> ?: return
                    Log.d("SessionRepo", "Processing message.updated: $info")
                    val message = GSON.fromJson(GSON.toJsonTree(info), Message::class.java)
                    val messageId = message.id ?: return
                    upsertMessage(message.copy(sessionID = message.sessionID ?: sessionId))

                    val parts = properties["parts"] as? List<*>
                    parts?.forEach { rawPart ->
                        val part = GSON.fromJson(GSON.toJsonTree(rawPart), Part::class.java)
                        val partId = part.id ?: return@forEach
                        upsertPart(sessionId, messageId, partId, part)
                    }
                }
                "message.removed" -> {
                    val messageID = properties["messageID"] as? String ?: return
                    _messages.value = _messages.value.filter { it.id != messageID }
                }
                "message.part.updated" -> {
                    val partData = properties["part"] as? Map<String, Any> ?: return
                    Log.d("SessionRepo", "Processing message.part.updated: $partData")
                    val part = GSON.fromJson(GSON.toJsonTree(partData), Part::class.java)
                    val messageId = part.messageID ?: return
                    val partId = part.id ?: return
                    if (!_messages.value.any { it.id == messageId }) {
                        upsertMessage(Message(id = messageId, sessionID = sessionId, role = "assistant"))
                    }
                    upsertPart(sessionId, messageId, partId, part)
                }
                "message.part.delta" -> {
                    val messageId = properties["messageID"] as? String ?: return
                    val partId = properties["partID"] as? String ?: return
                    val field = properties["field"] as? String ?: return
                    val delta = properties["delta"] as? String ?: return
                    
                    if (field == "text") {
                        val currentParts = _parts.value.toMutableMap()
                        val messageParts = currentParts[messageId]?.toMutableList() ?: mutableListOf()
                        val index = messageParts.indexOfFirst { it.id == partId }
                        if (index >= 0) {
                            val part = messageParts[index]
                            messageParts[index] = part.copy(text = (part.text ?: "") + delta)
                            currentParts[messageId] = messageParts
                            _parts.value = currentParts
                        }
                    }
                }
                "message.part.removed" -> {
                    val messageId = properties["messageID"] as? String ?: return
                    val partId = properties["partID"] as? String ?: return
                    removePart(messageId, partId)
                }
                "session.status" -> {
                    val status = properties["status"] as? Map<String, Any>
                    _isConnected.value = status?.get("type") != "idle"
                }
                "session.error" -> {
                    val error = properties["error"] as? Map<String, Any>
                    _error.value = error?.let {
                        val name = it["name"] as? String ?: "Session error"
                        val dataMap = it["data"] as? Map<String, Any>
                        val message = dataMap?.get("message") as? String
                        "$name${message?.let { ": $it" }.orEmpty()}"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error handling SSE event", e)
        }
    }

    private fun upsertMessage(message: Message) {
        val messageId = message.id ?: return
        val updatedMessage = message.copy(sessionID = message.sessionID ?: _currentSession.value?.id)
        val current = _messages.value.toMutableList()
        val index = current.indexOfFirst { it.id == messageId }
        
        if (index >= 0) {
            current[index] = updatedMessage
            Log.d("SessionRepo", "Message updated: $messageId, role: ${updatedMessage.role}")
        } else {
            current.add(updatedMessage)
            Log.d("SessionRepo", "Message added: $messageId, role: ${updatedMessage.role}")
        }
        _messages.value = current
    }

    private fun upsertPart(sessionId: String?, messageId: String, partId: String, part: Part) {
        val updatedPart = part.copy(
            sessionID = part.sessionID ?: sessionId,
            messageID = part.messageID ?: messageId,
        )
        val currentParts = _parts.value.toMutableMap()
        val messageParts = currentParts[messageId]?.toMutableList() ?: mutableListOf()
        val index = messageParts.indexOfFirst { it.id == partId }
        
        if (index >= 0) {
            messageParts[index] = updatedPart
            Log.d("SessionRepo", "Part updated: $partId in $messageId")
        } else {
            messageParts.add(updatedPart)
            Log.d("SessionRepo", "Part added: $partId in $messageId")
        }
        currentParts[messageId] = messageParts
        _parts.value = currentParts
    }

    private fun removePart(messageId: String, partId: String) {
        val currentParts = _parts.value.toMutableMap()
        currentParts[messageId] = currentParts[messageId]?.filter { it.id != partId }.orEmpty()
        _parts.value = currentParts
    }

    private fun closeEventSource() {
        eventSource?.cancel()
        eventSource = null
        sseConnected = false
        _isConnected.value = false
    }

    fun disconnectSse() {
        closeEventSource()
        sseOpening?.complete(false)
        sseOpening = null
    }

    fun clearError() {
        _error.value = null
    }

    fun addDebugLog(message: String) {
        DebugRepository.addLog(message)
    }

    fun clearDebugLogs() {
        DebugRepository.clearLogs()
    }

    private fun generateMessageId(): String = "msg_${UUID.randomUUID()}"

    companion object {
        private val GSON = Gson()
        private val MAP_TYPE = object : TypeToken<Map<String, Any>>() {}.type
    }
}
