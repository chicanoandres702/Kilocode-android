/*
 * [Parent Feature/Milestone] Phase 1: Foundation
 * [Child Task/Issue] #1
 * [Subtask] Implement SessionViewModel
 * [Upstream] SessionRepository -> [Downstream] HomeScreen/SessionScreen
 * [Law Check] 40 lines | Passed Do It Check
 */
package com.kilocode.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilocode.android.data.model.Session
import com.kilocode.android.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SessionViewModel(private val repository: SessionRepository) : ViewModel() {

    val sessions: StateFlow<List<Session>> = repository.sessions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.listSessions()
                if (repository.error.value == null) {
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun createSession(name: String): Session? {
        val session = repository.createSession(name)
        loadSessions()
        return session
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            loadSessions()
        }
    }
}
