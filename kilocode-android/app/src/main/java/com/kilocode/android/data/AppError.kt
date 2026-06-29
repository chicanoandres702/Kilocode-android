package com.kilocode.android.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Classified error types for autonomous recovery decisions.
 * Each type determines the retry strategy and whether user intervention is needed.
 */
sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Network is unreachable — retry with backoff when connectivity returns */
    class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** Server returned 401/403 — auth token expired or invalid */
    class AuthError(message: String) : AppError(message)

    /** Server returned 5xx — transient server error, retry with backoff */
    class ServerError(val code: Int, message: String) : AppError(message)

    /** Rate limited (429) — retry after delay */
    class RateLimitError(val retryAfterMs: Long = 5000L) : AppError("Rate limited — retry after ${retryAfterMs}ms")

    /** Unknown/unrecoverable error */
    class UnknownError(message: String, cause: Throwable? = null) : AppError(message, cause)
}

/**
 * Classify a throwable into an AppError type for autonomous recovery decisions.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is SocketTimeoutException -> AppError.NetworkError("Connection timed out", this)
    is UnknownHostException -> AppError.NetworkError("Cannot reach server", this)
    is IOException -> AppError.NetworkError(message ?: "Network error", this)
    else -> AppError.UnknownError(message ?: "Unknown error", this)
}

/**
 * HTTP status codes mapped to AppError types.
 */
fun httpError(code: Int, body: String = ""): AppError = when (code) {
    401, 403 -> AppError.AuthError("Authentication failed ($code)")
    429 -> AppError.RateLimitError()
    in 500..599 -> AppError.ServerError(code, "Server error ($code): $body")
    in 400..499 -> AppError.UnknownError("Client error ($code): $body")
    else -> AppError.UnknownError("HTTP $code: $body")
}

/**
 * Returns true if this error type supports automatic retry.
 */
fun AppError.isRetryable(): Boolean = when (this) {
    is AppError.NetworkError -> true
    is AppError.ServerError -> true
    is AppError.RateLimitError -> true
    is AppError.AuthError -> false
    is AppError.UnknownError -> false
}

/**
 * Returns the recommended retry delay in milliseconds for this error type.
 */
fun AppError.retryDelay(attempt: Int): Long = when (this) {
    is AppError.NetworkError -> (1000L * (1 shl attempt)).coerceAtMost(30000L) // exponential 1s..30s
    is AppError.ServerError -> (2000L * (1 shl attempt)).coerceAtMost(60000L) // exponential 2s..60s
    is AppError.RateLimitError -> retryAfterMs
    else -> 5000L
}
