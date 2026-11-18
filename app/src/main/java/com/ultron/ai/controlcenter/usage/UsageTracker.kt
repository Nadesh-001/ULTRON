package com.ultron.ai.controlcenter.usage

import com.ultron.ai.controlcenter.data.AIUsageDao
import com.ultron.ai.controlcenter.data.AIUsageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Local usage tracker.
 * Records request metrics and token consumption into Room DB for privacy-first analytics.
 */
class UsageTracker(
    private val usageDao: AIUsageDao
) {

    data class UsageSummary(
        val totalRequests: Int,
        val totalInputTokens: Int,
        val totalOutputTokens: Int,
        val totalTokens: Int,
        val failoverEvents: Int,
        val rateLimitErrors: Int,
        val serverErrors: Int,
        val successRatePercentage: Float,
        val records: List<AIUsageEntity>
    )

    /**
     * Records an API transaction into local database.
     */
    suspend fun recordUsage(
        modelId: String,
        inputTokens: Int,
        outputTokens: Int,
        latencyMs: Long,
        httpStatus: Int = 200,
        isSuccess: Boolean = true,
        isFailover: Boolean = false,
        errorMessage: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val entity = AIUsageEntity(
            requestId = UUID.randomUUID().toString(),
            modelId = modelId,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = inputTokens + outputTokens,
            latencyMs = latencyMs,
            httpStatus = httpStatus,
            isSuccess = isSuccess,
            isFailover = isFailover,
            errorMessage = errorMessage
        )
        usageDao.insertUsage(entity)
    }

    /**
     * Fetches local usage summary since [sinceTimestamp].
     */
    suspend fun getSummary(sinceTimestamp: Long): UsageSummary = withContext(Dispatchers.IO) {
        val records = usageDao.getUsageSince(sinceTimestamp)
        val requests = records.size
        val inTokens = records.sumOf { it.inputTokens }
        val outTokens = records.sumOf { it.outputTokens }
        val totalTokens = inTokens + outTokens
        val failovers = records.count { it.isFailover }
        val rateLimits = records.count { it.httpStatus == 429 }
        val serverErrors = records.count { it.httpStatus >= 500 }
        val successCount = records.count { it.isSuccess }

        val successRate = if (requests > 0) {
            (successCount.toFloat() / requests.toFloat()) * 100f
        } else {
            100f
        }

        UsageSummary(
            totalRequests = requests,
            totalInputTokens = inTokens,
            totalOutputTokens = outTokens,
            totalTokens = totalTokens,
            failoverEvents = failovers,
            rateLimitErrors = rateLimits,
            serverErrors = serverErrors,
            successRatePercentage = successRate,
            records = records
        )
    }
}
