package com.ultron.ai.core

import android.Manifest
import android.app.AppOpsManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Domain types
// ---------------------------------------------------------------------------

enum class PermissionState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    NOT_APPLICABLE
}

data class PermissionStatus(
    val name: String,
    val androidName: String,
    val state: PermissionState,
    val isSpecial: Boolean = false
)

// ---------------------------------------------------------------------------
// PermissionManager
// ---------------------------------------------------------------------------

class PermissionManager(
    private val app: Context
) {

    // Coroutine scope for background work
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // SharedPreferences backing store for "has this permission ever been requested?"
    private val prefs by lazy {
        app.getSharedPreferences("ultron_perm_tracker", Context.MODE_PRIVATE)
    }

    // StateFlow of all permission statuses
    private val _permissionStateFlow = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissionStateFlow: StateFlow<List<PermissionStatus>> = _permissionStateFlow.asStateFlow()

    // -----------------------------------------------------------------------
    // Runtime permission helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the [PermissionState] for a standard Android runtime permission.
     *
     * Logic:
     *  - GRANTED            → ContextCompat.checkSelfPermission == PERMISSION_GRANTED
     *  - PERMANENTLY_DENIED → denied AND shouldShowRequestPermissionRationale==false AND
     *                         the permission has previously been requested (tracked via prefs)
     *  - DENIED             → denied but rationale should still be shown, OR never requested yet
     */
    fun checkRuntimePermission(ctx: Context, permission: String): PermissionState {
        val granted = ContextCompat.checkSelfPermission(ctx, permission) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) return PermissionState.GRANTED

        // Not granted. Determine DENIED vs PERMANENTLY_DENIED.
        val hasBeenRequested = hasPermissionBeenRequested(ctx, permission)
        val showRationale = shouldShowRationale(ctx, permission)

        return if (hasBeenRequested && !showRationale) {
            // User ticked "Don't ask again" or the system has suppressed the dialog
            PermissionState.PERMANENTLY_DENIED
        } else {
            PermissionState.DENIED
        }
    }

    /**
     * Wraps Activity.shouldShowRequestPermissionRationale so we can call it from a
     * non-Activity context (returns false when ctx is not an Activity – which is the
     * conservative / correct answer: treat as potentially permanently denied only when
     * hasBeenRequested==true).
     */
    private fun shouldShowRationale(ctx: Context, permission: String): Boolean {
        return try {
            val activity = ctx as? android.app.Activity ?: return false
            activity.shouldShowRequestPermissionRationale(permission)
        } catch (_: Exception) {
            false
        }
    }

    // -----------------------------------------------------------------------
    // Special / system permission checks
    // -----------------------------------------------------------------------

    /** Checks whether any enabled NotificationListenerService belongs to this package. */
    fun checkNotificationListenerEnabled(ctx: Context): PermissionState {
        val enabledListeners = Settings.Secure.getString(
            ctx.contentResolver,
            "enabled_notification_listeners"
        ) ?: return PermissionState.DENIED

        val packageName = ctx.packageName
        val components = enabledListeners.split(":")
        val enabled = components.any { it.trim().startsWith(packageName) }
        return if (enabled) PermissionState.GRANTED else PermissionState.DENIED
    }

    /**
     * Checks whether the given [serviceClass] (an AccessibilityService subclass) is
     * currently enabled for this package.
     */
    fun checkAccessibilityEnabled(ctx: Context, serviceClass: Class<*>): PermissionState {
        val expectedComponent = ComponentName(ctx.packageName, serviceClass.name).flattenToString()
        val enabledServices = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return PermissionState.DENIED

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedComponent, ignoreCase = true)) {
                return PermissionState.GRANTED
            }
        }
        return PermissionState.DENIED
    }

    /** Checks Usage Stats access via AppOpsManager. */
    fun checkUsageStatsEnabled(ctx: Context): PermissionState {
        return try {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    ctx.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    ctx.packageName
                )
            }
            if (mode == AppOpsManager.MODE_ALLOWED) PermissionState.GRANTED else PermissionState.DENIED
        } catch (_: Exception) {
            PermissionState.DENIED
        }
    }

    /** Checks Settings.canDrawOverlays (SYSTEM_ALERT_WINDOW). */
    fun checkSystemAlertWindow(ctx: Context): PermissionState {
        return if (Settings.canDrawOverlays(ctx)) PermissionState.GRANTED else PermissionState.DENIED
    }

    /** Checks Settings.System.canWrite (WRITE_SETTINGS). */
    fun checkWriteSettings(ctx: Context): PermissionState {
        return if (Settings.System.canWrite(ctx)) PermissionState.GRANTED else PermissionState.DENIED
    }

    // -----------------------------------------------------------------------
    // Aggregate status
    // -----------------------------------------------------------------------

    /**
     * Returns a [PermissionStatus] for every permission this app tracks.
     * Special/system permissions that require a [Class] argument (Accessibility) use a
     * sentinel NOT_APPLICABLE value here — callers that know the concrete service class
     * can query [checkAccessibilityEnabled] directly.
     */
    fun getAllPermissionStatuses(ctx: Context): List<PermissionStatus> {
        val statuses = mutableListOf<PermissionStatus>()

        // --- Runtime permissions ---
        val runtimePerms = listOf(
            "RECORD_AUDIO"         to Manifest.permission.RECORD_AUDIO,
            "CAMERA"               to Manifest.permission.CAMERA,
            "READ_CONTACTS"        to Manifest.permission.READ_CONTACTS,
            "CALL_PHONE"           to Manifest.permission.CALL_PHONE,
            "ACCESS_FINE_LOCATION" to Manifest.permission.ACCESS_FINE_LOCATION,
            "ACCESS_COARSE_LOCATION" to Manifest.permission.ACCESS_COARSE_LOCATION,
            "SEND_SMS"             to Manifest.permission.SEND_SMS,
            "READ_CALENDAR"        to Manifest.permission.READ_CALENDAR,
            "WRITE_CALENDAR"       to Manifest.permission.WRITE_CALENDAR,
            "BLUETOOTH_CONNECT"    to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Manifest.permission.BLUETOOTH_CONNECT else "BLUETOOTH_CONNECT_UNSUPPORTED",
            "BLUETOOTH_SCAN"       to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Manifest.permission.BLUETOOTH_SCAN else "BLUETOOTH_SCAN_UNSUPPORTED",
            "READ_MEDIA_IMAGES"    to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE,
            "READ_MEDIA_VIDEO"     to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE,
            "POST_NOTIFICATIONS"   to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.POST_NOTIFICATIONS else "POST_NOTIFICATIONS_UNSUPPORTED"
        )

        for ((friendlyName, androidPerm) in runtimePerms) {
            val state = if (androidPerm.endsWith("_UNSUPPORTED")) {
                // Permission doesn't exist on this API level – treat as GRANTED (system default)
                PermissionState.NOT_APPLICABLE
            } else {
                checkRuntimePermission(ctx, androidPerm)
            }
            statuses.add(
                PermissionStatus(
                    name = friendlyName,
                    androidName = androidPerm,
                    state = state,
                    isSpecial = false
                )
            )
        }

        // --- Special / system permissions ---
        statuses.add(
            PermissionStatus(
                name = "NOTIFICATION_LISTENER",
                androidName = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                state = checkNotificationListenerEnabled(ctx),
                isSpecial = true
            )
        )
        statuses.add(
            PermissionStatus(
                name = "ACCESSIBILITY_SERVICE",
                androidName = "android.permission.BIND_ACCESSIBILITY_SERVICE",
                // Cannot check without a concrete service class; callers use checkAccessibilityEnabled directly
                state = PermissionState.NOT_APPLICABLE,
                isSpecial = true
            )
        )
        statuses.add(
            PermissionStatus(
                name = "USAGE_STATS",
                androidName = "android.permission.PACKAGE_USAGE_STATS",
                state = checkUsageStatsEnabled(ctx),
                isSpecial = true
            )
        )
        statuses.add(
            PermissionStatus(
                name = "SYSTEM_ALERT_WINDOW",
                androidName = Manifest.permission.SYSTEM_ALERT_WINDOW,
                state = checkSystemAlertWindow(ctx),
                isSpecial = true
            )
        )
        statuses.add(
            PermissionStatus(
                name = "WRITE_SETTINGS",
                androidName = Manifest.permission.WRITE_SETTINGS,
                state = checkWriteSettings(ctx),
                isSpecial = true
            )
        )

        return statuses
    }

    // -----------------------------------------------------------------------
    // Settings deep-link intents
    // -----------------------------------------------------------------------

    /**
     * Returns the most specific [Intent] that leads the user to the right settings page
     * for the given [permission] (use the Manifest.permission.* constant or one of the
     * special names defined in [getAllPermissionStatuses]).
     */
    fun getSettingsIntentForPermission(permission: String, ctx: Context): Intent {
        return when (permission) {
            // ---- Notification Listener ----
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "NOTIFICATION_LISTENER" ->
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

            // ---- Accessibility ----
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "ACCESSIBILITY_SERVICE" ->
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

            // ---- Usage Stats ----
            "android.permission.PACKAGE_USAGE_STATS",
            "USAGE_STATS" ->
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

            // ---- System Alert Window ----
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            "SYSTEM_ALERT_WINDOW" ->
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                )

            // ---- Write Settings ----
            Manifest.permission.WRITE_SETTINGS,
            "WRITE_SETTINGS" ->
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${ctx.packageName}")
                )

            // ---- All runtime permissions → app details page ----
            else ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                }
        }
    }

    // -----------------------------------------------------------------------
    // First-request tracking
    // -----------------------------------------------------------------------

    /** Records that [permission] has been requested at least once. */
    fun markPermissionRequested(ctx: Context, permission: String) {
        prefs.edit().putBoolean(prefKey(permission), true).apply()
    }

    /** Returns `true` if [permission] has previously been requested. */
    fun hasPermissionBeenRequested(ctx: Context, permission: String): Boolean {
        return prefs.getBoolean(prefKey(permission), false)
    }

    private fun prefKey(permission: String) = "perm_requested_$permission"

    // -----------------------------------------------------------------------
    // StateFlow refresh
    // -----------------------------------------------------------------------

    /** Recomputes all permission statuses and emits the new list via [permissionStateFlow]. */
    fun refreshAll(ctx: Context) {
        scope.launch {
            val statuses = getAllPermissionStatuses(ctx)
            _permissionStateFlow.emit(statuses)
        }
    }
}
