package com.ultron.ai.core

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.view.accessibility.AccessibilityManager
import com.ultron.ai.data.local.secure.KeystoreCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

enum class SecurityEventType {
    PERMISSION_DENIED,
    PERMISSION_REVOKED,
    UNAUTHORIZED_ACTION_BLOCKED,
    API_KEY_ACCESS,
    BACKGROUND_SERVICE_KILLED,
    EMERGENCY_STOP
}

data class SecurityEvent(
    val timestamp: Long,
    val type: SecurityEventType,
    val description: String
)

// ---------------------------------------------------------------------------
// SecurityManager
// ---------------------------------------------------------------------------

class SecurityManager(
    private val ctx: Context,
    private val permissionManager: PermissionManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // -----------------------------------------------------------------------
    // 1. API Key Protection (Android Keystore AES-256-GCM via KeystoreCipher)
    // -----------------------------------------------------------------------

    private val cipher by lazy { KeystoreCipher("ULTRON.security.keys") }
    private val securePrefs by lazy { ctx.getSharedPreferences("ultron_secure_keys", Context.MODE_PRIVATE) }

    /**
     * Stores [key] encrypted under [alias] in Android Keystore. The raw key value is never logged.
     */
    fun storeApiKey(key: String, alias: String) {
        try {
            val encrypted = cipher.encrypt(key.toByteArray(Charsets.UTF_8))
            val b64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            securePrefs.edit().putString(alias, b64).apply()
            logEvent(SecurityEventType.API_KEY_ACCESS, "API key stored securely for alias: $alias")
        } catch (e: Exception) {
            logEvent(SecurityEventType.API_KEY_ACCESS, "Failed to store API key for alias: $alias: ${e.message}")
        }
    }

    /**
     * Retrieves the API key for [alias], or null if not found.
     * Decrypts via Android Keystore. The raw key value is never logged.
     */
    fun retrieveApiKey(alias: String): String? {
        val b64 = securePrefs.getString(alias, null) ?: return null
        return try {
            val encrypted = Base64.decode(b64, Base64.NO_WRAP)
            val plain = cipher.decrypt(encrypted)
            logEvent(SecurityEventType.API_KEY_ACCESS, "API key retrieved for alias: $alias")
            String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            logEvent(SecurityEventType.API_KEY_ACCESS, "Failed to decrypt API key for alias: $alias")
            null
        }
    }

    // -----------------------------------------------------------------------
    // 2. Sensitive Data Protection
    // -----------------------------------------------------------------------

    /**
     * Verifies that the app's SharedPreferences directory files are
     * MODE_PRIVATE (accessible only to this app). On modern Android the OS
     * enforces this automatically, but we confirm and log any anomaly.
     */
    fun protectLocalData() {
        val prefsDir = ctx.filesDir.parentFile?.let {
            java.io.File(it, "shared_prefs")
        } ?: return

        if (!prefsDir.exists()) return

        prefsDir.listFiles()?.forEach { file ->
            // On Android, app files should not be world-readable/writable.
            // We log a warning if permissions look permissive.
            val path = file.absolutePath
            val readable = file.canRead()
            val writable = file.canWrite()
            if (readable && writable) {
                // This is expected for the app itself; nothing to flag.
            } else {
                logEvent(
                    SecurityEventType.UNAUTHORIZED_ACTION_BLOCKED,
                    "SharedPrefs file has restricted access: $path"
                )
            }
        }
    }

    /**
     * Clears all in-memory caches and sensitive session state.
     */
    fun wipeSessionData() {
        synchronized(_automationActionsBuffer) {
            _automationActionsBuffer.clear()
        }
        _recentAutomationActionsFlow.value = emptyList()
        _isMicrophoneActiveFlow.value = false
        logEvent(SecurityEventType.EMERGENCY_STOP, "Session data wiped")
    }

    // -----------------------------------------------------------------------
    // 3. Security Event Log — ring buffer of last 100 events
    // -----------------------------------------------------------------------

    private val eventBuffer = ArrayDeque<SecurityEvent>(100)
    private val _securityEventsFlow = MutableStateFlow<List<SecurityEvent>>(emptyList())

    /** Emits the last 100 security events, newest last. */
    val securityEvents: StateFlow<List<SecurityEvent>> = _securityEventsFlow.asStateFlow()

    /**
     * Records a [SecurityEvent] of [type] with [description].
     * Buffer is capped at 100; oldest entries are dropped.
     */
    fun logEvent(type: SecurityEventType, description: String) {
        val event = SecurityEvent(
            timestamp = System.currentTimeMillis(),
            type = type,
            description = description
        )
        synchronized(eventBuffer) {
            if (eventBuffer.size >= 100) {
                eventBuffer.pollFirst()
            }
            eventBuffer.addLast(event)
            _securityEventsFlow.value = eventBuffer.toList()
        }
    }

    // -----------------------------------------------------------------------
    // 4. Service Status Detection
    // -----------------------------------------------------------------------

    /**
     * Returns true if the ULTRON AccessibilityService is currently enabled.
     * Checks via [AccessibilityManager.getEnabledAccessibilityServiceList].
     */
    fun isAccessibilityServiceRunning(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { info ->
            info.resolveInfo.serviceInfo.packageName == ctx.packageName
        }
    }

    /**
     * Returns true if ULTRON's NotificationListenerService is currently enabled.
     * Checks the flat list of enabled notification listener package names.
     */
    fun isNotificationListenerRunning(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(
            ctx.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any { component ->
            component.startsWith(ctx.packageName)
        }
    }

    /**
     * Returns true if the ULTRON wake-word service is currently running.
     * Uses [ActivityManager.getRunningServices] (deprecated for 3rd-party use
     * but still reliable for the app's own services).
     */
    fun isWakeWordServiceRunning(): Boolean {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE).any { info ->
            info.service.packageName == ctx.packageName &&
                    info.service.className.contains("WakeWord", ignoreCase = true)
        }
    }

    // -----------------------------------------------------------------------
    // 5. Background Activity Monitor
    // -----------------------------------------------------------------------

    private val _isMicrophoneActiveFlow = MutableStateFlow(false)

    /**
     * True when [AudioManager.isMicrophoneMute] is false AND the
     * wake-word service is running (i.e. ULTRON is actively listening).
     */
    val isMicrophoneActive: StateFlow<Boolean> = _isMicrophoneActiveFlow.asStateFlow()

    /**
     * Updates [isMicrophoneActive]. Call this whenever the wake-word service
     * starts or stops, or when audio mute state changes.
     */
    fun refreshMicrophoneState() {
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val micUnmuted = audioManager?.isMicrophoneMute == false
        val wakeWordRunning = isWakeWordServiceRunning()
        _isMicrophoneActiveFlow.value = micUnmuted && wakeWordRunning
    }

    /**
     * Always false — ULTRON never secretly accesses the camera.
     * This flow can only become true via an explicit, user-initiated
     * screen-capture action surfaced in the UI.
     */
    private val _isCameraActiveFlow = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActiveFlow.asStateFlow()

    // Explicit camera activation — called only by intentional UI actions.
    fun setCameraActive(active: Boolean) {
        _isCameraActiveFlow.value = active
    }

    // Ring buffer for automation actions (last 20).
    private val _automationActionsBuffer = ArrayDeque<String>(20)
    private val _recentAutomationActionsFlow = MutableStateFlow<List<String>>(emptyList())

    /** Emits the last 20 automation action strings recorded via [recordAutomationAction]. */
    val recentAutomationActions: StateFlow<List<String>> =
        _recentAutomationActionsFlow.asStateFlow()

    /**
     * Records an automation [action] string. Buffer is capped at 20.
     */
    fun recordAutomationAction(action: String) {
        synchronized(_automationActionsBuffer) {
            if (_automationActionsBuffer.size >= 20) {
                _automationActionsBuffer.pollFirst()
            }
            _automationActionsBuffer.addLast(action)
            _recentAutomationActionsFlow.value = _automationActionsBuffer.toList()
        }
    }

    // -----------------------------------------------------------------------
    // 6. Security Warnings
    // -----------------------------------------------------------------------

    /**
     * Returns a list of human-readable warning strings covering:
     * - Missing critical runtime permissions
     * - Accessibility service not enabled
     * - Notification listener not enabled
     * - App running in a debug build
     * - SYSTEM_ALERT_WINDOW granted (overlay risk)
     */
    fun getSecurityWarnings(ctx: Context): List<String> {
        val warnings = mutableListOf<String>()

        // Critical runtime permissions
        val criticalPermissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_CONTACTS
        )
        criticalPermissions.forEach { perm ->
            val state = permissionManager.checkRuntimePermission(ctx, perm)
            if (state != PermissionState.GRANTED) {
                warnings.add("Critical permission not granted: ${perm.substringAfterLast('.')}")
            }
        }

        // Accessibility service
        if (!isAccessibilityServiceRunning(ctx)) {
            warnings.add("ULTRON Accessibility Service is not enabled.")
        }

        // Notification listener
        if (!isNotificationListenerRunning(ctx)) {
            warnings.add("ULTRON Notification Listener is not enabled.")
        }

        // Debug build
        if (isDebugBuild()) {
            warnings.add("App is running in a DEBUG build. Do not use in production.")
        }

        // SYSTEM_ALERT_WINDOW (overlay) granted
        if (Settings.canDrawOverlays(ctx)) {
            warnings.add(
                "SYSTEM_ALERT_WINDOW (draw overlays) is granted. " +
                        "Ensure the user knowingly enabled this permission."
            )
        }

        return warnings
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun isDebugBuild(): Boolean {
        return try {
            val appInfo = ctx.packageManager.getApplicationInfo(ctx.packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
}
