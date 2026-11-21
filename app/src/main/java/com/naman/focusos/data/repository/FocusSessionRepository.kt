package com.naman.focusos.data.repository

import android.content.Context
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.AppClassificationEntity
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
import com.naman.focusos.data.model.MonitoringStatus
import com.naman.focusos.data.model.SessionEventType
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FocusSessionRepository(private val context: Context) {

    private val db = FocusDatabase.getInstance(context)
    private val focusDao = db.focusSessionDao()
    private val taskDao = db.taskDao()

    fun getAllSessions(): Flow<List<FocusSessionEntity>> = focusDao.getAllSessions()

    fun getActiveSessionFlow(): Flow<FocusSessionEntity?> = focusDao.getActiveSessionFlow()

    suspend fun getActiveSession(): FocusSessionEntity? = withContext(Dispatchers.IO) {
        focusDao.getActiveSession()
    }

    suspend fun getSessionById(sessionId: Long): FocusSessionEntity? = withContext(Dispatchers.IO) {
        focusDao.getSessionById(sessionId)
    }

    fun getSessionFlowById(sessionId: Long): Flow<FocusSessionEntity?> = focusDao.getSessionFlowById(sessionId)

    fun getPausesForSession(sessionId: Long): Flow<List<FocusPauseEntity>> = focusDao.getPausesForSession(sessionId)

    suspend fun getPausesForSessionSync(sessionId: Long): List<FocusPauseEntity> = withContext(Dispatchers.IO) {
        focusDao.getPausesForSessionSync(sessionId)
    }

    fun getAppUsagesForSession(sessionId: Long): Flow<List<FocusAppUsageEntity>> = focusDao.getAppUsagesForSession(sessionId)

    suspend fun getAppUsagesForSessionSync(sessionId: Long): List<FocusAppUsageEntity> = withContext(Dispatchers.IO) {
        focusDao.getAppUsagesForSessionSync(sessionId)
    }

    fun getAllAppUsages(): Flow<List<FocusAppUsageEntity>> = focusDao.getAllAppUsages()

    suspend fun getAllAppUsagesSync(): List<FocusAppUsageEntity> = withContext(Dispatchers.IO) {
        focusDao.getAllAppUsagesSync()
    }

    fun getSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>> = focusDao.getSessionsForTask(taskId)

    suspend fun getSessionsForTaskSync(taskId: Long): List<FocusSessionEntity> = withContext(Dispatchers.IO) {
        focusDao.getSessionsForTaskSync(taskId)
    }

    fun getEventsForSession(sessionId: Long): Flow<List<FocusSessionEventEntity>> = focusDao.getEventsForSession(sessionId)

    suspend fun getEventsForSessionSync(sessionId: Long): List<FocusSessionEventEntity> = withContext(Dispatchers.IO) {
        focusDao.getEventsForSessionSync(sessionId)
    }

    fun getAllClassifications(): Flow<List<AppClassificationEntity>> = focusDao.getAllClassifications()

    suspend fun getAllClassificationsSync(): List<AppClassificationEntity> = withContext(Dispatchers.IO) {
        focusDao.getAllClassificationsSync()
    }

    suspend fun startSession(
        title: String,
        description: String?,
        linkedTaskId: Long?,
        category: String?,
        targetDurationSeconds: Long,
        monitoringEnabled: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = FocusSessionEntity(
            title = title.ifBlank { "Focus Session" },
            description = description,
            linkedTaskId = linkedTaskId,
            category = category,
            targetDurationSeconds = targetDurationSeconds,
            startTimestamp = now,
            sessionStatus = SessionStatus.ACTIVE,
            monitoringEnabled = monitoringEnabled,
            monitoringStatus = if (monitoringEnabled) MonitoringStatus.MONITORING_ACTIVE.name else MonitoringStatus.DISABLED.name,
            createdAt = now,
            updatedAt = now
        )
        val id = focusDao.insertSession(session)
        focusDao.insertEvent(
            FocusSessionEventEntity(
                sessionId = id,
                eventType = SessionEventType.SESSION_STARTED,
                eventTimestamp = now,
                metadata = "Started session: ${session.title}"
            )
        )
        id
    }

    suspend fun pauseSession(sessionId: Long, reason: String? = null) = withContext(Dispatchers.IO) {
        val session = focusDao.getSessionById(sessionId) ?: return@withContext
        val now = System.currentTimeMillis()
        focusDao.updateSession(
            session.copy(
                sessionStatus = SessionStatus.PAUSED,
                updatedAt = now
            )
        )
        focusDao.insertPause(
            FocusPauseEntity(
                sessionId = sessionId,
                pauseTimestamp = now,
                reason = reason
            )
        )
        focusDao.insertEvent(
            FocusSessionEventEntity(
                sessionId = sessionId,
                eventType = SessionEventType.SESSION_PAUSED,
                eventTimestamp = now,
                metadata = reason
            )
        )
    }

    suspend fun resumeSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = focusDao.getSessionById(sessionId) ?: return@withContext
        val now = System.currentTimeMillis()
        val activePause = focusDao.getActivePauseForSession(sessionId)
        if (activePause != null) {
            val pauseDuration = ((now - activePause.pauseTimestamp) / 1000).coerceAtLeast(0)
            focusDao.updatePause(
                activePause.copy(
                    resumeTimestamp = now,
                    pauseDurationSeconds = pauseDuration
                )
            )
            val updatedPausedDuration = session.pausedDurationSeconds + pauseDuration
            focusDao.updateSession(
                session.copy(
                    sessionStatus = SessionStatus.ACTIVE,
                    pausedDurationSeconds = updatedPausedDuration,
                    updatedAt = now
                )
            )
        } else {
            focusDao.updateSession(
                session.copy(
                    sessionStatus = SessionStatus.ACTIVE,
                    updatedAt = now
                )
            )
        }
        focusDao.insertEvent(
            FocusSessionEventEntity(
                sessionId = sessionId,
                eventType = SessionEventType.SESSION_RESUMED,
                eventTimestamp = now
            )
        )
    }

    suspend fun checkpointSession(sessionId: Long, snapshot: LiveTimerSnapshot) = withContext(Dispatchers.IO) {
        val session = focusDao.getSessionById(sessionId) ?: return@withContext
        val now = System.currentTimeMillis()
        focusDao.updateSession(
            session.copy(
                focusedDurationSeconds = snapshot.focusedSeconds,
                pausedDurationSeconds = snapshot.pausedSeconds,
                totalElapsedSeconds = snapshot.totalElapsedSeconds,
                distractedDurationSeconds = snapshot.distractedSeconds,
                productiveAppDurationSeconds = snapshot.productiveSeconds,
                distractionCount = snapshot.distractionCount,
                appSwitchCount = snapshot.appSwitchCount,
                updatedAt = now
            )
        )
    }

    suspend fun recordAppUsages(sessionId: Long, usages: List<FocusAppUsageEntity>) = withContext(Dispatchers.IO) {
        if (usages.isEmpty()) return@withContext
        focusDao.insertAppUsages(usages)
        for (u in usages) {
            focusDao.insertEvent(
                FocusSessionEventEntity(
                    sessionId = sessionId,
                    eventType = SessionEventType.APP_OPENED,
                    eventTimestamp = u.startTimestamp,
                    relatedAppPackage = u.packageName,
                    metadata = "${u.applicationName} (${u.durationSeconds}s) [${u.classification.name}]"
                )
            )
            focusDao.insertEvent(
                FocusSessionEventEntity(
                    sessionId = sessionId,
                    eventType = SessionEventType.RETURNED_TO_FOCUS,
                    eventTimestamp = u.endTimestamp,
                    relatedAppPackage = u.packageName
                )
            )
        }
    }

    suspend fun completeSession(
        sessionId: Long,
        snapshot: LiveTimerSnapshot,
        markLinkedTaskAsCompleted: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val session = focusDao.getSessionById(sessionId) ?: return@withContext
        val now = System.currentTimeMillis()
        focusDao.updateSession(
            session.copy(
                sessionStatus = SessionStatus.COMPLETED,
                endTimestamp = now,
                focusedDurationSeconds = snapshot.focusedSeconds,
                pausedDurationSeconds = snapshot.pausedSeconds,
                totalElapsedSeconds = snapshot.totalElapsedSeconds,
                distractedDurationSeconds = snapshot.distractedSeconds,
                productiveAppDurationSeconds = snapshot.productiveSeconds,
                distractionCount = snapshot.distractionCount,
                appSwitchCount = snapshot.appSwitchCount,
                monitoringStatus = MonitoringStatus.STOPPED.name,
                updatedAt = now
            )
        )
        focusDao.insertEvent(
            FocusSessionEventEntity(
                sessionId = sessionId,
                eventType = SessionEventType.SESSION_COMPLETED,
                eventTimestamp = now,
                metadata = "Focused for ${LiveTimerSnapshot.formatDuration(snapshot.focusedSeconds)}"
            )
        )

        if (markLinkedTaskAsCompleted && session.linkedTaskId != null) {
            val task = taskDao.getTaskById(session.linkedTaskId)
            if (task != null) {
                taskDao.updateTask(
                    task.copy(
                        status = TaskStatus.COMPLETED_ON_TIME,
                        actualCompletionTime = now
                    )
                )
            }
        }
    }

    suspend fun stopSession(sessionId: Long, snapshot: LiveTimerSnapshot) = withContext(Dispatchers.IO) {
        val session = focusDao.getSessionById(sessionId) ?: return@withContext
        val now = System.currentTimeMillis()
        focusDao.updateSession(
            session.copy(
                sessionStatus = SessionStatus.STOPPED,
                endTimestamp = now,
                focusedDurationSeconds = snapshot.focusedSeconds,
                pausedDurationSeconds = snapshot.pausedSeconds,
                totalElapsedSeconds = snapshot.totalElapsedSeconds,
                distractedDurationSeconds = snapshot.distractedSeconds,
                productiveAppDurationSeconds = snapshot.productiveSeconds,
                distractionCount = snapshot.distractionCount,
                appSwitchCount = snapshot.appSwitchCount,
                monitoringStatus = MonitoringStatus.STOPPED.name,
                updatedAt = now
            )
        )
        focusDao.insertEvent(
            FocusSessionEventEntity(
                sessionId = sessionId,
                eventType = SessionEventType.SESSION_STOPPED,
                eventTimestamp = now
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        focusDao.deleteSessionById(sessionId)
    }

    suspend fun updateAppClassification(
        packageName: String,
        applicationName: String,
        classification: AppClassification,
        isExcluded: Boolean = false
    ) = withContext(Dispatchers.IO) {
        focusDao.insertOrUpdateClassification(
            AppClassificationEntity(
                packageName = packageName,
                applicationName = applicationName,
                classification = classification,
                isExcluded = isExcluded,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun seedDefaultClassifications() = withContext(Dispatchers.IO) {
        val current = focusDao.getAllClassificationsSync()
        if (current.isEmpty()) {
            val defaults = listOf(
                AppClassificationEntity("com.naman.focusos", "Focus OS", AppClassification.FOCUS_APP),
                AppClassificationEntity("com.android.chrome", "Chrome", AppClassification.NEUTRAL_APP),
                AppClassificationEntity("com.google.android.youtube", "YouTube", AppClassification.DISTRACTING_APP),
                AppClassificationEntity("com.whatsapp", "WhatsApp", AppClassification.NEUTRAL_APP),
                AppClassificationEntity("com.instagram.android", "Instagram", AppClassification.DISTRACTING_APP),
                AppClassificationEntity("com.twitter.android", "X", AppClassification.DISTRACTING_APP),
                AppClassificationEntity("com.github.android", "GitHub", AppClassification.PRODUCTIVE_APP),
                AppClassificationEntity("com.slack", "Slack", AppClassification.PRODUCTIVE_APP),
                AppClassificationEntity("com.google.android.gm", "Gmail", AppClassification.PRODUCTIVE_APP),
                AppClassificationEntity("com.google.android.apps.docs", "Google Docs", AppClassification.PRODUCTIVE_APP)
            )
            focusDao.insertClassifications(defaults)
        }
    }
}
