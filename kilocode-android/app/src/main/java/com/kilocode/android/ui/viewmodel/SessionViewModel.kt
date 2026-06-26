package com.kilocode.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilocode.android.data.model.FileNode
import com.kilocode.android.data.model.Session
import com.kilocode.android.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SessionViewModel(private val repository: SessionRepository) : ViewModel() {

    val sessions: StateFlow<List<Session>> = repository.sessions
    val folders: StateFlow<List<FileNode>> = repository.folders
    val currentDirectory: StateFlow<String> = repository.currentDirectory
    val directoryExists: StateFlow<Boolean?> = repository.directoryExists
    val isCheckingDirectory: StateFlow<Boolean> = repository.isCheckingDirectory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingFolders = MutableStateFlow(false)
    val isLoadingFolders: StateFlow<Boolean> = _isLoadingFolders

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun checkDirectory(path: String) {
        viewModelScope.launch {
            _isLoadingFolders.value = true
            try {
                repository.checkDirectoryExists(path)
                if (repository.error.value == null) {
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingFolders.value = false
            }
        }
    }

    fun loadAndCheckDirectory(path: String) {
        viewModelScope.launch {
            _isLoadingFolders.value = true
            try {
                repository.checkDirectoryExists(path)
                if (repository.directoryExists.value == true) {
                    repository.listSessions(path)
                }
                if (repository.error.value == null) {
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingFolders.value = false
            }
        }
    }
    // loadFolders is replaced by checkDirectoryExists in SessionRepository — use loadAndCheckDirectory instead

    fun loadSessions(directory: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.listSessions(directory)
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

    fun navigateToFolder(path: String) {
        repository.navigateToFolder(path)
        loadAndCheckDirectory(path)
    }

    fun navigateUp(): String? {
        val parentPath = repository.navigateUp()
        if (parentPath != null) {
            loadAndCheckDirectory(parentPath)
        }
        return parentPath
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun createSession(name: String): Session? {
        val session = repository.createSession(name)
        loadSessions(repository.currentDirectory.value)
        return session
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            loadSessions(repository.currentDirectory.value)
        }
    }
}
