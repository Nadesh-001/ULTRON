package com.ultron.ai.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PrivacyManager(
    private val ctx: Context
) {

    // ── Sensitive data access log ────────────────────────────────────────────
    private val _sensitiveDataTouched = MutableStateFlow<List<String>>(emptyList())
    val sensitiveDataTouched: StateFlow<List<String>> = _sensitiveDataTouched.asStateFlow()

    // ── Mic recording session ────────────────────────────────────────────────
    var micRecordingReason: String? = null
        private set

    // ── MediaProjection token presence flag ─────────────────────────────────
    private var mediaProjectionTokenPresent: Boolean = false

    // ── In-memory sensitive collections ─────────────────────────────────────
    private val sensitiveCache: MutableList<String> = mutableListOf()

    // ── Regex patterns ───────────────────────────────────────────────────────
    companion object {
        private val EMAIL_REGEX = Regex(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
        )
        private val PHONE_REGEX = Regex(
            """(?:(?:\+?1\s*(?:[.\-]\s*)?)?(?:\(?\d{3}\)?[\s.\-]?)?\d{3}[\s.\-]?\d{4})"""
        )
        private val CARD_REGEX = Regex(
            """\b(?:\d[ \-]?){13,19}\b"""
        )
        // Strings of 20+ non-whitespace characters that look like keys
        // (contain at least one digit and one letter, typical of API keys/tokens)
        private val API_KEY_REGEX = Regex(
            """(?<![a-zA-Z0-9])(?=[a-zA-Z0-9]*[a-zA-Z])(?=[a-zA-Z0-9]*[0-9])[a-zA-Z0-9_\-./+]{20,}(?![a-zA-Z0-9])"""
        )
    }

    // ────────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Redacts sensitive patterns from [input] before it reaches any log sink.
     * Redaction order matters: more-specific patterns run before generic ones.
     */
    fun sanitizeForLogging(input: String): String {
        var result = input
        result = EMAIL_REGEX.replace(result, "[EMAIL]")
        result = PHONE_REGEX.replace(result, "[PHONE]")
        result = CARD_REGEX.replace(result) { match ->
            // Only replace if the digit count (ignoring separators) is 13-19
            val digits = match.value.filter { it.isDigit() }
            if (digits.length in 13..19) "[CARD]" else match.value
        }
        result = API_KEY_REGEX.replace(result, "[API_KEY]")
        recordSensitiveAccess("log-sanitization")
        return result
    }

    /**
     * Returns true only when an explicit MediaProjection token is held by the
     * application. Defaults to false for safety.
     */
    fun isScreenCaptureAuthorized(): Boolean = mediaProjectionTokenPresent

    /** Call this when a MediaProjection token is obtained. */
    fun setMediaProjectionToken(present: Boolean) {
        mediaProjectionTokenPresent = present
        if (present) recordSensitiveAccess("screen-capture-token")
    }

    /**
     * Clears all in-memory data tagged as sensitive and resets the access log.
     */
    fun clearSensitiveCache() {
        sensitiveCache.clear()
        _sensitiveDataTouched.value = emptyList()
    }

    /**
     * Returns true only when a wake-word trigger or explicit user voice
     * command initiated the mic session (i.e. [micRecordingReason] is set).
     */
    fun isMicRecordingAuthorized(): Boolean = micRecordingReason != null

    /** Starts a mic session with the given [reason]. */
    fun beginMicSession(reason: String) {
        micRecordingReason = reason
        recordSensitiveAccess("mic-session:$reason")
    }

    /** Ends the current mic session and clears the authorization. */
    fun endMicSession() {
        micRecordingReason = null
    }

    /**
     * Returns a human-readable English summary of the current privacy state.
     */
    fun getPrivacyReport(): String = buildString {
        appendLine("=== ULTRON Privacy Report ===")
        appendLine()
        appendLine("Mic recording authorized : ${isMicRecordingAuthorized()}")
        if (isMicRecordingAuthorized()) {
            appendLine("  Reason                 : $micRecordingReason")
        }
        appendLine("Screen capture authorized: ${isScreenCaptureAuthorized()}")
        appendLine()
        val touched = _sensitiveDataTouched.value
        if (touched.isEmpty()) {
            appendLine("No sensitive data types accessed this session.")
        } else {
            appendLine("Sensitive data accessed this session (${touched.size} event(s)):")
            touched.forEach { appendLine("  • $it") }
        }
        appendLine()
        appendLine("Sensitive cache entries  : ${sensitiveCache.size}")
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun recordSensitiveAccess(dataType: String) {
        val entry = "[${System.currentTimeMillis()}] $dataType"
        _sensitiveDataTouched.value = _sensitiveDataTouched.value + entry
    }
}
