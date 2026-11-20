package com.naman.focusos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.naman.focusos.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY CASE WHEN startTime IS NULL OR startTime = '' THEN 1 ELSE 0 END, startTime ASC, priority DESC, id ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY CASE WHEN startTime IS NULL OR startTime = '' THEN 1 ELSE 0 END, startTime ASC, priority DESC, id ASC")
    fun getTasksForDateSync(date: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate ASC, CASE WHEN startTime IS NULL OR startTime = '' THEN 1 ELSE 0 END, startTime ASC, priority DESC, id ASC")
    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate ASC, CASE WHEN startTime IS NULL OR startTime = '' THEN 1 ELSE 0 END, startTime ASC, priority DESC, id ASC")
    fun getTasksBetweenDatesSync(startDate: String, endDate: String): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY scheduledDate DESC, CASE WHEN startTime IS NULL OR startTime = '' THEN 1 ELSE 0 END, startTime ASC, priority DESC, id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY scheduledDate DESC, CASE WHEN startTime IS NULL OR startTime = '' THEN 1 ELSE 0 END, startTime ASC, priority DESC, id ASC")
    fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdSync(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY scheduledDate DESC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTaskSync(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    fun updateTaskSync(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE recurrenceRuleId = :ruleId")
    suspend fun deleteTasksByRecurrenceRule(ruleId: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
