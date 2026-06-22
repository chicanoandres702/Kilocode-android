package com.kilocode.android.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DebugRepository {
    private val _debugLogs = MutableStateFlow<List<String>>(emptyList())
    val debugLogs: StateFlow<List<String>> = _debugLogs

    fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _debugLogs.value = listOf("[$timestamp] $message") + _debugLogs.value.take(49)
    }

    fun clearLogs() {
        _debugLogs.value = emptyList()
    }
}
