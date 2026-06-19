/*
 * [Parent Feature/Milestone] Phase 1: Foundation
 * [Child Task/Issue] #1
 * [Subtask] Implement AuthPreferencesRepository
 * [Upstream] DataStore -> [Downstream] ApiClient
 * [Law Check] 28 lines | Passed Do It Check
 */
package com.kilocode.android.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class AuthPreferencesRepository(private val context: Context) {

    companion object {
        val SHARED_SECRET_KEY = stringPreferencesKey("shared_secret")
    }

    val sharedSecretFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SHARED_SECRET_KEY]
        }

    suspend fun saveSharedSecret(secret: String) {
        context.dataStore.edit { preferences ->
            preferences[SHARED_SECRET_KEY] = secret
        }
    }
}
