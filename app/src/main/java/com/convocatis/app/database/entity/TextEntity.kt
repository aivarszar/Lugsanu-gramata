package com.convocatis.app.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "texts")
data class TextEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val backendId: Long? = null,
    val originalId: Long? = null,
    val isShared: Boolean = false,
    val text: String = "",
    val title: String = "",
    val langId: Long? = null,
    val like: Boolean? = null,
    val code: String? = null,
    val type: Int? = null,
    val rating: Int = 0,
    val highlighted: Boolean = false,
    val isDefault: Boolean = false,
    val schedule: String? = null,
    val dirty: Boolean = false,
    val likeDirty: Boolean = false,
    val scheduleDirty: Boolean = false,
    val isSharedDirty: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Parcelable {

    fun getNextPrayerTime(): Long {
        // TODO: Implement prayer time calculation based on schedule
        return -1
    }
}
