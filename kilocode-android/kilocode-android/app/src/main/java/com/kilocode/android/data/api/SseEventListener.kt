package com.kilocode.android.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener

class SseEventListener(
    private val onEvent: (String, Map<String, Any>) -> Unit,
    private val onError: (String) -> Unit = {},
    private val onClosed: () -> Unit = {},
) : EventSourceListener() {

    companion object {
        private const val TAG = "SSE"
        private val gson = Gson()
    }

    override fun onOpen(eventSource: EventSource, response: Response) {
        Log.d(TAG, "SSE connection opened")
    }

    override fun onEvent(
        eventSource: EventSource,
        id: String?,
        type: String?,
        data: String,
    ) {
        Log.d(TAG, "SSE event: type=$type")
        try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val eventData: Map<String, Any> = gson.fromJson(data, mapType)
            onEvent(type ?: "unknown", eventData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SSE event", e)
        }
    }

    override fun onClosed(eventSource: EventSource) {
        Log.d(TAG, "SSE connection closed")
        onClosed()
    }

    override fun onFailure(
        eventSource: EventSource,
        t: Throwable?,
        response: Response?,
    ) {
        Log.e(TAG, "SSE connection failed: ${t?.message}", t)
        onError(t?.message ?: "Unknown error")
    }
}
