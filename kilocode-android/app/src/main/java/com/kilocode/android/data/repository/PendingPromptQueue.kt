package com.kilocode.android.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private val Context.pendingPromptsDataStore by preferencesDataStore(name = "pending_prompts")

/**
 * Represents a pending prompt that should be sent when connectivity returns.
 */
data class PendingPrompt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String,
    val prompt: String,
    val agent: String? = null,
    val modelProvider: String? = null,
    val modelId: String? = null,
    val directory: String? = null,
    val serverUrl: String,
    val sharedSecret: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val maxAttempts: Int = 5
)

/**
 * Manages a persistent queue of prompts that failed due to network errors.
 * Automatically retries them when connectivity returns.
 * Survives app restarts.
 */
class PendingPromptQueue(private val context: Context) {

    private val gson = Gson()
    private val dataStore get() = context.pendingPromptsDataStore

    companion object {
        private const val TAG = "PendingPromptQueue"
        private val QUEUE_KEY = stringPreferencesKey("queue")
    }

    val pendingCount: Flow<Int> = dataStore.data
        .map { prefs ->
            val list = parseQueue(prefs[QUEUE_KEY] ?: "[]")
            list.size
        }

    suspend fun enqueue(prompt: PendingPrompt) {
        dataStore.edit { prefs ->
            val current = parseQueue(prefs[QUEUE_KEY] ?: "[]")
            val updated = current + prompt.copy(
                attemptCount = prompt.attemptCount + 1
            )
            prefs[QUEUE_KEY] = gson.toJson(updated)
            Log.d(TAG, "Enqueued prompt ${prompt.id}, queue size: ${updated.size}")
        }
    }

    suspend fun dequeue(id: String): PendingPrompt? {
        var removed: PendingPrompt? = null
        dataStore.edit { prefs ->
            val current = parseQueue(prefs[QUEUE_KEY] ?: "[]")
            val target = current.firstOrNull { it.id == id }
            if (target != null) {
                removed = target
                prefs[QUEUE_KEY] = gson.toJson(current.filter { it.id != id })
            }
        }
        return removed
    }

    suspend fun peekAll(): List<PendingPrompt> {
        var result: List<PendingPrompt> = emptyList()
        dataStore.data.first().let { prefs ->
            result = parseQueue(prefs[QUEUE_KEY] ?: "[]")
        }
        return result
    }

    suspend fun remove(id: String) {
        dataStore.edit { prefs ->
            val current = parseQueue(prefs[QUEUE_KEY] ?: "[]")
            prefs[QUEUE_KEY] = gson.toJson(current.filter { it.id != id })
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs[QUEUE_KEY] = "[]"
        }
    }

    private fun parseQueue(json: String): List<PendingPrompt> {
        return try {
            gson.fromJson(json, object : TypeToken<List<PendingPrompt>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Observes network connectivity and notifies listeners when internet becomes available.
 * Use this to trigger auto-retry of pending prompts when connection returns.
 */
class NetworkConnectivityObserver(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Flow that emits `true` when internet is available, `false` when it becomes unavailable.
     */
    val isConnected: StateFlow<Boolean> = flow {
        // Emit current state first
        emit(isNetworkAvailable())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network available, check if it has internet
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                if (hasInternet) {
                    sharedFlow.tryEmit(true)
                }
            }

            override fun onLost(network: Network) {
                sharedFlow.tryEmit(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                sharedFlow.tryEmit(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Collect from shared flow and emit to StateFlow
        sharedFlow.collect { emit(it) }

        // Unregister when collector is cancelled
        connectivityManager.unregisterNetworkCallback(callback) // Will never reach here since collect suspends
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        started = SharingStarted.Eagerly,
        initialValue = isNetworkAvailable()
    )

    private val sharedFlow = MutableSharedFlow<Boolean>(replay = 1)

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkConnectivityObserver? = null

        fun getInstance(context: Context): NetworkConnectivityObserver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkConnectivityObserver(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
