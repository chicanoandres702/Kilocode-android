package com.kilocode.android.data.api

import android.util.Log
import com.kilocode.android.BuildConfig
import com.kilocode.android.data.AppError
import com.kilocode.android.data.httpError
import com.kilocode.android.data.isRetryable
import com.kilocode.android.data.model.*
import com.kilocode.android.data.retryDelay
import com.kilocode.android.data.toAppError
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(baseUrl: String, sharedSecret: String) {

    val baseUrl: String = baseUrl.removeSuffix("/") + "/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sharedSecret))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // Separate client for SSE that doesn't buffer the body via logging
    private val sseClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sharedSecret))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
            }
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No read timeout for streaming
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(this.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: KiloCodeApi = retrofit.create(KiloCodeApi::class.java)

    // ── Retry-with-backoff wrapper ─────────────────────────────────────────────
    // All API calls go through this to handle transient failures autonomously.

    companion object {
        private const val MAX_RETRIES = 3

        @Volatile
        private var INSTANCE: ApiClient? = null
        private var instanceBaseUrl: String? = null
        private var instanceSharedSecret: String? = null

        fun getInstance(baseUrl: String, sharedSecret: String): ApiClient {
            // Trim and sanitize URL
            val sanitizedBaseUrl = baseUrl.trim().removeSuffix("/") + "/"

            // Check for invalid URL format
            if (sanitizedBaseUrl.toHttpUrlOrNull() == null) {
                Log.e("ApiClient", "Invalid base URL: $sanitizedBaseUrl")
                return INSTANCE ?: ApiClient("http://localhost/", "") // Fallback
            }

            return synchronized(this) {
                if (
                    INSTANCE == null ||
                    instanceBaseUrl != sanitizedBaseUrl ||
                    instanceSharedSecret != sharedSecret
                ) {
                    INSTANCE = ApiClient(sanitizedBaseUrl, sharedSecret)
                    instanceBaseUrl = sanitizedBaseUrl
                    instanceSharedSecret = sharedSecret
                }
                INSTANCE!!
            }
        }

        fun updateBaseUrl(baseUrl: String, sharedSecret: String) {
            synchronized(this) {
                val normalizedBaseUrl = baseUrl.removeSuffix("/") + "/"
                if (
                    INSTANCE == null ||
                    instanceBaseUrl != normalizedBaseUrl ||
                    instanceSharedSecret != sharedSecret
                ) {
                    INSTANCE = ApiClient(normalizedBaseUrl, sharedSecret)
                    instanceBaseUrl = normalizedBaseUrl
                    instanceSharedSecret = sharedSecret
                }
            }
        }
    }

    /**
     * Execute a suspend block with exponential-backoff retry on transient errors.
     * Auth errors and unknown 4xx errors are NOT retried — they need user action.
     */
    private suspend fun <T> withRetry(
        operation: String,
        block: suspend () -> T
    ): T {
        var lastError: AppError? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val appError = e.toAppError()
                lastError = appError
                if (!appError.isRetryable()) {
                    throw e // Don't retry auth or unknown errors
                }
                val delayMs = appError.retryDelay(attempt)
                Log.w("ApiClient", "$operation failed (attempt ${attempt + 1}/$MAX_RETRIES): ${appError.message}. Retrying in ${delayMs}ms")
                kotlinx.coroutines.delay(delayMs)
            }
        }
        throw lastError ?: Exception("$operation failed after $MAX_RETRIES attempts")
    }

    suspend fun getSessions(directory: String? = null): List<Session> = withRetry("getSessions") {
        val response = api.listSessions(directory = directory)
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    suspend fun getMessages(sessionId: String, directory: String? = null): List<MessageWithParts> = withRetry("getMessages") {
        val response = api.listMessages(sessionId, directory = directory)
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    suspend fun sendPrompt(sessionId: String, prompt: String, agent: String? = null, model: ModelInfo? = null, directory: String? = null) = withRetry("sendPrompt") {
        val request = PromptRequest(
            parts = listOf(PartRequest(type = "text", text = prompt)),
            agent = agent,
            model = model
        )
        val response = api.sendPrompt(sessionId, directory = directory, request = request)
        if (!response.isSuccessful) {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    suspend fun listAgents(): List<Agent> = withRetry("listAgents") {
        val response = api.listAgents()
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    suspend fun cloneRepo(action: String, repo: String): RepoOperationResponse = withRetry("cloneRepo") {
        val request = CloneRepoRequest(action = action, repo = repo)
        val response = api.repoOperation(request)
        if (response.isSuccessful) {
            response.body() ?: RepoOperationResponse(success = false, error = "Empty response")
        } else {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    suspend fun listRepos(): List<RepoEntry> = withRetry("listRepos") {
        val response = api.listRepos()
        if (response.isSuccessful) {
            response.body()?.repos ?: emptyList()
        } else {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    suspend fun searchGitHubRepos(query: String): List<RepoEntry> = withRetry("searchRepos") {
        val response = api.searchRepos(query)
        if (response.isSuccessful) {
            response.body()?.repos ?: emptyList()
        } else {
            throw httpError(response.code(), response.errorBody()?.string() ?: "")
        }
    }

    fun createStreamCall(path: String): okhttp3.Call {
        val request = Request.Builder()
            .url("${baseUrl}${path}")
            .header("Accept", "text/event-stream")
            .build()
        return sseClient.newCall(request)
    }

    fun createEventSource(
        path: String,
        listener: EventSourceListener,
    ): EventSource {
        val request = Request.Builder()
            .url("${baseUrl}${path}")
            .build()
        val factory = EventSources.createFactory(okHttpClient)
        return factory.newEventSource(request, listener)
    }
}
