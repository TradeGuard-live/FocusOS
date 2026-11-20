package com.naman.focusos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.naman.focusos.data.model.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries WHERE isEnabled = 1 ORDER BY startTime ASC")
    fun getEnabledEntries(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE isEnabled = 1 ORDER BY startTime ASC")
    fun getEnabledEntriesSync(): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries ORDER BY startTime ASC")
    fun getAllEntries(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries ORDER BY startTime ASC")
    fun getAllEntriesSync(): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): TimetableEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntrySync(entry: TimetableEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TimetableEntryEntity>): List<Long>

    @Update
    suspend fun updateEntry(entry: TimetableEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAllEntries()
}
