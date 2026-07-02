package com.kilocode.android.data.model

data class BackgroundTask(
    val id: String?,
    val command: String,
    val description: String?,
    val status: String, // running, completed, failed, cancelled
    val startTime: Long,
    val endTime: Long?,
)
