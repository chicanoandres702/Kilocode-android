package com.kilocode.android.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.util.UUID

class SessionRepository(private val apiClient: ApiClient) {
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var eventSource: EventSource? = null

    suspend fun listSessions() {
        _isLoading.value = true
        try {
            val response = apiClient.api.listSessions()
            _sessions.value = response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error loading sessions", e)
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
            val providers = apiClient.api.listProviders()
            val config = apiClient.api.getConfig()
            val options = buildList {
                providers.takeIf { it.isSuccessful }?.body().orEmpty().forEach { provider ->
                    provider.models.values.forEach { model ->
                        add(ModelOption(provider.id, model.id, "${provider.name} · ${model.name}"))
                    }
                }
                if (isEmpty()) {
                    config.takeIf { it.isSuccessful }?.body()?.models.orEmpty().forEach { id ->
                        add(ModelOption("default", id, id))
                    }
                }
            }
            _models.value = options
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error loading models", e)
        }
    }

    suspend fun sendPrompt(
        sessionId: String,
        text: String,
        agent: String? = null,
        model: ModelOption? = null,
    ): Boolean {
        return try {
            _isLoading.value = true
            val request = mutableMapOf<String, Any>(
                "messageID" to generateMessageId(),
                "parts" to listOf(mapOf("type" to "text", "text" to text))
            )
            agent?.let { request["agent"] = it }
            model?.let {
                request["model"] = mapOf("providerID" to it.providerID, "modelID" to it.modelID)
                request["providerID"] = it.providerID
                request["modelID"] = it.modelID
            }
            val response = apiClient.api.sendPrompt(sessionId, request)
            if (response.isSuccessful) {
                response.body()?.let { messageWithParts ->
                    messageWithParts.info?.let { message ->
                        upsertMessage(message)
                        message.id?.let { _parts.value = _parts.value + (it to messageWithParts.parts) }
                    }
                }
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
        } finally {
            _isLoading.value = false
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

    fun connectSse(sessionId: String) {
        disconnectSse()
        eventSource = apiClient.createEventSource(
            "event",
            object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                    _isConnected.value = true
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    handleSseEvent(type, data)
                }

                override fun onClosed(eventSource: EventSource) {
                    _isConnected.value = false
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                    _isConnected.value = false
                    Log.e("SessionRepo", "SSE failed: ${t?.message}")
                }
            }
        )
    }

    private fun handleSseEvent(type: String?, data: String) {
        try {
            val event: Map<String, Any> = GSON.fromJson(data, MAP_TYPE)
            val properties = event["properties"] as? Map<String, Any> ?: return

            when (type) {
                "message.updated" -> {
                    val info = properties["info"] as? Map<String, Any> ?: return
                    upsertMessage(GSON.fromJson(GSON.toJsonTree(info), Message::class.java))
                }
                "message.removed" -> {
                    val messageID = properties["messageID"] as? String ?: return
                    _messages.value = _messages.value.filter { it.id != messageID }
                }
                "message.part.updated" -> {
                    val partData = properties["part"] as? Map<String, Any> ?: return
                    val part = GSON.fromJson(GSON.toJsonTree(partData), Part::class.java)
                    val messageId = part.messageID ?: return
                    val partId = part.id ?: return
                    upsertPart(messageId, partId, part)
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
                        val data = it["data"] as? Map<String, Any>
                        val message = data?.get("message") as? String
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
        val current = _messages.value.toMutableList()
        val index = current.indexOfFirst { it.id == messageId }
        if (index >= 0) current[index] = message else current.add(message)
        _messages.value = current
    }

    private fun upsertPart(messageId: String, partId: String, part: Part) {
        val currentParts = _parts.value.toMutableMap()
        val messageParts = currentParts[messageId]?.toMutableList() ?: mutableListOf()
        val index = messageParts.indexOfFirst { it.id == partId }
        if (index >= 0) messageParts[index] = part else messageParts.add(part)
        currentParts[messageId] = messageParts
        _parts.value = currentParts
    }

    private fun removePart(messageId: String, partId: String) {
        val currentParts = _parts.value.toMutableMap()
        currentParts[messageId] = currentParts[messageId]?.filter { it.id != partId }.orEmpty()
        _parts.value = currentParts
    }

    fun disconnectSse() {
        eventSource?.cancel()
        eventSource = null
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
