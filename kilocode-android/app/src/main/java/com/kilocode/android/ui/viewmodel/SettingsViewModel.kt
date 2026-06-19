/*
 * [Parent Feature/Milestone] Phase 1: Foundation
 * [Child Task/Issue] #1
 * [Subtask] Implement SettingsViewModel
 * [Upstream] AuthPreferencesRepository -> [Downstream] SettingsScreen
 * [Law Check] 26 lines | Passed Do It Check
 */
package com.kilocode.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kilocode.android.data.repository.AuthPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthPreferencesRepository(application)
    
    val sharedSecret: StateFlow<String?> = repository.sharedSecretFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveSharedSecret(secret: String) {
        viewModelScope.launch {
            repository.saveSharedSecret(secret)
        }
    }
}
