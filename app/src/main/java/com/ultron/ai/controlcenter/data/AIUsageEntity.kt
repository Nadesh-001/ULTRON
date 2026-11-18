package com.ultron.ai.controlcenter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local record of an AI API request for usage tracking, latency monitoring, and audit trails.
 * Never stores API keys or raw private user content.
 */
@Entity(tableName = "ai_usage_records")
data class AIUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "GROQ",
    val modelId: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = 0,
    val latencyMs: Long = 0,
    val httpStatus: Int = 200,
    val isSuccess: Boolean = true,
    val isFailover: Boolean = false,
    val errorMessage: String? = null
)
