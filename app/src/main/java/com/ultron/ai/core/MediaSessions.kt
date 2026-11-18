package com.ultron.ai.core

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.ultron.ai.service.ULTRONNotificationListener

/**
 * The media sessions players publish: which app is playing, and a handle to
 * press play or pause on it. Reading them needs notification-listener
 * access; without it [active] is null.
 */
class MediaSessions(private val context: Context) {

    private companion object {
        const val TAG = "MediaSessions"
    }

    /** Null when notification-listener access has not been granted. */
    fun active(): List<MediaController>? = try {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        manager.getActiveSessions(ComponentName(context, ULTRONNotificationListener::class.java))
    } catch (e: SecurityException) {
        Log.w(TAG, "No notification listener access: ${e.message}")
        null
    } catch (e: Exception) {
        Log.e(TAG, "Could not read media sessions: ${e.message}")
        emptyList()
    }

    fun of(packageName: String): MediaController? = active()?.firstOrNull { it.packageName == packageName }

    fun isPlaying(controller: MediaController): Boolean =
        controller.playbackState?.state == PlaybackState.STATE_PLAYING
}
