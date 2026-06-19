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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(this.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: KiloCodeApi = retrofit.create(KiloCodeApi::class.java)

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

        fun getInstance(baseUrl: String, sharedSecret: String): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(baseUrl, sharedSecret).also { INSTANCE = it }
            }
        }

        fun updateBaseUrl(baseUrl: String, sharedSecret: String) {
            synchronized(this) {
                INSTANCE = ApiClient(baseUrl, sharedSecret)
            }
        }
    }
}
