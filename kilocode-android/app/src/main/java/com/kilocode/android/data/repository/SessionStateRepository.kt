package com.kilocode.android.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionStateDataStore by preferencesDataStore(name = "session_state")

/**
 * Persists the active session ID and directory so the app can auto-resume
 * after a crash or restart. Uses DataStore for async, crash-safe writes.
 */
class SessionStateRepository(private val context: Context) {

    private val dataStore get() = context.sessionStateDataStore

    companion object {
        private val SESSION_ID_KEY = stringPreferencesKey("active_session_id")
        private val DIRECTORY_KEY = stringPreferencesKey("active_directory")
    }

    /**
     * Save the currently active session for crash recovery.
     */
    suspend fun save(sessionId: String, directory: String) {
        dataStore.edit { prefs ->
            prefs[SESSION_ID_KEY] = sessionId
            prefs[DIRECTORY_KEY] = directory
        }
    }

    /**
     * Retrieve the last active session ID and directory.
     * Returns Pair(null, null) if no session was saved.
     */
    suspend fun get(): Pair<String?, String?> {
        val prefs = dataStore.data.first()
        val sessionId = prefs[SESSION_ID_KEY]
        val directory = prefs[DIRECTORY_KEY]
        return sessionId to directory
    }

    /**
     * Clear persisted session state (e.g., on logout or explicit session close).
     */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(SESSION_ID_KEY)
            prefs.remove(DIRECTORY_KEY)
        }
    }
}
