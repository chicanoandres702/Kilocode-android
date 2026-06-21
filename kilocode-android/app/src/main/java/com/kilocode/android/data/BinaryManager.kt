package com.kilocode.android.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object BinaryManager {
    private const val BINARY_NAME = "kilo_linux_arm64"
    private const val INTERNAL_BINARY_NAME = "kilo_binary"
    private var process: Process? = null

    var isServerRunning = mutableStateOf(false)
        private set

    val logs = mutableStateListOf<String>()

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logs.add(0, "[$timestamp] $message")
        if (logs.size > 50) logs.removeAt(logs.size - 1)
    }

    private fun startLogStreaming() {
        thread(start = true) {
            try {
                // Simplified HTTP POST streaming
                while (isServerRunning.value) {
                    if (logs.isNotEmpty()) {
                        val log = logs.first()
                        try {
                            val url = URL("http://18.227.97.23:9000")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.doOutput = true
                            conn.outputStream.use { it.write(log.toByteArray()) }
                            conn.responseCode // Triggers the request
                        } catch (e: Exception) {
                            Log.e("BinaryManager", "Log stream failed: ${e.message}")
                        }
                        
                        Thread.sleep(1000) // Lower frequency for HTTP POST
                    } else {
                        Thread.sleep(1000)
                    }
                }
            } catch (e: Exception) {
                addLog("Log stream thread failed: ${e.message}")
            }
        }
    }

    fun prepareBinary(context: Context): File {
        val binaryFile = File(context.filesDir, INTERNAL_BINARY_NAME)
        if (!binaryFile.exists()) {
            addLog("Extracting binary...")
            context.assets.open(BINARY_NAME).use { inputStream ->
                FileOutputStream(binaryFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            addLog("Binary extracted.")
        }
        try {
            Runtime.getRuntime().exec("chmod 755 ${binaryFile.absolutePath}").waitFor()
        } catch (e: Exception) {
            addLog("Failed to chmod: ${e.message}")
        }
        return binaryFile
    }

    fun startServer(context: Context, serverUrl: String, autonomousMode: Boolean = false) {
        if (isServerRunning.value) return
        try {
            addLog("Preparing binary...")
            val file = prepareBinary(context)
            val modeLabel = if (autonomousMode) "autonomous mode" else "interactive mode"
            addLog("Starting server at $serverUrl ($modeLabel)...")
            val arguments = mutableListOf(file.absolutePath, "serve", "--url", serverUrl)
            if (autonomousMode) {
                arguments += "--auto"
            }
            val processBuilder = ProcessBuilder(arguments)
            
            val env = processBuilder.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["XDG_DATA_HOME"] = context.filesDir.absolutePath
            
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (process?.isAlive == true) {
                    isServerRunning.value = true
                    addLog("Server started successfully.")
                    startLogStreaming() // Start streaming after successful start
                } else {
                    isServerRunning.value = false
                    addLog("Server failed (Process dead).")
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        reader.forEachLine { line ->
                            addLog("Error: $line")
                        }
                    }
                }
            }, 500)
            
            isServerRunning.value = true
            addLog("Server process started.")
            
        } catch (e: Exception) {
            addLog("Failed to start: ${e.message}")
            isServerRunning.value = false
        }
    }

    fun stopServer() {
        process?.destroy()
        process = null
        isServerRunning.value = false
        addLog("Server stopped.")
    }
}
