package com.kilocode.android.data

import android.content.Context
import android.util.Log
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.ModelInfo
import com.kilocode.android.data.repository.NetworkConnectivityObserver
import com.kilocode.android.data.repository.PendingPrompt
import com.kilocode.android.data.repository.PendingPromptQueue
import com.kilocode.android.data.repository.SessionStateRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Autonomous recovery manager that orchestrates:
 * - Network connectivity monitoring
 * - Pending prompt queue auto-retry when connection returns
 * - Session state persistence for crash recovery
 *
 * This is the central "make it seamless" component.
 */
class AutonomousRecoveryManager private constructor(
    private val context: Context
) {
    private val connectivityObserver = NetworkConnectivityObserver.getInstance(context)
    private val promptQueue = PendingPromptQueue(context)
    private val sessionState = SessionStateRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var wasConnected = true

    companion object {
        private const val TAG = "AutoRecoveryMgr"

        @Volatile
        private var INSTANCE: AutonomousRecoveryManager? = null

        fun getInstance(context: Context): AutonomousRecoveryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutonomousRecoveryManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {
        // Start monitoring connectivity and auto-retrying pending prompts
        scope.launch {
            connectivityObserver.isConnected.collect { connected ->
                if (connected && !wasConnected) {
                    Log.i(TAG, "Connection restored — retrying pending prompts")
                    retryPendingPrompts()
                }
                wasConnected = connected
            }
        }
    }

    /**
     * Queue a prompt for retry when connectivity returns.
     */
    fun queuePendingPrompt(prompt: PendingPrompt) {
        scope.launch {
            promptQueue.enqueue(prompt)
            Log.d(TAG, "Queued prompt for retry: ${prompt.id}")
        }
    }

    /**
     * Retry all pending prompts now (called when connectivity returns).
     */
    private suspend fun retryPendingPrompts() {
        val pending = promptQueue.peekAll()
        if (pending.isEmpty()) return

        Log.i(TAG, "Retrying ${pending.size} pending prompts")

        for (prompt in pending) {
            if (prompt.attemptCount >= prompt.maxAttempts) {
                Log.w(TAG, "Prompt ${prompt.id} exceeded max attempts, removing it")
                promptQueue.remove(prompt.id)
                continue
            }

            try {
                val client = ApiClient.getInstance(prompt.serverUrl, prompt.sharedSecret)
                val modelInfo = if (prompt.modelProvider != null && prompt.modelId != null) {
                    ModelInfo(prompt.modelProvider, prompt.modelId)
                } else null

                client.sendPrompt(
                    sessionId = prompt.sessionId,
                    prompt = prompt.prompt,
                    agent = prompt.agent,
                    model = modelInfo,
                    directory = prompt.directory
                )

                // Success — remove from queue
                promptQueue.remove(prompt.id)
                Log.i(TAG, "Prompt ${prompt.id} sent successfully on retry")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to retry prompt ${prompt.id}: ${e.message}")
                // Will retry again when connectivity changes or next attempt cycle
            }
        }
    }

    /**
     * Persist the current session ID for crash recovery.
     */
    suspend fun saveSessionState(sessionId: String, directory: String) {
        sessionState.save(sessionId, directory)
    }

    /**
     * Get the last active session ID for auto-recovery on app restart.
     */
    suspend fun getLastSessionState(): Pair<String?, String?> {
        return sessionState.get()
    }

    /**
     * Clear persisted session state (e.g., on logout or explicit close).
     */
    suspend fun clearSessionState() {
        sessionState.clear()
    }

    /**
     * Get flow of pending prompt count for UI indicator.
     */
    fun getPendingCount(): Flow<Int> = promptQueue.pendingCount

    /**
     * Force retry now — used when user taps retry or app comes to foreground.
     */
    fun forceRetryNow() {
        scope.launch {
            retryPendingPrompts()
        }
    }
}
