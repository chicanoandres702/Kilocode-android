package com.kilocode.android.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object BinaryManager {
    private const val BINARY_NAME = "kilo_linux_arm64"
    private const val INTERNAL_BINARY_NAME = "kilo_binary"
    private var process: Process? = null

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
        if (process?.isAlive == true) return

        try {
            val file = prepareBinary(context)
            // Assuming 'serve' is the command to deploy
            val processBuilder = ProcessBuilder(file.absolutePath, "serve", "--url", serverUrl)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()

            Log.d("KiloServer", "Server started with URL: $serverUrl")
        } catch (e: Exception) {
            Log.e("KiloServer", "Failed to start Kilo server", e)
        }
    }

    fun stopServer() {
        process?.destroy()
        process = null
        Log.d("KiloServer", "Server stopped")
    }
}
