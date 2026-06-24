package com.kilocode.android.data.repository

import com.google.gson.Gson
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.Message
import com.kilocode.android.data.model.Part
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class SessionRepositoryTest {

    @Test
    fun testHandleSseEventMessageUpdated() = runBlocking {
        // Create a mock ApiClient
        // For unit tests, we need to mock the ApiClient structure
        val apiClient = mock(ApiClient::class.java)
        // We also need to mock the Retrofit service it contains
        val apiService = mock(com.kilocode.android.data.api.KiloCodeApi::class.java)
        `when`(apiClient.api).thenReturn(apiService)
        
        val repository = SessionRepository(apiClient)

        // Simulate an SSE "message.updated" event
        val eventData = """
        {
            "properties": {
                "info": {
                    "id": "msg1",
                    "role": "assistant"
                },
                "parts": [
                    {
                        "id": "part1",
                        "messageID": "msg1",
                        "type": "text",
                        "text": "Hello, world!"
                    }
                ]
            }
        }
        """.trimIndent()

        // Use reflection to call the private method
        val method = SessionRepository::class.java.getDeclaredMethod("handleSseEvent", String::class.java, String::class.java)
        method.isAccessible = true
        method.invoke(repository, "message.updated", eventData)

        // Verify the state
        val messages = repository.messages.first()
        assertEquals(1, messages.size)
        assertEquals("msg1", messages[0].id)
        assertEquals("assistant", messages[0].role)

        val partsMap = repository.parts.first()
        assertEquals(1, partsMap["msg1"]?.size)
        assertEquals("Hello, world!", partsMap["msg1"]?.get(0)?.text)
    }
}
