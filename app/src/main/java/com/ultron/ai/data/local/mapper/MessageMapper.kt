package com.ultron.ai.data.local.mapper

import com.ultron.ai.data.local.entity.ChatMessageEntity
import com.ultron.ai.domain.model.Message
import com.ultron.ai.domain.model.MessageRole

/**
 * Pure mapping functions between Entity and Domain models (FP style).
 * DRY: centralized mapping logic, used by repository only.
 */

fun ChatMessageEntity.toDomain(): Message = Message(
    id = id,
    content = content,
    role = MessageRole.valueOf(role),
    sessionId = sessionId,
    timestamp = timestamp
)

fun Message.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    content = content,
    role = role.name,
    sessionId = sessionId,
    timestamp = timestamp
)
