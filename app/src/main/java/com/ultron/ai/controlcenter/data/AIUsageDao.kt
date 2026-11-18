package com.ultron.ai.controlcenter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AIUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(record: AIUsageEntity): Long

    @Query("SELECT * FROM ai_usage_records ORDER BY timestamp DESC LIMIT 500")
    fun observeRecentUsage(): Flow<List<AIUsageEntity>>

    @Query("SELECT * FROM ai_usage_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getUsageSince(sinceTimestamp: Long): List<AIUsageEntity>

    @Query("SELECT COUNT(*) FROM ai_usage_records WHERE timestamp >= :sinceTimestamp")
    suspend fun getRequestCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COALESCE(SUM(totalTokens), 0) FROM ai_usage_records WHERE timestamp >= :sinceTimestamp")
    suspend fun getTotalTokensSince(sinceTimestamp: Long): Int

    @Query("SELECT COALESCE(SUM(inputTokens), 0) FROM ai_usage_records WHERE timestamp >= :sinceTimestamp")
    suspend fun getInputTokensSince(sinceTimestamp: Long): Int

    @Query("SELECT COALESCE(SUM(outputTokens), 0) FROM ai_usage_records WHERE timestamp >= :sinceTimestamp")
    suspend fun getOutputTokensSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM ai_usage_records WHERE isFailover = 1 AND timestamp >= :sinceTimestamp")
    suspend fun getFailoverCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM ai_usage_records WHERE httpStatus = 429 AND timestamp >= :sinceTimestamp")
    suspend fun getRateLimitCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM ai_usage_records WHERE httpStatus >= 500 AND timestamp >= :sinceTimestamp")
    suspend fun getServerErrorCountSince(sinceTimestamp: Long): Int
}
