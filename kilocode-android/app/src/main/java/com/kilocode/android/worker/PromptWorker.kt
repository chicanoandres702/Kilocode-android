package com.kilocode.android.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.ModelInfo
import com.kilocode.android.data.model.PartRequest
import com.kilocode.android.data.model.PromptRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for reliable background prompt execution.
 * Survives app restarts and device reboots.
 */
class PromptWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessionId = inputData.getString(EXTRA_SESSION_ID) ?: return@withContext Result.failure()
        val prompt = inputData.getString(EXTRA_PROMPT) ?: return@withContext Result.failure()
        val agent = inputData.getString(EXTRA_AGENT)
        val modelProvider = inputData.getString(EXTRA_MODEL_PROVIDER)
        val modelId = inputData.getString(EXTRA_MODEL_ID)
        val directory = inputData.getString(EXTRA_DIRECTORY)
        val serverUrl = inputData.getString(EXTRA_SERVER_URL) ?: "http://18.191.142.105:4096/"
        val sharedSecret = inputData.getString(EXTRA_SHARED_SECRET) ?: ""

        setForegroundAsync(createForegroundInfo("Sending prompt..."))

        try {
            val apiClient = ApiClient.getInstance(serverUrl, sharedSecret)

            setForegroundAsync(createForegroundInfo("Generating response..."))

            val modelInfo = if (modelProvider != null && modelId != null) {
                ModelInfo(modelProvider, modelId)
            } else null

            val response = apiClient.api.sendPrompt(
                sessionId,
                directory = directory,
                request = PromptRequest(
                    parts = listOf(PartRequest(type = "text", text = prompt)),
                    agent = agent,
                    model = modelInfo
                )
            )

            if (response.isSuccessful) {
                showCompletionNotification("Prompt completed successfully")
                Result.success()
            } else {
                Log.e(TAG, "Prompt failed: HTTP ${response.code()}")
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    showCompletionNotification("Prompt failed after 3 attempts")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in PromptWorker", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                showCompletionNotification("Error: ${e.message}")
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Kilo Code")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Prompts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when generating responses in the background"
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showCompletionNotification(message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Kilo Code")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    companion object {
        private const val TAG = "PromptWorker"
        private const val CHANNEL_ID = "kilo_prompt_worker"
        private const val NOTIFICATION_ID = 2001

        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_AGENT = "agent"
        const val EXTRA_MODEL_PROVIDER = "model_provider"
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_DIRECTORY = "directory"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_SHARED_SECRET = "shared_secret"

        fun enqueue(
            context: Context,
            sessionId: String,
            prompt: String,
            agent: String?,
            modelProvider: String?,
            modelId: String?,
            directory: String?,
            serverUrl: String,
            sharedSecret: String
        ): Operation {
            val inputData = workDataOf(
                EXTRA_SESSION_ID to sessionId,
                EXTRA_PROMPT to prompt,
                EXTRA_AGENT to agent,
                EXTRA_MODEL_PROVIDER to modelProvider,
                EXTRA_MODEL_ID to modelId,
                EXTRA_DIRECTORY to directory,
                EXTRA_SERVER_URL to serverUrl,
                EXTRA_SHARED_SECRET to sharedSecret
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PromptWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("prompt")
                .addTag("prompt_$sessionId")
                .build()

            return WorkManager.getInstance(context).enqueue(request)
        }
    }
}
