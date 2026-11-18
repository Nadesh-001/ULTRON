package com.ultron.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ultron.ai.data.local.dao.BackupDao
import com.ultron.ai.data.local.dao.ChatMessageDao
import com.ultron.ai.data.local.dao.InteractionDao
import com.ultron.ai.data.local.dao.ErrandDao
import com.ultron.ai.data.local.dao.MemoryDao
import com.ultron.ai.data.local.dao.ModeDao
import com.ultron.ai.data.local.dao.ModeScheduleDao
import com.ultron.ai.data.local.dao.NotificationDao
import com.ultron.ai.data.local.dao.SessionSummaryDao
import com.ultron.ai.data.local.dao.UserPreferenceDao
import com.ultron.ai.data.local.entity.ChatMessageEntity
import com.ultron.ai.data.local.entity.InteractionEntity
import com.ultron.ai.data.local.entity.ErrandEntity
import com.ultron.ai.data.local.entity.ModeEntity
import com.ultron.ai.data.local.entity.ModeEventEntity
import com.ultron.ai.data.local.entity.ModeRunEntity
import com.ultron.ai.data.local.entity.ModeScheduleEntity
import com.ultron.ai.data.local.entity.MemoryEntity
import com.ultron.ai.data.local.entity.NotificationEntity
import com.ultron.ai.data.local.entity.SessionSummaryEntity
import com.ultron.ai.data.local.entity.UserPreferenceEntity

@Database(
    entities = [
        ChatMessageEntity::class,
        UserPreferenceEntity::class,
        MemoryEntity::class,
        InteractionEntity::class,
        NotificationEntity::class,
        SessionSummaryEntity::class,
        ErrandEntity::class,
        ModeEntity::class,
        ModeScheduleEntity::class,
        ModeRunEntity::class,
        ModeEventEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class UltronDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun memoryDao(): MemoryDao
    abstract fun interactionDao(): InteractionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun sessionSummaryDao(): SessionSummaryDao
    abstract fun errandDao(): ErrandDao
    abstract fun backupDao(): BackupDao
    abstract fun modeDao(): ModeDao
    abstract fun modeScheduleDao(): ModeScheduleDao
}

/** Backward compatibility alias */
typealias UltronDatabase = UltronDatabase
