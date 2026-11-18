package com.ultron.ai.controlcenter.failover

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Intelligent error classification for AI requests.
 * Distinguishes recoverable transient failures (429, 502, 503, 504, socket timeouts)
 * from non-recoverable configuration errors (401 invalid key, 403 forbidden, 400 bad payload).
 */
object ErrorClassifier {

    enum class ErrorCategory {
        RATE_LIMITED,        // HTTP 429 - Recoverable via backup model or backoff
        SERVICE_UNAVAILABLE, // HTTP 502, 503, 504 - Recoverable via backup model
        NETWORK_TIMEOUT,     // SocketTimeoutException - Recoverable
        CONNECTION_FAILED,   // UnknownHostException, IOException - Recoverable
        AUTHENTICATION_FAIL, // HTTP 401 - Non-recoverable without new credentials
        PERMISSION_DENIED,   // HTTP 403 - Non-recoverable
        INVALID_REQUEST,     // HTTP 400 - Non-recoverable
        UNKNOWN              // Non-recoverable generic error
    }

    data class ErrorAnalysis(
        val category: ErrorCategory,
        val isRecoverable: Boolean,
        val retryAfterSeconds: Int? = null,
        val userFriendlyMessage: String
    )

    fun classify(throwable: Throwable?, httpStatusCode: Int? = null): ErrorAnalysis {
        // 1. Analyze HTTP status code if present
        if (httpStatusCode != null) {
            when (httpStatusCode) {
                429 -> return ErrorAnalysis(
                    category = ErrorCategory.RATE_LIMITED,
                    isRecoverable = true,
                    userFriendlyMessage = "Rate limit reached. Automatically switching to backup model."
                )
                502, 503, 504 -> return ErrorAnalysis(
                    category = ErrorCategory.SERVICE_UNAVAILABLE,
                    isRecoverable = true,
                    userFriendlyMessage = "Provider server temporarily unavailable (HTTP $httpStatusCode)."
                )
                401 -> return ErrorAnalysis(
                    category = ErrorCategory.AUTHENTICATION_FAIL,
                    isRecoverable = false,
                    userFriendlyMessage = "Invalid API Key. Please verify your credentials in Settings."
                )
                403 -> return ErrorAnalysis(
                    category = ErrorCategory.PERMISSION_DENIED,
                    isRecoverable = false,
                    userFriendlyMessage = "Access forbidden. Your account does not have permission for this model."
                )
                400 -> return ErrorAnalysis(
                    category = ErrorCategory.INVALID_REQUEST,
                    isRecoverable = false,
                    userFriendlyMessage = "Invalid request format or incompatible model parameters."
                )
            }
        }

        // 2. Analyze Exception type
        return when (throwable) {
            is SocketTimeoutException -> ErrorAnalysis(
                category = ErrorCategory.NETWORK_TIMEOUT,
                isRecoverable = true,
                userFriendlyMessage = "Connection timed out. Retrying on standby model."
            )
            is UnknownHostException -> ErrorAnalysis(
                category = ErrorCategory.CONNECTION_FAILED,
                isRecoverable = true,
                userFriendlyMessage = "Unable to resolve host. Check your internet connection."
            )
            is IOException -> ErrorAnalysis(
                category = ErrorCategory.CONNECTION_FAILED,
                isRecoverable = true,
                userFriendlyMessage = "Network transport error occurred."
            )
            else -> ErrorAnalysis(
                category = ErrorCategory.UNKNOWN,
                isRecoverable = false,
                userFriendlyMessage = throwable?.localizedMessage ?: "Unknown AI request failure."
            )
        }
    }
}
