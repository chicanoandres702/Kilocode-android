package com.kilocode.android.data.repository

import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class PlanningRepositoryTest {

    @Test
    fun testRepositoryInitialization() = runBlocking {
        val apiClient = mock(ApiClient::class.java)
        val apiService = mock(com.kilocode.android.data.api.KiloCodeApi::class.java)
        `when`(apiClient.api).thenReturn(apiService)

        val repository = PlanningRepository(apiClient)

        // Verify initial state
        assertTrue(repository.milestones.first().isEmpty())
        assertTrue(repository.issues.first().isEmpty())
        assertFalse(repository.isLoading.first())
        assertNull(repository.error.first())
    }

    @Test
    fun testClearError() = runBlocking {
        val apiClient = mock(ApiClient::class.java)
        val apiService = mock(com.kilocode.android.data.api.KiloCodeApi::class.java)
        `when`(apiClient.api).thenReturn(apiService)

        val repository = PlanningRepository(apiClient)

        // Verify clearError works
        repository.clearError()
        assertNull(repository.error.first())
    }

    @Test
    fun testInvalidateCache() = runBlocking {
        val apiClient = mock(ApiClient::class.java)
        val apiService = mock(com.kilocode.android.data.api.KiloCodeApi::class.java)
        `when`(apiClient.api).thenReturn(apiService)

        val repository = PlanningRepository(apiClient)

        // Verify invalidateCache works
        repository.invalidateCache()
        assertTrue(repository.milestones.first().isEmpty())
    }
}