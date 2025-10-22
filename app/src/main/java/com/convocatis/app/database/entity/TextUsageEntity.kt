package com.convocatis.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text_usages")
data class TextUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val textId: Long = 0,
    val usageCount: Int = 0,
    val lastUsedAt: Long = 0L
)
