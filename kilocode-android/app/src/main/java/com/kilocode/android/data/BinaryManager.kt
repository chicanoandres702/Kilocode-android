package com.kilocode.android.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.io.FileOutputStream

object BinaryManager {
    private const val BINARY_NAME = "kilo_linux_arm64"
    private const val INTERNAL_BINARY_NAME = "kilo_binary"
    private var process: Process? = null

    var isServerRunning = mutableStateOf(false)
        private set

    val logs = mutableStateListOf<String>()

    private fun addLog(message: String) {
        logs.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $message")
        if (logs.size > 50) logs.removeAt(logs.size - 1)
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
        }

        binaryFile.setExecutable(true)
        return binaryFile
    }

    fun startServer(context: Context, serverUrl: String) {
        if (isServerRunning.value) return

        try {
            val file = prepareBinary(context)
            val processBuilder = ProcessBuilder(file.absolutePath, "serve", "--url", serverUrl)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()
            
            addLog("Server starting at $serverUrl...")
            
            // Check process status after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (process?.isAlive == true) {
                    isServerRunning.value = true
                    addLog("Server started successfully.")
                } else {
                    isServerRunning.value = false
                    val exitValue = process?.exitValue()
                    addLog("Server failed to start! (Exit code: $exitValue)")
                    
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        reader.forEachLine { line ->
                            addLog("Error: $line")
                        }
                    }
                }
            }, 500)

        } catch (e: Exception) {
            addLog("Failed to start server: ${e.message}")
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

            }
        }

        binaryFile.setExecutable(true)
        return binaryFile
    }

    fun startServer(context: Context, serverUrl: String) {
        if (isServerRunning) return

        try {
            val file = prepareBinary(context)
            val processBuilder = ProcessBuilder(file.absolutePath, "serve", "--url", serverUrl)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()
            
            // Check process status after a short delay to catch immediate crashes
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (process?.isAlive == true) {
                    isServerRunning = true
                    Log.d("KiloServer", "Server started with URL: $serverUrl")
                } else {
                    isServerRunning = false
                    val exitValue = process?.exitValue()
                    Log.e("KiloServer", "Server exited immediately with code: $exitValue")
                    
                    // Read output to get error message
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        reader.forEachLine { line ->
                            Log.e("KiloServer", "Binary Output: $line")
                        }
                    }
                }
            }, 500)

        } catch (e: Exception) {
            Log.e("KiloServer", "Failed to start Kilo server", e)
            isServerRunning = false
        }
    }

    }

    fun stopServer() {
        process?.destroy()
        process = null
        isServerRunning = false
        Log.d("KiloServer", "Server stopped")
    }
}
