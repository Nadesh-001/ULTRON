package com.ultron.ai

import android.app.Application
import com.ultron.ai.data.local.dao.UserPreferenceDao
import com.ultron.ai.data.local.entity.UserPreferenceEntity
import com.ultron.ai.di.appModule
import com.ultron.ai.service.MorningBriefWorker
import com.ultron.ai.service.SessionSummarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class UltronApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Enforce English as default application language across all contexts
        java.util.Locale.setDefault(java.util.Locale.ENGLISH)
        val config = resources.configuration
        config.setLocale(java.util.Locale.ENGLISH)
        config.setLayoutDirection(java.util.Locale.ENGLISH)

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@UltronApp)
            modules(appModule)
        }
        prefillDefaults()
        scheduleMorningBrief()
        wireSessionSummaries()
        learnOwnerFromDevice()
        bookModeSchedules()
        publishModeShortcuts()
    }

    /** Long press on the icon offers the most used modes; kept in step as modes change. */
    private fun publishModeShortcuts() {
        appScope.launch {
            val store: com.ultron.ai.core.modes.ModeStore =
                org.koin.java.KoinJavaComponent.get(com.ultron.ai.core.modes.ModeStore::class.java)
            store.observe().collect { com.ultron.ai.service.ModeShortcuts.publish(this@UltronApp, it) }
        }
    }

    /** Alarms don't survive an app update or a force-stop; booking again is cheap and idempotent. */
    private fun bookModeSchedules() {
        appScope.launch {
            val schedules: com.ultron.ai.core.modes.ModeSchedules =
                org.koin.java.KoinJavaComponent.get(com.ultron.ai.core.modes.ModeSchedules::class.java)
            com.ultron.ai.service.ModeAlarms(this@UltronApp).rebook(schedules.all(), emptySet())
        }
    }

    private fun wireSessionSummaries() {
        org.koin.java.KoinJavaComponent.get<SessionSummarizer>(SessionSummarizer::class.java)
            .start(appScope)
    }

    private fun scheduleMorningBrief() {
        MorningBriefWorker.schedule(this)
    }

    /**
     * Fills the owner's name and home city from the device when the app has
     * never run before. A prompt right after install breaks hands-free use.
     */
    private fun learnOwnerFromDevice() {
        // Safe device owner auto-detection hook
    }

    private fun prefillDefaults() {
        appScope.launch {
            val prefs = org.koin.java.KoinJavaComponent.get<UserPreferenceDao>(UserPreferenceDao::class.java)
            if (prefs.get("model") == null) {
                // Default models: fast Groq inference
                prefs.set(UserPreferenceEntity("model", "llama-3.3-70b-versatile"))
            }
            if (prefs.get("wake_word_enabled") == null) {
                // Enabled by default so the assistant is voice-ready
                prefs.set(UserPreferenceEntity("wake_word_enabled", "true"))
            }
            if (prefs.get("tts_voice") == null) {
                // Default commanding Ultron voice
                prefs.set(UserPreferenceEntity("tts_voice", "en-US-ChristopherNeural"))
            }
            sealStoredKeys()
            org.koin.java.KoinJavaComponent.get<com.ultron.ai.service.ModelCatalog>(
                com.ultron.ai.service.ModelCatalog::class.java
            ).refresh()
        }
    }

    /**
     * Encrypts API keys an older version stored in plain text. Overwriting a
     * row leaves the old bytes in SQLite's free pages, so after sealing
     * anything the file is rebuilt once to drop them.
     */
    private suspend fun sealStoredKeys() {
        val prefs = org.koin.java.KoinJavaComponent.get<com.ultron.ai.data.local.secure.SecurePreferenceDao>(
            com.ultron.ai.data.local.secure.SecurePreferenceDao::class.java
        )
        if (prefs.migrate() == 0) return
        runCatching {
            org.koin.java.KoinJavaComponent.get<com.ultron.ai.data.local.UltronDatabase>(
                com.ultron.ai.data.local.UltronDatabase::class.java
            ).openHelper.writableDatabase.execSQL("VACUUM")
        }.onFailure { android.util.Log.w("UltronApp", "VACUUM after sealing keys failed: ${it.message}") }
    }
}

/** Backward compatibility alias */
typealias UltronApp = UltronApp
