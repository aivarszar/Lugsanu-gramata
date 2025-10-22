package com.convocatis.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.convocatis.app.database.entity.TextEntity

@Dao
interface TextDao {
    @Query("SELECT * FROM texts ORDER BY title ASC")
    fun getAllTexts(): LiveData<List<TextEntity>>

    @Query("SELECT * FROM texts WHERE highlighted = 1 ORDER BY title ASC")
    fun getHighlightedTexts(): LiveData<List<TextEntity>>

    @Query("SELECT * FROM texts WHERE id = :id")
    suspend fun getTextById(id: Long): TextEntity?

    @Query("SELECT * FROM texts WHERE title LIKE '%' || :searchTerm || '%' OR text LIKE '%' || :searchTerm || '%'")
    fun searchTexts(searchTerm: String): LiveData<List<TextEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertText(text: TextEntity): Long

    @Update
    suspend fun updateText(text: TextEntity)

    @Delete
    suspend fun deleteText(text: TextEntity)

    @Query("DELETE FROM texts")
    suspend fun deleteAllTexts()
}
