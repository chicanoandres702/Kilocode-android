package com.kilocode.android.data.repository

import android.util.Log
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
import org.junit.Before
import org.mockito.Mockito
import org.mockito.MockedStatic

class SessionRepositoryTest {

    @Before
    fun setup() {
        // Mock android.util.Log to prevent RuntimeException in unit tests
        Mockito.mockStatic(Log::class.java).use { mockedLog ->
            // This needs to be done differently for static methods in JUnit
        }
    }
    
    // Actually, mocking static methods with Mockito needs a Rule or use MockK.
    // For simplicity, I'll modify SessionRepository to use a logging interface that is mockable.
    
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
        // The repository expects the event to have a "payload" field now, due to recent changes
        val eventData = """
        {
            "payload": {
                "type": "message.updated",
                "properties": {
                    "info": {
                        "id": "msg1",
                        "role": "assistant"
                    }
                }
            }
        }
        """.trimIndent()
        
        // Also simulate a part update, which is now handled separately
        val partData = """
        {
            "payload": {
                "type": "message.part.updated",
                "properties": {
                    "messageID": "msg1",
                    "part": {
                        "id": "part1",
                        "messageID": "msg1",
                        "type": "text",
                        "text": "Hello, world!"
                    }
                }
            }
        }
        """.trimIndent()

        // Use reflection to call the private method
        val method = SessionRepository::class.java.getDeclaredMethod("handleSseEvent", String::class.java, String::class.java)
        method.isAccessible = true
        method.invoke(repository, "message", eventData)
        method.invoke(repository, "message", partData)

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
