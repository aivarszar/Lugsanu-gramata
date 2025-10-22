package com.convocatis.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.convocatis.app.database.dao.GroupDao
import com.convocatis.app.database.dao.NotificationDao
import com.convocatis.app.database.dao.TextDao
import com.convocatis.app.database.entity.*

@Database(
    entities = [
        TextEntity::class,
        GroupEntity::class,
        NotificationEntity::class,
        LanguageEntity::class,
        DenominationEntity::class,
        TextUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun textDao(): TextDao
    abstract fun groupDao(): GroupDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "convocatis_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
