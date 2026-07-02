package com.kilocode.android.data.repository

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kilocode.android.data.model.BackgroundTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TaskManagerRepository(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val _tasks = MutableStateFlow<List<BackgroundTask>>(emptyList())
    val tasks: StateFlow<List<BackgroundTask>> = _tasks

    suspend fun fetchBackgroundTasks() {
        val workInfos = workManager.getWorkInfosByTag("prompt").get()
        
        val backgroundTasks = workInfos.map { workInfo ->
            BackgroundTask(
                id = workInfo.id.toString(),
                command = "Prompt",
                description = workInfo.tags.firstOrNull { it.startsWith("prompt_") }?.removePrefix("prompt_") ?: "Background task",
                status = workInfo.state.name,
                startTime = 0,
                endTime = null
            )
        }
        _tasks.value = backgroundTasks
    }

    fun cancelTask(taskId: String) {
        workManager.cancelWorkById(java.util.UUID.fromString(taskId))
    }

    fun deleteTask(taskId: String) {
        workManager.cancelWorkById(java.util.UUID.fromString(taskId))
        workManager.pruneWork()
    }

    suspend fun retryTask(taskId: String) {
        val workInfo = workManager.getWorkInfoById(java.util.UUID.fromString(taskId)).get() ?: return
        val data = workInfo.outputData
        
        // Re-enqueue using PromptWorker.enqueue
        com.kilocode.android.worker.PromptWorker.enqueue(
            context,
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_SESSION_ID) ?: "",
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_PROMPT) ?: "",
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_AGENT),
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_MODEL_PROVIDER),
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_MODEL_ID),
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_DIRECTORY),
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_SERVER_URL) ?: "http://18.191.142.105:4096/",
            data.getString(com.kilocode.android.worker.PromptWorker.EXTRA_SHARED_SECRET) ?: ""
        )
    }
}
