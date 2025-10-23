package com.convocatis.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "texts")
data class TextEntity(
    @PrimaryKey
    val rid: Long,                       // RID from XML
    val title: String,                   // Description from XML
    val rawContent: String,              // String from XML with special codes (|, %, ^, >><<)
    val categoryType: Int? = null,       // Text_type from XML
    val categoryCode: String? = null,    // Code from XML
    val languageCode: String = "lv"      // "lv" or "en"
) : Serializable
