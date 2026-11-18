package com.ultron.ai.core

import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmergencyStopController(
    private val ctx: Context,
    private val securityManager: SecurityManager,
    private val privacyManager: PrivacyManager
) {

    // ── State ────────────────────────────────────────────────────────────────
    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    private val _stopLog = MutableStateFlow<List<String>>(emptyList())
    val stopLog: StateFlow<List<String>> = _stopLog.asStateFlow()

    // ── Action name constants ────────────────────────────────────────────────
    companion object {
        const val ACTION_STOP_AUTOMATION  = "com.ultron.ai.STOP_AUTOMATION"
        const val ACTION_DISCONNECT       = "com.ultron.ai.DISCONNECT_SESSION"
        const val ACTION_RESET_AUTOMATION = "com.ultron.ai.RESET_AUTOMATION"
        const val WORKMANAGER_TAG         = "ultron_ai_task"
    }

    // ── Descriptive catalogue for UI ─────────────────────────────────────────
    val emergencyStopActions: List<String> = listOf(
        "Stop All Automation — sets the emergency-stopped flag and broadcasts " +
                "ACTION_STOP_AUTOMATION so all receivers cease activity immediately.",
        "Disable Voice Processing — stops the UltronWakeWordService foreground service " +
                "and clears the mic-recording session in PrivacyManager.",
        "Stop Background AI Tasks — cancels every WorkManager job tagged " +
                "\"$WORKMANAGER_TAG\", preventing queued or periodic AI work from running.",
        "Disconnect AI Sessions — broadcasts ACTION_DISCONNECT so active network " +
                "sessions (WebSocket, SSE, gRPC streams) tear down gracefully.",
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sets the emergency-stopped flag, broadcasts [ACTION_STOP_AUTOMATION]
     * via LocalBroadcastManager, and appends a timestamped log entry.
     */
    fun stopAllAutomation() {
        _isEmergencyStopped.value = true
        sendLocalBroadcast(ACTION_STOP_AUTOMATION)
        log("stopAllAutomation: emergency flag set; broadcast $ACTION_STOP_AUTOMATION sent")
    }

    /**
     * Stops the wake-word service, ends the mic session in [PrivacyManager],
     * and logs the action.
     */
    fun disableVoiceProcessing() {
        try {
            val serviceIntent = Intent().apply {
                setClassName(ctx.packageName, "com.ultron.ai.service.UltronWakeWordService")
            }
            ctx.stopService(serviceIntent)
        } catch (e: Exception) {
            log("disableVoiceProcessing: stopService error — ${e.message}")
        }
        privacyManager.endMicSession()
        log("disableVoiceProcessing: UltronWakeWordService stopped; mic session cleared")
    }

    /**
     * Cancels all WorkManager tasks carrying the tag [WORKMANAGER_TAG] and logs.
     */
    fun stopBackgroundAiTasks() {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(WORKMANAGER_TAG)
        log("stopBackgroundAiTasks: all WorkManager tasks with tag \"$WORKMANAGER_TAG\" cancelled")
    }

    /**
     * Broadcasts [ACTION_DISCONNECT] locally to signal all active AI sessions
     * to close their connections, then logs.
     */
    fun disconnectAiSessions() {
        sendLocalBroadcast(ACTION_DISCONNECT)
        log("disconnectAiSessions: broadcast $ACTION_DISCONNECT sent")
    }

    /**
     * Clears the emergency-stopped flag, broadcasts [ACTION_RESET_AUTOMATION],
     * and logs recovery.
     */
    fun resetAutomationState() {
        _isEmergencyStopped.value = false
        sendLocalBroadcast(ACTION_RESET_AUTOMATION)
        log("resetAutomationState: emergency flag cleared; system ready to resume; broadcast $ACTION_RESET_AUTOMATION sent")
    }

    /**
     * Executes all four stop operations in sequence, then records a
     * [SecurityEventType.EMERGENCY_STOP] event on [SecurityManager].
     *
     * Order: stopAllAutomation → disableVoiceProcessing →
     *        stopBackgroundAiTasks → disconnectAiSessions
     */
    fun fullEmergencyStop() {
        log("fullEmergencyStop: initiating full emergency stop sequence")
        stopAllAutomation()
        disableVoiceProcessing()
        stopBackgroundAiTasks()
        disconnectAiSessions()
        try {
            securityManager.logEvent(SecurityEventType.EMERGENCY_STOP, "Full emergency stop initiated")
        } catch (e: Exception) {
            log("fullEmergencyStop: could not record SecurityEvent — ${e.message}")
        }
        log("fullEmergencyStop: sequence complete")
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun log(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .format(Date())
        val entry = "[$timestamp] $message"
        _stopLog.value = _stopLog.value + entry
    }

    private fun sendLocalBroadcast(action: String) {
        val intent = Intent(action).setPackage(ctx.packageName)
        ctx.sendBroadcast(intent)
    }
}
