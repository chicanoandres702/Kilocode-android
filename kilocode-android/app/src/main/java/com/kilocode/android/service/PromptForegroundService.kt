package com.kilocode.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kilocode.android.MainActivity
import com.kilocode.android.R
import com.kilocode.android.data.api.ApiClient
import com.kilocode.android.data.model.ModelInfo
import com.kilocode.android.data.model.ModelOption
import com.kilocode.android.data.model.PartRequest
import com.kilocode.android.data.model.PromptRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps prompt generation running when the app is in the background
 * or the screen is off. Shows a persistent notification with progress and a cancel action.
 */
class PromptForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        val prompt = intent?.getStringExtra(EXTRA_PROMPT)
        val agent = intent?.getStringExtra(EXTRA_AGENT)
        val modelProvider = intent?.getStringExtra(EXTRA_MODEL_PROVIDER)
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID)
        val directory = intent?.getStringExtra(EXTRA_DIRECTORY)
        val serverUrl = intent?.getStringExtra(EXTRA_SERVER_URL) ?: DEFAULT_SERVER_URL
        val sharedSecret = intent?.getStringExtra(EXTRA_SHARED_SECRET) ?: ""

        if (sessionId == null || prompt == null) {
            Log.e(TAG, "Missing sessionId or prompt extra")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Sending prompt...", 0))

        serviceScope.launch {
            try {
                val apiClient = ApiClient.getInstance(serverUrl, sharedSecret)
                updateNotification("Generating response...", 30)

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
                    updateNotification("Prompt completed successfully", 100)
                    // Wait a moment so user can see the completion notification
                    kotlinx.coroutines.delay(2000)
                } else {
                    updateNotification("Prompt failed: HTTP ${response.code()}", 0)
                    kotlinx.coroutines.delay(3000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in foreground service", e)
                updateNotification("Error: ${e.message}", 0)
                kotlinx.coroutines.delay(3000)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        createNotificationChannel()

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PromptForegroundService::class.java).apply {
                action = ACTION_CANCEL
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kilo Code")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text, progress))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prompt Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when generating responses in the background"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "PromptForegroundService"
        private const val CHANNEL_ID = "kilo_prompt_generation"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_AGENT = "agent"
        const val EXTRA_MODEL_PROVIDER = "model_provider"
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_DIRECTORY = "directory"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_SHARED_SECRET = "shared_secret"
        const val ACTION_CANCEL = "cancel"

        private const val DEFAULT_SERVER_URL = "http://18.191.142.105:4096/"

        fun start(context: Context, sessionId: String, prompt: String, agent: String?,
                  model: ModelOption?, directory: String?, serverUrl: String, sharedSecret: String) {
            val intent = Intent(context, PromptForegroundService::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_PROMPT, prompt)
                putExtra(EXTRA_AGENT, agent)
                putExtra(EXTRA_MODEL_PROVIDER, model?.providerID)
                putExtra(EXTRA_MODEL_ID, model?.modelID)
                putExtra(EXTRA_DIRECTORY, directory)
                putExtra(EXTRA_SERVER_URL, serverUrl)
                putExtra(EXTRA_SHARED_SECRET, sharedSecret)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
