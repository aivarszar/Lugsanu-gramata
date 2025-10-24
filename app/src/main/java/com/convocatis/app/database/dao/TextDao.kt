package com.convocatis.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.convocatis.app.database.entity.TextEntity

@Dao
interface TextDao {
    @Query("SELECT * FROM texts ORDER BY title ASC")
    fun getAllTexts(): LiveData<List<TextEntity>>

    @Query("SELECT * FROM texts WHERE rid = :rid")
    suspend fun getTextByRid(rid: Long): TextEntity?

    @Query("SELECT * FROM texts WHERE title LIKE '%' || :searchTerm || '%' OR rawContent LIKE '%' || :searchTerm || '%'")
    fun searchTexts(searchTerm: String): LiveData<List<TextEntity>>

    @Query("SELECT * FROM texts WHERE categoryType = :categoryType ORDER BY title ASC")
    fun getTextsByCategory(categoryType: Int): LiveData<List<TextEntity>>

    @Query("SELECT DISTINCT categoryCode FROM texts WHERE categoryCode IS NOT NULL AND categoryCode != '' ORDER BY categoryCode ASC")
    suspend fun getUniqueCategoryCodes(): List<String>

    @Query("SELECT * FROM texts WHERE categoryCode = :categoryCode ORDER BY title ASC")
    fun getTextsByCategoryCode(categoryCode: String): LiveData<List<TextEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertText(text: TextEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(texts: List<TextEntity>)

    @Query("DELETE FROM texts")
    suspend fun deleteAllTexts()
}
