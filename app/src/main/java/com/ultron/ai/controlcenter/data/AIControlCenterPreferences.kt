package com.ultron.ai.controlcenter.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences storage for AI Control Center routing policies,
 * model assignments, and failover parameters.
 */
class AIControlCenterPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ultron_ai_control_center_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_SWITCHING = "auto_model_switching"
        private const val KEY_SMART_ROUTING = "smart_model_routing"
        private const val KEY_RATE_LIMIT_MONITORING = "rate_limit_monitoring"
        private const val KEY_MAX_RETRIES = "max_retries"
        private const val KEY_PRIMARY_MODEL = "primary_chat_model"
        private const val KEY_BACKUP_MODEL = "backup_chat_model"
        private const val KEY_VISION_MODEL = "vision_model"
        private const val KEY_VOICE_MODEL = "voice_model"

        const val DEFAULT_PRIMARY_MODEL = "llama-3.3-70b-versatile"
        const val DEFAULT_BACKUP_MODEL = "llama-3.1-8b-instant"
        const val DEFAULT_VISION_MODEL = "llama-3.2-90b-vision-preview"
        const val DEFAULT_VOICE_MODEL = "whisper-large-v3-turbo"
    }

    data class ControlCenterConfig(
        val autoModelSwitching: Boolean,
        val smartRouting: Boolean,
        val rateLimitMonitoring: Boolean,
        val maxRetries: Int,
        val primaryChatModel: String,
        val backupChatModel: String,
        val visionModel: String,
        val voiceModel: String
    )

    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<ControlCenterConfig> = _config.asStateFlow()

    private fun readConfig(): ControlCenterConfig {
        return ControlCenterConfig(
            autoModelSwitching = prefs.getBoolean(KEY_AUTO_SWITCHING, true),
            smartRouting = prefs.getBoolean(KEY_SMART_ROUTING, true),
            rateLimitMonitoring = prefs.getBoolean(KEY_RATE_LIMIT_MONITORING, true),
            maxRetries = prefs.getInt(KEY_MAX_RETRIES, 2),
            primaryChatModel = prefs.getString(KEY_PRIMARY_MODEL, DEFAULT_PRIMARY_MODEL) ?: DEFAULT_PRIMARY_MODEL,
            backupChatModel = prefs.getString(KEY_BACKUP_MODEL, DEFAULT_BACKUP_MODEL) ?: DEFAULT_BACKUP_MODEL,
            visionModel = prefs.getString(KEY_VISION_MODEL, DEFAULT_VISION_MODEL) ?: DEFAULT_VISION_MODEL,
            voiceModel = prefs.getString(KEY_VOICE_MODEL, DEFAULT_VOICE_MODEL) ?: DEFAULT_VOICE_MODEL
        )
    }

    fun setAutoModelSwitching(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SWITCHING, enabled).apply()
        _config.value = readConfig()
    }

    fun setSmartRouting(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_ROUTING, enabled).apply()
        _config.value = readConfig()
    }

    fun setRateLimitMonitoring(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RATE_LIMIT_MONITORING, enabled).apply()
        _config.value = readConfig()
    }

    fun setMaxRetries(retries: Int) {
        prefs.edit().putInt(KEY_MAX_RETRIES, retries.coerceIn(1, 5)).apply()
        _config.value = readConfig()
    }

    fun setPrimaryModel(modelId: String) {
        prefs.edit().putString(KEY_PRIMARY_MODEL, modelId).apply()
        _config.value = readConfig()
    }

    fun setBackupModel(modelId: String) {
        prefs.edit().putString(KEY_BACKUP_MODEL, modelId).apply()
        _config.value = readConfig()
    }

    fun setVisionModel(modelId: String) {
        prefs.edit().putString(KEY_VISION_MODEL, modelId).apply()
        _config.value = readConfig()
    }

    fun setVoiceModel(modelId: String) {
        prefs.edit().putString(KEY_VOICE_MODEL, modelId).apply()
        _config.value = readConfig()
    }

    fun restoreDefaults() {
        prefs.edit().clear().apply()
        _config.value = readConfig()
    }
}
