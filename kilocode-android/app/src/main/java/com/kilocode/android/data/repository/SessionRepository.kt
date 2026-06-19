/*
 * [Parent Feature/Milestone] Phase 1: Foundation
 * [Child Task/Issue] #1
 * [Subtask] Implement SessionRepository
 * [Upstream] KiloCodeApi -> [Downstream] SessionViewModel
 * [Law Check] 50 lines | Passed Do It Check
 */
package com.kilocode.android.data.repository

import com.kilocode.android.data.api.KiloCodeApi
import com.kilocode.android.data.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepository(private val api: KiloCodeApi) {

    suspend fun listSessions() = withContext(Dispatchers.IO) {
        try {
            val response = api.listSessions()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createSession(name: String) = withContext(Dispatchers.IO) {
        try {
            val response = api.createSession(mapOf("name" to name))
            response.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            api.deleteSession(sessionId)
        } catch (e: Exception) {
            // Handle error
        }
    }
}

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
                    loadMessages(session.id)
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
                val msgs = response.body() ?: emptyList()
                _messages.value = msgs
                withContext(Dispatchers.IO) {
                    msgs.map { message ->
                        async { loadParts(sessionId, message.id) }
                    }.awaitAll()
                }
            } else {
                _error.value = "Failed to load messages: ${response.code()}"
            }
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error loading messages", e)
            _error.value = "Connection error: ${e.message}"
        }
    }

    private suspend fun loadParts(sessionId: String, messageId: String) {
        try {
            val response = apiClient.api.listParts(sessionId, messageId)
            if (response.isSuccessful) {
                val messageParts: List<Part> = response.body() ?: emptyList()
                _parts.value = _parts.value + (messageId to messageParts)
            }
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error loading parts", e)
        }
    }

    suspend fun sendPrompt(sessionId: String, text: String): Boolean {
        return try {
            _isLoading.value = true
            val request = mapOf(
                "messageID" to generateMessageId(),
                "parts" to listOf(mapOf("type" to "text", "text" to text))
            )
            val response = apiClient.api.sendPrompt(sessionId, request)
            if (response.isSuccessful) {
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
            if (!response.isSuccessful) {
                Log.e("SessionRepo", "Failed to abort session: ${response.code()}")
            }
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
        val baseUrl = apiClient.baseUrl.removeSuffix("/")
        eventSource = apiClient.createEventSource(
            "$baseUrl/session/$sessionId/events",
            object : okhttp3.sse.EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                    _isConnected.value = true
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    handleSseEvent(type, data)
                }

                override fun onClosed(eventSource: EventSource) {
                    _isConnected.value = false
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: okhttp3.Response?,
                ) {
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
                    val message = GSON.fromJson(GSON.toJsonTree(info), Message::class.java)
                    val current = _messages.value.toMutableList()
                    val index = current.indexOfFirst { it.id == message.id }
                    if (index >= 0) {
                        current[index] = message
                    } else {
                        current.add(message)
                    }
                    _messages.value = current
                }
                "message.removed" -> {
                    val messageID = properties["messageID"] as? String ?: return
                    _messages.value = _messages.value.filter { it.id != messageID }
                }
                "part.updated" -> {
                    val partData = properties["part"] as? Map<String, Any> ?: return
                    val part = GSON.fromJson(GSON.toJsonTree(partData), Part::class.java)
                    val currentParts = _parts.value.toMutableMap()
                    val messageParts = currentParts[part.messageID]?.toMutableList() ?: mutableListOf()
                    val index = messageParts.indexOfFirst { it.id == part.id }
                    if (index >= 0) {
                        messageParts[index] = part
                    } else {
                        messageParts.add(part)
                    }
                    currentParts[part.messageID] = messageParts
                    _parts.value = currentParts
                }
            }
        } catch (e: Exception) {
            Log.e("SessionRepo", "Error handling SSE event", e)
        }
    }

    fun disconnectSse() {
        eventSource?.cancel()
        eventSource = null
        _isConnected.value = false
    }

    private fun generateMessageId(): String {
        return "msg_${UUID.randomUUID()}"
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        private val GSON = Gson()
        private val MAP_TYPE = object : TypeToken<Map<String, Any>>() {}.type
    }
}
