package com.convocatis.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.convocatis.app.database.entity.GroupEntity

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): LiveData<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("DELETE FROM groups")
    suspend fun deleteAllGroups()
}
