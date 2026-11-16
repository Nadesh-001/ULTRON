package com.ultron.ai.domain.usecase

import com.ultron.ai.domain.model.AssistantMode
import com.ultron.ai.domain.model.Message
import com.ultron.ai.domain.repository.AssistantRepository
import kotlinx.coroutines.flow.Flow

class SendMessageUseCase(private val repository: AssistantRepository) {

    suspend operator fun invoke(
        prompt: String,
        history: List<Message>,
        mode: AssistantMode
    ): Flow<String> {
        return repository.sendMessage(prompt, history, mode)
    }

    suspend fun saveMessage(message: Message) {
        repository.saveMessage(message)
    }
}
