package com.kilocode.android.data.api

import android.util.Log
import com.kilocode.android.BuildConfig
import com.kilocode.android.data.model.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
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

    suspend fun getSessions(directory: String? = null): List<Session> {
        val response = api.listSessions(directory = directory)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        }
        throw Exception("Failed to list sessions: ${response.code()} ${response.message()}")
    }

    suspend fun getMessages(sessionId: String): List<MessageWithParts> {
        val response = api.listMessages(sessionId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        }
        throw Exception("Failed to list messages: ${response.code()}")
    }

    suspend fun sendPrompt(sessionId: String, prompt: String, agent: String? = null, model: ModelInfo? = null) {
        val request = PromptRequest(
            parts = listOf(PartRequest(type = "text", text = prompt)),
            agent = agent,
            model = model
        )
        val response = api.sendPrompt(sessionId, request)
        if (!response.isSuccessful) {
            throw Exception("Failed to send prompt: ${response.code()} ${response.message()}")
        }
    }

    suspend fun listAgents(): List<Agent> {
        val response = api.listAgents()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        }
        throw Exception("Failed to list agents: ${response.code()}")
    }

    suspend fun cloneRepo(action: String, repo: String): RepoOperationResponse {
        val request = CloneRepoRequest(action = action, repo = repo)
        val response = api.repoOperation(request)
        if (response.isSuccessful) {
            return response.body() ?: RepoOperationResponse(success = false, error = "Empty response")
        }
        throw Exception("Failed to $action repo: ${response.code()} ${response.message()}")
    }

    suspend fun listRepos(): List<RepoEntry> {
        val response = api.listRepos()
        if (response.isSuccessful) {
            return response.body()?.repos ?: emptyList()
        }
        throw Exception("Failed to list repos: ${response.code()}")
    }

    suspend fun searchGitHubRepos(query: String): List<RepoEntry> {
        val response = api.searchRepos(query)
        if (response.isSuccessful) {
            return response.body()?.repos ?: emptyList()
        }
        throw Exception("Failed to search GitHub repos: ${response.code()}")
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

    companion object {
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
}
