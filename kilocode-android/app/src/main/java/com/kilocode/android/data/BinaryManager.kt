package com.kilocode.android.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream

object BinaryManager {
    private const val BINARY_NAME = "kilo_linux_arm64"
    private const val INTERNAL_BINARY_NAME = "kilo_binary"
    private var process: Process? = null

    var isServerRunning by mutableStateOf(false)
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
        if (isServerRunning) return

        try {
            val file = prepareBinary(context)
            val processBuilder = ProcessBuilder(file.absolutePath, "serve", "--url", serverUrl)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()
            isServerRunning = true

            Log.d("KiloServer", "Server started with URL: $serverUrl")
        } catch (e: Exception) {
            Log.e("KiloServer", "Failed to start Kilo server", e)
            isServerRunning = false
        }
    }

    fun stopServer() {
        process?.destroy()
        process = null
        isServerRunning = false
        Log.d("KiloServer", "Server stopped")
    }
}
