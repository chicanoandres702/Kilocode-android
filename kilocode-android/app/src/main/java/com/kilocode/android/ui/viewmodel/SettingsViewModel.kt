/*
 * [Parent Feature/Milestone] Phase 1: Foundation
 * [Child Task/Issue] #1
 * [Subtask] Implement SettingsViewModel with Test Connection
 * [Upstream] AuthPreferencesRepository -> [Downstream] SettingsScreen
 * [Law Check] 40 lines | Passed Do It Check
 */
package com.kilocode.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.repository.AuthPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthPreferencesRepository(application)

    val sharedSecret: StateFlow<String?> = repository.sharedSecretFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val serverUrl: StateFlow<String?> = repository.serverUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autonomousMode: StateFlow<Boolean> = repository.autonomousModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val apiServerUrl: StateFlow<String?> = repository.apiServerUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            repository.saveServerUrl(url)
        }
    }

    fun saveApiServerUrl(url: String) {
        viewModelScope.launch {
            repository.saveApiServerUrl(url)
        }
    }

    fun saveAutonomousMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutonomousMode(enabled)
        }
    }

    fun saveSharedSecret(secret: String) {
        viewModelScope.launch {
            repository.saveSharedSecret(secret)
        }
    }

    fun testConnection(serverUrl: String, secret: String) {
        viewModelScope.launch {
            _connectionStatus.value = "Testing..."
            try {
                val client = ApiClient.getInstance(serverUrl, secret)
                val response = client.api.getConfig()
                if (response.isSuccessful) {
                    _connectionStatus.value = "Success: Connected to server."
                } else {
                    _connectionStatus.value = "Error: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Error: ${e.message}"
            }
        }
    }
}
