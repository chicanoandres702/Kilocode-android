package com.kilocode.android.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.io.FileOutputStream

object BinaryManager {
    private const val BINARY_NAME = "kilo_linux_arm64"
    private const val INTERNAL_BINARY_NAME = "kilo_binary"
    private var process: Process? = null

    var isServerRunning = mutableStateOf(false)
        private set

    fun prepareBinary(context: Context): File {
        val binaryFile = File(context.filesDir, INTERNAL_BINARY_NAME)

        if (!binaryFile.exists()) {
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
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (process?.isAlive == true) {
                    isServerRunning.value = true
                    Log.d("KiloServer", "Server started with URL: $serverUrl")
                } else {
                    isServerRunning.value = false
                    val exitValue = process?.exitValue()
                    Log.e("KiloServer", "Server exited immediately with code: $exitValue")
                    
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        reader.forEachLine { line ->
                            Log.e("KiloServer", "Binary Output: $line")
                        }
                    }
                }
            }, 500)

        } catch (e: Exception) {
            Log.e("KiloServer", "Failed to start Kilo server", e)
            isServerRunning.value = false
        }
    }

    fun stopServer() {
        process?.destroy()
        process = null
        isServerRunning.value = false
        Log.d("KiloServer", "Server stopped")
    }
}

