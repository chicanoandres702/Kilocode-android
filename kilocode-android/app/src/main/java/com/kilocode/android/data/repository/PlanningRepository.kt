package com.kilocode.android.data.repository

import android.util.Log
import com.kilocode.android.data.AppError
import com.kilocode.android.data.httpError
import com.kilocode.android.data.isRetryable
import com.kilocode.android.data.model.*
import com.kilocode.android.data.retryDelay
import com.kilocode.android.data.toAppError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response

/**
 * Repository for planning data (milestones and issues) with retry and in-memory caching.
 * Wraps ApiClient calls following the same pattern as SessionRepository.
 */
class PlanningRepository(private val apiClient: com.kilocode.android.data.api.ApiClient) {

    companion object {
        private const val TAG = "PlanningRepository"
        private const val MAX_RETRIES = 3
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    // -- State flows for reactive UI ---------------------------------------------

    private val _milestones = MutableStateFlow<List<Milestone>>(emptyList())
    val milestones: StateFlow<List<Milestone>> = _milestones.asStateFlow()

    private val _issues = MutableStateFlow<Map<Int, List<Issue>>>(emptyMap())
    val issues: StateFlow<Map<Int, List<Issue>>> = _issues.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    // -- Cache -------------------------------------------------------------------

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private var milestonesCache: CacheEntry<List<Milestone>>? = null
    private val issuesCache = mutableMapOf<Int, CacheEntry<List<Issue>>>()

    private fun <T> getCached(entry: CacheEntry<T>?): T? {
        if (entry == null) return null
        return if (System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS) {
            entry.data
        } else null
    }

    // -- Retry wrapper -----------------------------------------------------------

    private suspend fun <T> withRetry(
        operation: String,
        block: suspend () -> T,
    ): T {
        var lastError: AppError? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val appError = e.toAppError()
                lastError = appError
                if (!appError.isRetryable()) {
                    throw e
                }
                val delayMs = appError.retryDelay(attempt)
                Log.w(TAG, "$operation failed (attempt ${attempt + 1}/$MAX_RETRIES): ${appError.message}. Retrying in ${delayMs}ms")
                delay(delayMs)
            }
        }
        throw lastError ?: Exception("$operation failed after $MAX_RETRIES attempts")
    }

    private fun <T> unwrap(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw httpError(204, "Empty response body")
        } else {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    // -- Public API --------------------------------------------------------------

    suspend fun getMilestones(state: String? = null, forceRefresh: Boolean = false): List<Milestone> {
        Log.i(TAG, "getMilestones called: state=$state, forceRefresh=$forceRefresh")
        getCached(milestonesCache)?.let { if (!forceRefresh) return it }
        _isLoading.value = true
        _error.value = null
        return try {
            val result = withRetry("getMilestones") {
                val call = apiClient.api.listMilestones(state = state)
                Log.d(TAG, "getMilestones requested: ${call.raw().request.url}")
                unwrap(call)
            }
            val list = result.milestones
            milestonesCache = CacheEntry(list)
            _milestones.value = list
            list
        } catch (e: Exception) {
            val appError = e.toAppError()
            _error.value = appError
            Log.e(TAG, "getMilestones failed", e)
            emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getIssues(milestoneNumber: Int, state: String? = null, forceRefresh: Boolean = false): List<Issue> {
        Log.i(TAG, "getIssues called: milestoneNumber=$milestoneNumber, state=$state, forceRefresh=$forceRefresh")
        getCached(issuesCache[milestoneNumber])?.let { if (!forceRefresh) return it }
        _isLoading.value = true
        _error.value = null
        return try {
            val result = withRetry("getIssues") {
                val call = apiClient.api.listMilestoneIssues(milestoneNumber, state = state)
                Log.d(TAG, "getIssues requested: ${call.raw().request.url}")
                unwrap(call)
            }
            val list = result.issues
            issuesCache[milestoneNumber] = CacheEntry(list)
            _issues.value = _issues.value.toMutableMap().apply { put(milestoneNumber, list) }
            list
        } catch (e: Exception) {
            val appError = e.toAppError()
            _error.value = appError
            Log.e(TAG, "getIssues failed for milestone #$milestoneNumber", e)
            emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createMilestone(title: String, description: String? = null, dueOn: String? = null): Milestone? {
        _error.value = null
        return try {
            val result = withRetry("createMilestone") {
                unwrap(apiClient.api.createMilestone(CreateMilestoneRequest(title = title, description = description, dueOn = dueOn)))
            }
            // Invalidate milestones cache
            milestonesCache = null
            _milestones.value = _milestones.value + result
            result
        } catch (e: Exception) {
            val appError = e.toAppError()
            _error.value = appError
            Log.e(TAG, "createMilestone failed", e)
            null
        }
    }

    suspend fun createIssue(title: String, body: String? = null, milestoneNumber: Int? = null, labels: List<String> = emptyList()): Issue? {
        _error.value = null
        return try {
            val result = withRetry("createIssue") {
                unwrap(apiClient.api.createIssue(CreateIssueRequest(title = title, body = body, milestone = milestoneNumber, labels = labels)))
            }
            // Invalidate issues cache for this milestone
            if (milestoneNumber != null) {
                issuesCache.remove(milestoneNumber)
                _issues.value = _issues.value.toMutableMap().apply {
                    put(milestoneNumber, (this[milestoneNumber] ?: emptyList()) + result)
                }
            }
            result
        } catch (e: Exception) {
            val appError = e.toAppError()
            _error.value = appError
            Log.e(TAG, "createIssue failed", e)
            null
        }
    }

    suspend fun updateIssueState(issueNumber: Int, state: String): Issue? {
        _error.value = null
        return try {
            val result = withRetry("updateIssueState") {
                unwrap(apiClient.api.updateIssueState(UpdateIssueStateRequest(issueNumber = issueNumber, state = state)))
            }
            // Invalidate all issues caches (issue may have moved between milestones)
            issuesCache.clear()
            result
        } catch (e: Exception) {
            val appError = e.toAppError()
            _error.value = appError
            Log.e(TAG, "updateIssueState failed for #$issueNumber", e)
            null
        }
    }

    fun invalidateCache() {
        milestonesCache = null
        issuesCache.clear()
    }

    fun clearError() {
        _error.value = null
    }
}
