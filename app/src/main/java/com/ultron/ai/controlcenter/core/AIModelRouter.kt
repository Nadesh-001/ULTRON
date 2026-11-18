package com.ultron.ai.controlcenter.core

import com.ultron.ai.controlcenter.data.AIControlCenterPreferences
import com.ultron.ai.controlcenter.failover.ErrorClassifier

/**
 * Intelligent AI Model Router.
 * Analyzes request intent and required capabilities, routing to the optimal primary model
 * and validating strict capability compatibility before initiating failovers.
 */
class AIModelRouter(
    private val preferences: AIControlCenterPreferences
) {

    enum class RequestType {
        NORMAL_CHAT,
        TOOL_EXECUTION,
        IMAGE_VISION,
        AUDIO_TRANSCRIPTION,
        COMPLEX_REASONING,
        LIGHTWEIGHT
    }

    companion object {
        /**
         * Verified model capabilities registry.
         * Models alone do not declare their capabilities via simple list APIs,
         * so a verified registry ensures models are never tasked with incompatible work.
         */
        val VERIFIED_REGISTRY: Map<String, Set<ModelCapability>> = mapOf(
            "llama-3.3-70b-versatile" to setOf(
                ModelCapability.TEXT,
                ModelCapability.TOOL_CALLING,
                ModelCapability.REASONING,
                ModelCapability.STREAMING
            ),
            "llama-3.1-8b-instant" to setOf(
                ModelCapability.TEXT,
                ModelCapability.STREAMING
            ),
            "llama-3.2-90b-vision-preview" to setOf(
                ModelCapability.VISION,
                ModelCapability.TEXT,
                ModelCapability.STREAMING
            ),
            "llama-3.2-11b-vision-preview" to setOf(
                ModelCapability.VISION,
                ModelCapability.TEXT,
                ModelCapability.STREAMING
            ),
            "whisper-large-v3-turbo" to setOf(
                ModelCapability.AUDIO
            ),
            "qwen/qwen3.8-27b" to setOf(
                ModelCapability.VISION,
                ModelCapability.TEXT,
                ModelCapability.STREAMING
            ),
            "openai/gpt-oss-120b" to setOf(
                ModelCapability.TEXT,
                ModelCapability.REASONING,
                ModelCapability.STREAMING
            )
        )
    }

    /**
     * Resolves the appropriate model ID for the given [requestType] according to current preferences.
     */
    fun selectModel(requestType: RequestType): String {
        val config = preferences.config.value

        if (!config.smartRouting) {
            return when (requestType) {
                RequestType.IMAGE_VISION -> config.visionModel
                RequestType.AUDIO_TRANSCRIPTION -> config.voiceModel
                else -> config.primaryChatModel
            }
        }

        return when (requestType) {
            RequestType.NORMAL_CHAT -> config.primaryChatModel
            RequestType.TOOL_EXECUTION -> {
                // Ensure chosen model has tool-calling
                val capabilities = VERIFIED_REGISTRY[config.primaryChatModel].orEmpty()
                if (ModelCapability.TOOL_CALLING in capabilities) {
                    config.primaryChatModel
                } else {
                    "llama-3.3-70b-versatile" // Fallback to verified tool-calling model
                }
            }
            RequestType.IMAGE_VISION -> config.visionModel
            RequestType.AUDIO_TRANSCRIPTION -> config.voiceModel
            RequestType.COMPLEX_REASONING -> config.primaryChatModel
            RequestType.LIGHTWEIGHT -> config.backupChatModel
        }
    }

    /**
     * Validates whether [candidateModel] has the required capabilities for [requestType].
     * Rule: Never switch an image request to text-only, or tool request to non-tool model.
     */
    fun isCompatibleForFailover(candidateModel: String, requestType: RequestType): Boolean {
        val capabilities = VERIFIED_REGISTRY[candidateModel].orEmpty()
        return when (requestType) {
            RequestType.IMAGE_VISION -> ModelCapability.VISION in capabilities
            RequestType.TOOL_EXECUTION -> ModelCapability.TOOL_CALLING in capabilities
            RequestType.AUDIO_TRANSCRIPTION -> ModelCapability.AUDIO in capabilities
            RequestType.NORMAL_CHAT, RequestType.COMPLEX_REASONING, RequestType.LIGHTWEIGHT ->
                ModelCapability.TEXT in capabilities
        }
    }

    /**
     * Obtains the backup model for failover. Returns null if automatic switching is disabled
     * or if the configured backup model cannot support the required capabilities.
     */
    fun getFailoverModel(primaryModel: String, requestType: RequestType): String? {
        val config = preferences.config.value
        if (!config.autoModelSwitching) return null

        val candidate = if (requestType == RequestType.IMAGE_VISION) {
            "llama-3.2-11b-vision-preview"
        } else {
            config.backupChatModel
        }

        if (candidate == primaryModel) return null

        return if (isCompatibleForFailover(candidate, requestType)) {
            candidate
        } else {
            null
        }
    }

    /**
     * Inspects all verified models and returns structured [ModelInfo] representations.
     */
    fun getAvailableModels(): List<ModelInfo> {
        val config = preferences.config.value
        return listOf(
            ModelInfo(
                id = "llama-3.3-70b-versatile",
                displayName = "Llama 3.3 70B Versatile",
                provider = "GROQ",
                capabilities = VERIFIED_REGISTRY["llama-3.3-70b-versatile"].orEmpty(),
                averageLatencyMs = 142,
                isPrimary = config.primaryChatModel == "llama-3.3-70b-versatile",
                isBackup = config.backupChatModel == "llama-3.3-70b-versatile",
                isAvailable = true
            ),
            ModelInfo(
                id = "llama-3.1-8b-instant",
                displayName = "Llama 3.1 8B Instant",
                provider = "GROQ",
                capabilities = VERIFIED_REGISTRY["llama-3.1-8b-instant"].orEmpty(),
                averageLatencyMs = 68,
                isPrimary = config.primaryChatModel == "llama-3.1-8b-instant",
                isBackup = config.backupChatModel == "llama-3.1-8b-instant",
                isAvailable = true
            ),
            ModelInfo(
                id = "llama-3.2-90b-vision-preview",
                displayName = "Llama 3.2 90B Vision",
                provider = "GROQ",
                capabilities = VERIFIED_REGISTRY["llama-3.2-90b-vision-preview"].orEmpty(),
                averageLatencyMs = 210,
                isPrimary = config.visionModel == "llama-3.2-90b-vision-preview",
                isBackup = false,
                isAvailable = true
            ),
            ModelInfo(
                id = "whisper-large-v3-turbo",
                displayName = "Whisper Large v3 Turbo",
                provider = "GROQ",
                capabilities = VERIFIED_REGISTRY["whisper-large-v3-turbo"].orEmpty(),
                averageLatencyMs = 95,
                isPrimary = config.voiceModel == "whisper-large-v3-turbo",
                isBackup = false,
                isAvailable = true
            )
        )
    }
}
