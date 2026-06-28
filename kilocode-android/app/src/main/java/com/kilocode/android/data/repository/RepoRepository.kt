package com.kilocode.android.data.repository

import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.RepoEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class RepoRepository(
    private val apiClient: ApiClient
) {
    private val _clonedRepos = MutableStateFlow<List<RepoEntry>>(emptyList())
    val clonedRepos: StateFlow<List<RepoEntry>> = _clonedRepos.asStateFlow()

    private val _currentRepo = MutableStateFlow<String?>(null)
    val currentRepo: StateFlow<String?> = _currentRepo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setCurrentRepo(repo: String?) {
        _currentRepo.value = repo
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun cloneRepo(repo: String): Result<String> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null

        try {
            val result = apiClient.cloneRepo("clone", repo)

            if (result.success) {
                val repoName = repo.replace("/", "_")
                val current = _clonedRepos.value.toMutableList()
                // Remove any existing entry for this repo and add as local
                current.removeAll { it.name == repoName }
                current.add(RepoEntry(name = repoName, source = "local"))
                _clonedRepos.value = current
                Result.success(repoName)
            } else {
                val errMsg = result.error ?: "Unknown error"
                _error.value = errMsg
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun reopenRepo(repo: String): Result<String> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null

        try {
            val result = apiClient.cloneRepo("reopen", repo)

            if (result.success) {
                val repoName = repo.replace("/", "_")
                Result.success(repoName)
            } else {
                val errMsg = result.error ?: "Not found"
                if (errMsg.contains("not found", ignoreCase = true)) {
                    _error.value = "Repository not cloned. Clone first."
                } else {
                    _error.value = errMsg
                }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun listLocalRepos(): List<RepoEntry> = withContext(Dispatchers.IO) {
        try {
            val repos = apiClient.listRepos()
            _clonedRepos.value = repos
            repos
        } catch (e: Exception) {
            _error.value = e.message
            _clonedRepos.value
        }
    }

    suspend fun searchGitHubRepos(query: String): List<RepoEntry> = withContext(Dispatchers.IO) {
        try {
            val results = apiClient.searchGitHubRepos(query)
            results
        } catch (e: Exception) {
            _error.value = e.message
            emptyList()
        }
    }
}
