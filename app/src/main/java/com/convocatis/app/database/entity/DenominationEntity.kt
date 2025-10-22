package com.convocatis.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "denominations")
data class DenominationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val backendId: Long? = null,
    val name: String = "",
    val description: String = ""
)
