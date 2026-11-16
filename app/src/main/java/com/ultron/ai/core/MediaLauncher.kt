package com.ultron.ai.core

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.ultron.ai.domain.model.CameraMode

/**
 * Camera, voice recorder, and system settings screens.
 *
 * Everything here launches something and says honestly what happened. Where
 * the platform blocks the thing the user actually asked for — a photo taken
 * without touching the phone, an NFC toggle — the reply says so instead of
 * reporting success for an action that did not occur.
 */
class MediaLauncher(private val context: Context) {

    private companion object {
        const val TAG = "MediaLauncher"

        /** Toggles Android no longer lets any third-party app flip. */
        val UNTOGGLEABLE = setOf(
            SettingsScreens.Screen.NFC,
            SettingsScreens.Screen.WIFI,
            SettingsScreens.Screen.BLUETOOTH,
            SettingsScreens.Screen.MOBILE_DATA,
            SettingsScreens.Screen.AIRPLANE,
            SettingsScreens.Screen.HOTSPOT,
            SettingsScreens.Screen.LOCATION
        )

        /**
         * Undocumented but honoured by every major camera app, Samsung's
         * included: which lens to open on. There is no public API for it.
         */
        const val EXTRA_CAMERA_FACING = "android.intent.extras.CAMERA_FACING"
        const val CAMERA_FACING_FRONT = 1

        /** Samsung/Google use this alongside the above; harmless elsewhere. */
        const val EXTRA_USE_FRONT_CAMERA = "android.intent.extra.USE_FRONT_CAMERA"
    }

    fun openCamera(mode: CameraMode): String = try {
        val intent = when (mode) {
            CameraMode.VIDEO -> Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            CameraMode.PHOTO -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            CameraMode.SELFIE -> Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(EXTRA_CAMERA_FACING, CAMERA_FACING_FRONT)
                putExtra(EXTRA_USE_FRONT_CAMERA, true)
            }
            CameraMode.JUST_OPEN -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        }
        launch(intent)
        when (mode) {
            CameraMode.PHOTO -> "Camera ready — press shutter"
            CameraMode.SELFIE -> "Front camera ready"
            CameraMode.VIDEO -> "Video camera ready — press record"
            CameraMode.JUST_OPEN -> "Opening camera"
        }
    } catch (e: Exception) {
        Log.e(TAG, "Camera launch failed: ${e.message}")
        "Couldn't open camera"
    }

    fun recordAudio(): String = try {
        launch(Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION))
        "Opening voice recorder"
    } catch (e: Exception) {
        Log.e(TAG, "Recorder launch failed: ${e.message}")
        "No voice recorder app found on device"
    }

    /**
     * Opens the settings screen [phrase] names.
     */
    fun openSettings(phrase: String): String {
        val screen = SettingsScreens.match(phrase) ?: SettingsScreens.Screen.ROOT
        return try {
            launch(Intent(screen.action))
            if (screen in UNTOGGLEABLE) {
                "Android doesn't allow apps to toggle ${screen.label} directly — opened settings"
            } else {
                "Opening ${screen.label}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Settings screen ${screen.name} failed: ${e.message}")
            try {
                launch(Intent(Settings.ACTION_SETTINGS))
                "This settings screen isn't available — opened main settings"
            } catch (_: Exception) {
                "Couldn't open settings"
            }
        }
    }

    private fun launch(intent: Intent) {
        // Started from a service, so there is no task to attach to.
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
