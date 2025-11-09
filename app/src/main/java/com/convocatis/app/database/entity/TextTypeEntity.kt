package com.convocatis.app.database.entity

/**
 * Represents a text type from conv_texts_types.xml
 */
data class TextTypeEntity(
    val rid: Long,
    val description: String,
    val type: Int?,
    val code: String?
)
