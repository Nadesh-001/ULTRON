package com.ultron.ai.service

import android.util.Log
import com.ultron.ai.data.local.dao.InteractionDao
import com.ultron.ai.core.CorrectionDetector
import com.ultron.ai.core.FactFilter
import com.ultron.ai.data.local.dao.MemoryDao
import com.ultron.ai.data.local.dao.UserPreferenceDao
import com.ultron.ai.data.local.entity.InteractionEntity
import com.ultron.ai.data.local.entity.MemoryEntity
import com.ultron.ai.data.local.entity.UserPreferenceEntity
import com.ultron.ai.data.remote.GroqApiService
import com.ultron.ai.data.remote.LazuriApiService
import com.ultron.ai.data.remote.dto.ApiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The assistant's memory: structured facts extracted from conversations
 * (preferences, habits, interests, ...) plus a raw interaction log.
 */
class UltronMemory(
    private val memoryDao: MemoryDao,
    private val interactionDao: InteractionDao,
    private val groqApi: GroqApiService,
    private val prefDao: UserPreferenceDao,
    private val lazuriApi: LazuriApiService,
    private val catalog: ModelCatalog
) {

    companion object {
        private const val TAG = "UltronMemory"

        private const val PREF_LAZURI_URL = "lazuri_base_url"
        private const val PREF_LAZURI_KEY = "lazuri_api_key"
        const val PREF_LAZURI_DEVICE_ID = "lazuri_device_id"
        const val PREF_LAZURI_ENABLED = "lazuri_enabled"

        private const val PREF_SESSION_ID = "voice_session_id"
        private const val PREF_SESSION_LAST_ACTIVITY = "voice_session_last_activity"

        private const val SESSION_TIMEOUT_MS = 60 * 60 * 1000L

        object Category {
            const val PREFERENCE = "preference"
            const val FACT = "fact"
            const val HABIT = "habit"
            const val MUSIC = "music"
            const val CONTACT = "contact"
            const val INTEREST = "interest"
        }

        private const val EXTRACTION_PROMPT = """Analyze this conversation and extract facts about the user. Return ONLY a list of facts in this exact format, one per line:
CATEGORY|KEY|VALUE

Categories: preference, fact, habit, music, contact, interest

Examples:
preference|language|English
fact|name|Alex
habit|morning_routine|checks news first
music|favorite_genre|hip-hop
contact|best_friend|John
interest|hobby|programming

Rules:
- Only extract CLEAR facts STATED by the user, not assumptions
- A question is never a fact. "What is my name?" tells you nothing — skip it.
- Extract only from the User line, never from the Assistant line
- Skip greetings, commands (open app, set alarm), and small talk
- If nothing meaningful, respond with just: NONE
- Max 5 facts per conversation
- Use lowercase for category and key
- Value can be in any language"""
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun recordCorrection(correction: CorrectionDetector.Correction) {
        withContext(Dispatchers.IO) {
            saveOrUpdate(
                category = "correction",
                key = "correction_" + (System.currentTimeMillis() / 1000),
                value = CorrectionDetector.asMemoryNote(correction),
                source = "user_correction"
            )
        }
    }

    var onSessionClosed: ((String) -> Unit)? = null

    suspend fun logInteraction(userInput: String, response: String, commandType: String?, sessionId: String? = null) {
        withContext(Dispatchers.IO) {
            interactionDao.insert(
                InteractionEntity(
                    userInput = userInput,
                    assistantResponse = response,
                    commandType = commandType,
                    sessionId = sessionId
                )
            )
        }
        pushInteractionToLazuri(userInput, response, commandType, sessionId)
    }

    suspend fun getOrCreateSessionId(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastActivity = prefDao.get(PREF_SESSION_LAST_ACTIVITY)?.toLongOrNull()
        val existing = prefDao.get(PREF_SESSION_ID)

        val sessionId = if (existing != null && lastActivity != null && (now - lastActivity) < SESSION_TIMEOUT_MS) {
            existing
        } else {
            java.util.UUID.randomUUID().toString()
        }

        prefDao.set(UserPreferenceEntity(PREF_SESSION_ID, sessionId))
        prefDao.set(UserPreferenceEntity(PREF_SESSION_LAST_ACTIVITY, now.toString()))
        sessionId
    }

    suspend fun getSessionHistory(sessionId: String, limit: Int = 10): List<InteractionEntity> =
        withContext(Dispatchers.IO) { interactionDao.getBySession(sessionId, limit) }

    suspend fun startNewSession(): String = withContext(Dispatchers.IO) {
        prefDao.get(PREF_SESSION_ID)?.let { onSessionClosed?.invoke(it) }
        val sessionId = java.util.UUID.randomUUID().toString()
        prefDao.set(UserPreferenceEntity(PREF_SESSION_ID, sessionId))
        prefDao.set(UserPreferenceEntity(PREF_SESSION_LAST_ACTIVITY, System.currentTimeMillis().toString()))
        sessionId
    }

    suspend fun resumeSession(sessionId: String) = withContext(Dispatchers.IO) {
        prefDao.set(UserPreferenceEntity(PREF_SESSION_ID, sessionId))
        prefDao.set(UserPreferenceEntity(PREF_SESSION_LAST_ACTIVITY, System.currentTimeMillis().toString()))
    }

    fun extractMemoriesInBackground(userInput: String, assistantResponse: String) {
        syncScope.launch {
            try {
                extractAndSaveMemories(userInput, assistantResponse)
            } catch (e: Exception) {
                Log.w(TAG, "Background memory extraction failed: ${e.message}")
            }
        }
    }

    suspend fun extractAndSaveMemories(userInput: String, assistantResponse: String) {
        withContext(Dispatchers.IO) {
            try {
                val apiKey = prefDao.get("groq_api_key") ?: return@withContext

                val conversation = "User: $userInput\nAssistant: $assistantResponse"
                val messages = listOf(
                    ApiMessage(role = "system", content = EXTRACTION_PROMPT),
                    ApiMessage(role = "user", content = conversation)
                )

                val responseBuilder = StringBuilder()
                groqApi.streamCompletion(apiKey, messages, catalog.model(com.ultron.ai.core.GroqModels.Role.FAST))
                    .catch { e ->
                        Log.e(TAG, "Extraction failed: ${e.message}")
                        if (e is com.ultron.ai.data.remote.GroqApiException && e.modelMissing) catalog.refresh()
                    }
                    .collect { token -> responseBuilder.append(token) }

                val result = responseBuilder.toString().trim()
                if (result == "NONE" || result.isBlank()) return@withContext

                result.lines()
                    .filter { it.contains("|") }
                    .forEach { line ->
                        val parts = line.split("|", limit = 3)
                        if (parts.size == 3) {
                            val (category, key, value) = parts.map { it.trim().lowercase() }
                            if (FactFilter.isStorable(key, value)) {
                                saveOrUpdate(category, key, value, "conversation")
                            } else {
                                Log.d(TAG, "Rejected an extracted fact")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Memory extraction error: ${e.message}")
            }
        }
    }

    suspend fun saveOrUpdate(category: String, key: String, value: String, source: String) {
        val existing = memoryDao.findByKey(key, category)
        if (existing != null) {
            memoryDao.updateValue(existing.id, value)
        } else {
            memoryDao.insert(
                MemoryEntity(
                    category = category,
                    key = key,
                    value = value,
                    source = source
                )
            )
        }
        pushFactToLazuri(category, key, value)
    }

    suspend fun buildMemoryContext(): String = withContext(Dispatchers.IO) {
        val memories = memoryDao.getRecent(30)
        if (memories.isEmpty()) return@withContext ""

        buildString {
            appendLine("\n--- User Memory (things you know about this user) ---")
            val grouped = memories.groupBy { it.category }
            grouped.forEach { (category, items) ->
                appendLine("[$category]")
                items.forEach { m ->
                    appendLine("- ${m.key}: ${m.value}")
                }
            }
            appendLine("--- End of Memory ---")
            if (memories.any { it.category == "correction" }) {
                appendLine(
                    "Where a correction contradicts an earlier fact, the correction wins."
                )
            }
            appendLine(
                "These are saved notes, not part of the current conversation. " +
                    "Use them to personalise your answers. Do not talk about databases " +
                    "or memory systems; if asked how you know something, say it was " +
                    "saved from an earlier conversation. Never claim the user told you " +
                    "something in this conversation unless it appears above in this chat."
            )
        }
    }

    suspend fun getMemories(): List<MemoryEntity> = memoryDao.getRecent(50)

    suspend fun search(query: String): List<MemoryEntity> = memoryDao.search(query)

    suspend fun deleteMemory(id: Long) = memoryDao.delete(id)

    suspend fun getStats(): MemoryStats {
        val memCount = memoryDao.count()
        val interactionCount = interactionDao.count()
        val commandStats = interactionDao.getCommandStats()
        return MemoryStats(memCount, interactionCount, commandStats.associate { it.commandType to it.cnt })
    }

    suspend fun registerWithLazuri(baseUrl: String, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val device = lazuriApi.registerDevice(baseUrl, apiKey, name = "Ultron Mobile", platform = "phone")
            prefDao.set(UserPreferenceEntity(PREF_LAZURI_URL, baseUrl))
            prefDao.set(UserPreferenceEntity(PREF_LAZURI_KEY, apiKey))
            prefDao.set(UserPreferenceEntity(PREF_LAZURI_DEVICE_ID, device.id))
            prefDao.set(UserPreferenceEntity(PREF_LAZURI_ENABLED, "true"))
            Result.success(device.id)
        } catch (e: Exception) {
            Log.e(TAG, "Lazuri registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun saveNote(text: String): Boolean = withContext(Dispatchers.IO) {
        val (url, key, _) = lazuriCreds() ?: return@withContext false
        val title = text.take(60).substringBefore('\n').trim().ifBlank { "Note" }
        runCatching { lazuriApi.createNote(url, key, title, text) }.getOrDefault(false)
    }

    suspend fun isLazuriConfigured(): Boolean =
        prefDao.get(PREF_LAZURI_ENABLED) == "true" && prefDao.get(PREF_LAZURI_DEVICE_ID) != null

    private suspend fun lazuriCreds(): Triple<String, String, String>? {
        if (prefDao.get(PREF_LAZURI_ENABLED) != "true") return null
        val url = prefDao.get(PREF_LAZURI_URL) ?: return null
        val key = prefDao.get(PREF_LAZURI_KEY) ?: return null
        val deviceId = prefDao.get(PREF_LAZURI_DEVICE_ID) ?: return null
        return Triple(url, key, deviceId)
    }

    private fun pushInteractionToLazuri(userInput: String, response: String, commandType: String?, sessionId: String?) {
        syncScope.launch {
            val (url, key, deviceId) = lazuriCreds() ?: return@launch
            try {
                val meta = commandType?.let { mapOf("commandType" to it) }
                listOf("user" to userInput, "assistant" to response).forEach { (role, content) ->
                    lazuriApi.createMemory(
                        url, key, role = role, content = content,
                        sourceDeviceId = deviceId, sessionId = sessionId, metadata = meta
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lazuri interaction push failed: ${e.message}")
            }
        }
    }

    private fun pushFactToLazuri(category: String, key: String, value: String) {
        syncScope.launch {
            val (url, apiKey, deviceId) = lazuriCreds() ?: return@launch
            try {
                lazuriApi.createMemory(
                    url, apiKey,
                    role = "fact",
                    content = "$key: $value",
                    sourceDeviceId = deviceId,
                    metadata = mapOf("category" to category, "key" to key)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Lazuri fact push failed: ${e.message}")
            }
        }
    }

    data class MemoryStats(
        val totalMemories: Int,
        val totalInteractions: Int,
        val commandUsage: Map<String, Int>
    )
}

/** Backward compatibility alias */
typealias FridayMemory = UltronMemory
