package com.naman.focusos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.naman.focusos.data.model.AppClassificationEntity
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    // =========================================================================
    // FOCUS SESSIONS
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Delete
    suspend fun deleteSession(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
    fun getSessionFlowById(sessionId: Long): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE sessionStatus IN ('ACTIVE', 'PAUSED') ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE sessionStatus IN ('ACTIVE', 'PAUSED') ORDER BY startTimestamp DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions ORDER BY startTimestamp DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startTimestamp DESC")
    suspend fun getAllSessionsSync(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE startTimestamp >= :startTime AND startTimestamp <= :endTime ORDER BY startTimestamp DESC")
    fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE startTimestamp >= :startTime AND startTimestamp <= :endTime ORDER BY startTimestamp DESC")
    suspend fun getSessionsBetweenSync(startTime: Long, endTime: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE sessionStatus = 'COMPLETED' ORDER BY startTimestamp DESC LIMIT :limit")
    fun getRecentCompletedSessions(limit: Int = 50): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE linkedTaskId = :taskId ORDER BY startTimestamp DESC")
    fun getSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE linkedTaskId = :taskId ORDER BY startTimestamp DESC")
    suspend fun getSessionsForTaskSync(taskId: Long): List<FocusSessionEntity>

    // =========================================================================
    // FOCUS PAUSES
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPause(pause: FocusPauseEntity): Long

    @Update
    suspend fun updatePause(pause: FocusPauseEntity)

    @Query("SELECT * FROM focus_session_pauses WHERE sessionId = :sessionId ORDER BY pauseTimestamp ASC")
    fun getPausesForSession(sessionId: Long): Flow<List<FocusPauseEntity>>

    @Query("SELECT * FROM focus_session_pauses WHERE sessionId = :sessionId ORDER BY pauseTimestamp ASC")
    suspend fun getPausesForSessionSync(sessionId: Long): List<FocusPauseEntity>

    @Query("SELECT * FROM focus_session_pauses WHERE sessionId = :sessionId AND resumeTimestamp IS NULL ORDER BY pauseTimestamp DESC LIMIT 1")
    suspend fun getActivePauseForSession(sessionId: Long): FocusPauseEntity?

    // =========================================================================
    // FOCUS APP USAGES
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(usage: FocusAppUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsages(usages: List<FocusAppUsageEntity>)

    @Query("SELECT * FROM focus_app_usages WHERE sessionId = :sessionId ORDER BY startTimestamp ASC")
    fun getAppUsagesForSession(sessionId: Long): Flow<List<FocusAppUsageEntity>>

    @Query("SELECT * FROM focus_app_usages WHERE sessionId = :sessionId ORDER BY startTimestamp ASC")
    suspend fun getAppUsagesForSessionSync(sessionId: Long): List<FocusAppUsageEntity>

    @Query("SELECT * FROM focus_app_usages WHERE startTimestamp >= :startTime AND startTimestamp <= :endTime ORDER BY startTimestamp DESC")
    fun getAllAppUsagesBetween(startTime: Long, endTime: Long): Flow<List<FocusAppUsageEntity>>

    @Query("SELECT * FROM focus_app_usages WHERE startTimestamp >= :startTime AND startTimestamp <= :endTime ORDER BY startTimestamp DESC")
    suspend fun getAllAppUsagesBetweenSync(startTime: Long, endTime: Long): List<FocusAppUsageEntity>

    @Query("SELECT * FROM focus_app_usages ORDER BY startTimestamp DESC")
    fun getAllAppUsages(): Flow<List<FocusAppUsageEntity>>

    @Query("SELECT * FROM focus_app_usages ORDER BY startTimestamp DESC")
    suspend fun getAllAppUsagesSync(): List<FocusAppUsageEntity>

    // =========================================================================
    // FOCUS EVENTS
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FocusSessionEventEntity): Long

    @Query("SELECT * FROM focus_session_events WHERE sessionId = :sessionId ORDER BY eventTimestamp ASC")
    fun getEventsForSession(sessionId: Long): Flow<List<FocusSessionEventEntity>>

    @Query("SELECT * FROM focus_session_events WHERE sessionId = :sessionId ORDER BY eventTimestamp ASC")
    suspend fun getEventsForSessionSync(sessionId: Long): List<FocusSessionEventEntity>

    // =========================================================================
    // APP CLASSIFICATIONS
    // =========================================================================

    @Query("SELECT * FROM app_classifications WHERE packageName = :packageName")
    suspend fun getClassification(packageName: String): AppClassificationEntity?

    @Query("SELECT * FROM app_classifications ORDER BY applicationName ASC")
    fun getAllClassifications(): Flow<List<AppClassificationEntity>>

    @Query("SELECT * FROM app_classifications ORDER BY applicationName ASC")
    suspend fun getAllClassificationsSync(): List<AppClassificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateClassification(classification: AppClassificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassifications(classifications: List<AppClassificationEntity>)

    @Query("DELETE FROM app_classifications WHERE packageName = :packageName")
    suspend fun deleteClassification(packageName: String)
}
