package com.naman.focusos.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(
    tableName = "tasks",
    indices = [
        Index("scheduledDate"),
        Index("status"),
        Index("categoryId"),
        Index("recurrenceRuleId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = RecurrenceRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceRuleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val categoryId: Long = 1,
    val priority: Priority = Priority.MEDIUM,
    val scheduledDate: String,                 // "YYYY-MM-DD"
    val startTime: String? = null,              // "HH:mm" (24-hour)
    val durationMinutes: Int = 30,             // Estimated planned duration
    val deadlineTime: String? = null,           // "HH:mm" (24-hour)
    val reminderMinutesBefore: Int = 10,       // Advance reminder (e.g. 10m)
    val recurrenceRuleId: Long? = null,        // Null if one-time
    val status: TaskStatus = TaskStatus.UPCOMING,
    val actualCompletionTime: Long? = null,    // Epoch millis when completed
    val notes: String = "",
    val notificationTime1: String? = null,     // "HH:mm" 1st notification time
    val notificationTime2: String? = null,     // "HH:mm" 2nd notification time (if not completed)
    val notificationTime3: String? = null,     // "HH:mm" 3rd notification time (if not completed)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Compute effective status dynamically based on current time
     * If user marked it manually as Completed, Skipped, etc., respect that.
     * Otherwise check if deadline has passed or if task is in progress.
     */
    fun evaluateEffectiveStatus(now: LocalDateTime = LocalDateTime.now()): TaskStatus {
        // If already completed or skipped, return as is
        if (status.isFinished()) {
            return status
        }

        val taskDate = try {
            LocalDate.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            return status
        }

        val today = now.toLocalDate()
        val currentTime = now.toLocalTime()

        // If scheduled date is in the future, it is UPCOMING
        if (taskDate.isAfter(today)) {
            return TaskStatus.UPCOMING
        }

        // If scheduled date was in the past and task wasn't completed, it is MISSED
        if (taskDate.isBefore(today)) {
            return TaskStatus.MISSED
        }

        // For today: check deadline time if specified
        if (deadlineTime != null) {
            val deadline = try {
                LocalTime.parse(deadlineTime, DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                null
            }
            if (deadline != null && currentTime.isAfter(deadline)) {
                return TaskStatus.MISSED
            }
        }

        // Check if start time is active
        if (startTime != null) {
            val start = try {
                LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                null
            }
            if (start != null) {
                val end = start.plusMinutes(durationMinutes.toLong())
                if (currentTime.isAfter(start) && currentTime.isBefore(end)) {
                    return TaskStatus.IN_PROGRESS
                }
            }
        }

        return TaskStatus.UPCOMING
    }

    /**
     * Complete task with precise rule:
     * If completed before or at deadline -> COMPLETED_ON_TIME
     * If completed after deadline -> COMPLETED_LATE
     */
    fun markCompleted(completedAt: LocalDateTime = LocalDateTime.now()): TaskEntity {
        val taskDate = try {
            LocalDate.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            completedAt.toLocalDate()
        }

        var isLate = false
        if (completedAt.toLocalDate().isAfter(taskDate)) {
            isLate = true
        } else if (completedAt.toLocalDate() == taskDate && deadlineTime != null) {
            val deadline = try {
                LocalTime.parse(deadlineTime, DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                null
            }
            if (deadline != null && completedAt.toLocalTime().isAfter(deadline)) {
                isLate = true
            }
        }

        val newStatus = if (isLate) TaskStatus.COMPLETED_LATE else TaskStatus.COMPLETED_ON_TIME
        val completionEpoch = completedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return copy(
            status = newStatus,
            actualCompletionTime = completionEpoch,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Formats actualCompletionTime into a user-friendly time string (e.g. "11:15 AM" or "11:15").
     */
    fun getFormattedCompletionTime(is24Hour: Boolean = false): String? {
        val epoch = actualCompletionTime ?: return null
        return try {
            val instant = Instant.ofEpochMilli(epoch)
            val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
            ldt.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
        } catch (e: Exception) {
            null
        }
    }

    fun markIncomplete(): TaskEntity {
        return copy(
            status = TaskStatus.UPCOMING,
            actualCompletionTime = null,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun markSkipped(): TaskEntity {
        return copy(
            status = TaskStatus.SKIPPED,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Intelligent Chronological Comparator for Focus OS tasks:
         * 1. Tasks with scheduled times (startTime != null && startTime.isNotBlank()) appear FIRST.
         * 2. Scheduled tasks are sorted in ascending chronological order ("05:00" < "06:00" < "08:30" ...).
         * 3. Tasks without scheduled times (startTime == null || startTime.isBlank()) appear AFTER all scheduled tasks.
         * 4. When two tasks have the same scheduled time (or both have no scheduled time),
         *    deterministic secondary sorting applies:
         *    a. Priority level descending (CRITICAL -> HIGH -> MEDIUM -> LOW)
         *    b. Title ascending (case-insensitive)
         *    c. Task ID ascending
         *
         * Guarantees:
         * - Does NOT use task creation time (createdAt) as a replacement for missing scheduled time.
         * - Does NOT let hard deadlines (deadlineTime) override scheduled-time ordering.
         * - Preserves existing task data and completion status.
         * - Completely deterministic across loads and refreshes.
         */
        val ChronologicalComparator: Comparator<TaskEntity> = Comparator { a, b ->
            val aTime = normalizeTimeString(a.startTime)
            val bTime = normalizeTimeString(b.startTime)

            val aHasTime = aTime != null
            val bHasTime = bTime != null

            when {
                aHasTime && !bHasTime -> -1
                !aHasTime && bHasTime -> 1
                aHasTime && bHasTime -> {
                    val timeComp = aTime!!.compareTo(bTime!!)
                    if (timeComp != 0) {
                        timeComp
                    } else {
                        compareSecondary(a, b)
                    }
                }
                else -> {
                    compareSecondary(a, b)
                }
            }
        }

        private fun normalizeTimeString(time: String?): String? {
            if (time.isNullOrBlank()) return null
            val parts = time.trim().split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                if (h != null && m != null) {
                    return String.format(java.util.Locale.US, "%02d:%02d", h, m)
                }
            }
            return time.trim()
        }

        private fun compareSecondary(a: TaskEntity, b: TaskEntity): Int {
            val priorityComp = b.priority.level.compareTo(a.priority.level)
            if (priorityComp != 0) return priorityComp

            val titleComp = a.title.compareTo(b.title, ignoreCase = true)
            if (titleComp != 0) return titleComp

            return a.id.compareTo(b.id)
        }
    }
}

fun List<TaskEntity>.sortedChronologically(): List<TaskEntity> =
    this.sortedWith(TaskEntity.ChronologicalComparator)
