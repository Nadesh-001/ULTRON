package com.ultron.ai.controlcenter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AIUsageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AIControlCenterDatabase : RoomDatabase() {

    abstract fun aiUsageDao(): AIUsageDao

    companion object {
        private const val DB_NAME = "ultron_control_center.db"

        fun create(context: Context): AIControlCenterDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AIControlCenterDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
