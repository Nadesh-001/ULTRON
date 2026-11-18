package com.ultron.ai.data.repository

import com.ultron.ai.agent.UltronAgent
import com.ultron.ai.agent.PromptBudget
import com.ultron.ai.core.SystemPromptBuilder
import com.ultron.ai.data.local.dao.ChatMessageDao
import com.ultron.ai.data.local.dao.ChatSessionSummary
import com.ultron.ai.data.local.dao.UserPreferenceDao
import com.ultron.ai.data.local.mapper.toDomain
import com.ultron.ai.data.local.mapper.toEntity
import com.ultron.ai.data.remote.GroqApiException
import com.ultron.ai.data.remote.GroqApiService
import com.ultron.ai.data.remote.dto.ApiMessage
import com.ultron.ai.domain.model.AssistantMode
import com.ultron.ai.domain.model.Message
import com.ultron.ai.domain.model.MessageRole
import com.ultron.ai.domain.repository.AssistantRepository
import com.ultron.ai.service.ULTRONMemory
import com.ultron.ai.service.ModelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

// Seven collaborators, each used: the chat needs the model, history, settings,
// prompt, memory, model choice and the agent. Bundling them would only hide that.
@Suppress("LongParameterList")
class AssistantRepositoryImpl(
    private val groqApi: GroqApiService,
    private val chatDao: ChatMessageDao,
    private val prefDao: UserPreferenceDao,
    private val promptBuilder: SystemPromptBuilder,
    private val memory: ULTRONMemory,
    private val catalog: ModelCatalog,
    private val agent: UltronAgent
) : AssistantRepository {

    companion object {
        const val PREF_API_KEY = "groq_api_key"
    }

    override suspend fun sendMessage(
        prompt: String,
        history: List<Message>,
        mode: AssistantMode
    ): Flow<String> {
        val apiKey = prefDao.get(PREF_API_KEY) ?: ""
        val model = catalog.model(com.ultron.ai.core.GroqModels.Role.CHAT)
        val memoryContext = memory.buildMemoryContext()
        val apiMessages = buildApiMessages(prompt, history, mode, memoryContext)

        val responseBuilder = StringBuilder()
        val russian = false
        val backup = catalog.model(com.ultron.ai.core.GroqModels.Role.FAST)
        val settings = UltronAgent.Settings(apiKey, model, GroqApiService.DEFAULT_MAX_TOKENS, backup)
        return agent.reply(settings, apiMessages, russian)
            .catch { e ->
                // A retired model: re-read the list so the next message works.
                if (e is GroqApiException && e.modelMissing) catalog.refresh()
                throw e
            }
            .onCompletion {
                val response = responseBuilder.toString()
                if (response.isNotBlank()) {
                    memory.logInteraction(prompt, response, null)
                    // Extraction is a second LLM call — keep it off the stream's
                    // completion path so the chat UI isn't held open waiting.
                    memory.extractMemoriesInBackground(prompt, response)
                }
            }
            .map { token ->
                responseBuilder.append(token)
                token
            }
    }

    override fun getSessionMessages(sessionId: String): Flow<List<Message>> =
        chatDao.observeSession(sessionId).map { entities -> entities.map { it.toDomain() } }

    override fun getSessions(): Flow<List<ChatSessionSummary>> = chatDao.observeSessions()

    override suspend fun saveMessage(message: Message) {
        chatDao.insert(message.toEntity())
    }

    override suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSession(sessionId)
    }

    override suspend fun analyzeImage(imageBase64: String, prompt: String): Flow<String> {
        val apiKey = prefDao.get(PREF_API_KEY) ?: ""
        val model = catalog.model(com.ultron.ai.core.GroqModels.Role.VISION)
        return groqApi.analyzeImage(apiKey, imageBase64, prompt, model)
    }

    override suspend fun analyzeText(text: String, prompt: String): Flow<String> {
        val apiKey = prefDao.get(PREF_API_KEY) ?: ""
        return groqApi.analyzeText(apiKey, text, prompt, catalog.model(com.ultron.ai.core.GroqModels.Role.CHAT))
    }

    private fun buildApiMessages(
        prompt: String,
        history: List<Message>,
        mode: AssistantMode,
        memoryContext: String
    ): List<ApiMessage> = buildList {
        add(ApiMessage(role = "system", content = promptBuilder.build(mode, memoryContext, withTools = true)))

        val recent = history.takeLast(PromptBudget.CHAT_HISTORY)
        recent.forEachIndexed { i, msg ->
            add(ApiMessage(
                role = when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                },
                content = PromptBudget.older(msg.content, fromEnd = recent.size - i)
            ))
        }

        add(ApiMessage(role = "user", content = prompt))
    }
}
