package com.naman.focusos.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// =============================================================================
// ENUMS
// =============================================================================

enum class SessionStatus {
    IDLE,
    ACTIVE,
    PAUSED,
    STOPPED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(value: String): SessionStatus = try {
            valueOf(value.uppercase())
        } catch (e: Exception) {
            IDLE
        }
    }
}

enum class AppClassification(val displayName: String) {
    FOCUS_APP("Focus OS"),
    PRODUCTIVE_APP("Productive"),
    NEUTRAL_APP("Neutral"),
    DISTRACTING_APP("Distraction"),
    PAUSED_SESSION_USAGE("Paused Usage"),
    UNKNOWN_APP("Unknown"),
    SYSTEM_APP("System");

    companion object {
        fun fromString(value: String): AppClassification = try {
            valueOf(value.uppercase())
        } catch (e: Exception) {
            UNKNOWN_APP
        }
    }
}

enum class SessionEventType {
    SESSION_STARTED,
    SESSION_PAUSED,
    SESSION_RESUMED,
    SESSION_STOPPED,
    SESSION_COMPLETED,
    APP_OPENED,
    APP_CLOSED,
    RETURNED_TO_FOCUS,
    MONITORING_INTERRUPTED,
    MONITORING_RESTORED;

    companion object {
        fun fromString(value: String): SessionEventType = try {
            valueOf(value.uppercase())
        } catch (e: Exception) {
            SESSION_STARTED
        }
    }
}

enum class MonitoringStatus(val displayName: String) {
    NOT_STARTED("Not Started"),
    MONITORING_ACTIVE("Active"),
    MONITORING_PAUSED("Paused"),
    PERMISSION_DENIED("No Permission"),
    MONITORING_GAP("Monitoring Gap"),
    DISABLED("Disabled"),
    STOPPED("Stopped");

    companion object {
        fun fromString(value: String): MonitoringStatus = try {
            valueOf(value.uppercase())
        } catch (e: Exception) {
            NOT_STARTED
        }
    }
}

// =============================================================================
// ROOM ENTITIES
// =============================================================================

@Entity(
    tableName = "focus_sessions",
    indices = [
        Index("linkedTaskId"),
        Index("startTimestamp"),
        Index("sessionStatus")
    ],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTaskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val linkedTaskId: Long? = null,
    val category: String? = null,
    val targetDurationSeconds: Long = 0L,         // 0 = open-ended / free session
    val startTimestamp: Long = 0L,                 // System.currentTimeMillis()
    val endTimestamp: Long? = null,                // System.currentTimeMillis() when finished
    val totalElapsedSeconds: Long = 0L,
    val focusedDurationSeconds: Long = 0L,
    val pausedDurationSeconds: Long = 0L,
    val distractedDurationSeconds: Long = 0L,
    val productiveAppDurationSeconds: Long = 0L,
    val sessionStatus: SessionStatus = SessionStatus.IDLE,
    val monitoringEnabled: Boolean = true,
    val monitoringStatus: String = MonitoringStatus.NOT_STARTED.name,
    val distractionCount: Int = 0,
    val appSwitchCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "focus_session_pauses",
    indices = [
        Index("sessionId"),
        Index("pauseTimestamp")
    ],
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FocusPauseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val pauseTimestamp: Long,                      // Wall-clock millis
    val resumeTimestamp: Long? = null,             // Wall-clock millis when resumed
    val pauseDurationSeconds: Long = 0L,
    val reason: String? = null
)

@Entity(
    tableName = "focus_app_usages",
    indices = [
        Index("sessionId"),
        Index("packageName"),
        Index("startTimestamp")
    ],
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FocusAppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val packageName: String,
    val applicationName: String,
    val classification: AppClassification,
    val startTimestamp: Long,                      // Wall-clock millis
    val endTimestamp: Long,                        // Wall-clock millis
    val durationSeconds: Long = 0L,
    val sessionState: String,                      // "ACTIVE" or "PAUSED"
    val monitoringConfidence: String = "HIGH"      // "HIGH", "ESTIMATED", "GAP"
)

@Entity(
    tableName = "focus_session_events",
    indices = [
        Index("sessionId"),
        Index("eventTimestamp")
    ],
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FocusSessionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val eventType: SessionEventType,
    val eventTimestamp: Long,
    val relatedAppPackage: String? = null,
    val metadata: String? = null
)

@Entity(tableName = "app_classifications")
data class AppClassificationEntity(
    @PrimaryKey
    val packageName: String,
    val applicationName: String,
    val classification: AppClassification,
    val isExcluded: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
