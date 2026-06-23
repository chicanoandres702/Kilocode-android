package com.kilocode.android.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(this.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: KiloCodeApi = retrofit.create(KiloCodeApi::class.java)

    fun createStreamCall(path: String): okhttp3.Call {
        val request = Request.Builder()
            .url("${baseUrl}${path}")
            .header("Accept", "text/event-stream")
            .build()
        return okHttpClient.newCall(request)
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
            val normalizedBaseUrl = baseUrl.removeSuffix("/") + "/"
            return synchronized(this) {
                if (
                    INSTANCE == null ||
                    instanceBaseUrl != normalizedBaseUrl ||
                    instanceSharedSecret != sharedSecret
                ) {
                    INSTANCE = ApiClient(normalizedBaseUrl, sharedSecret)
                    instanceBaseUrl = normalizedBaseUrl
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
