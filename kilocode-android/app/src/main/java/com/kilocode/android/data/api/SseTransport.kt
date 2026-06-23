package com.kilocode.android.data.api

import android.util.Log
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import okhttp3.OkHttpClient

class SseTransport(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val listener: EventSourceListener
) {
    private var eventSource: EventSource? = null

    fun connect(path: String) {
        cancel()
        val request = Request.Builder()
            .url("${baseUrl.removeSuffix("/")}/$path")
            .build()
        val factory = EventSources.createFactory(client)
        eventSource = factory.newEventSource(request, listener)
        Log.d("SseTransport", "Connecting to: ${request.url}")
    }

    fun cancel() {
        eventSource?.cancel()
        eventSource = null
    }
}
