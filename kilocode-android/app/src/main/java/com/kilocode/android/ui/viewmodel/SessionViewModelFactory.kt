/*
 * [Parent Feature/Milestone] Phase 1: Foundation
 * [Child Task/Issue] #1
 * [Subtask] Implement SessionViewModelFactory
 * [Upstream] ApiClient -> [Downstream] SessionViewModel
 * [Law Check] 20 lines | Passed Do It Check
 */
package com.kilocode.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.repository.SessionRepository

class SessionViewModelFactory(private val apiClient: ApiClient) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(SessionRepository(apiClient)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
