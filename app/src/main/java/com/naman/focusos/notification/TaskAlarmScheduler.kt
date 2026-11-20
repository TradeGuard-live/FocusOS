package com.naman.focusos.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.receiver.TaskAlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TaskAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun scheduleTaskReminders(task: TaskEntity) {
        cancelTaskReminders(task.id)
        if (task.status.isFinished()) {
            return
        }

        val taskDate = try {
            LocalDate.parse(task.scheduledDate, dateFormatter)
        } catch (e: Exception) {
            return
        }

        // 1. Advance reminder & Start time reminder
        if (task.startTime != null) {
            val startTime = try {
                LocalTime.parse(task.startTime, timeFormatter)
            } catch (e: Exception) {
                null
            }

            if (startTime != null) {
                val startDateTime = LocalDateTime.of(taskDate, startTime)
                val advanceMinutes = task.reminderMinutesBefore.coerceAtLeast(0)

                if (advanceMinutes > 0) {
                    val reminderTime = startDateTime.minusMinutes(advanceMinutes.toLong())
                    scheduleAlarm(
                        triggerTimeMillis = toEpochMillis(reminderTime),
                        requestCode = (task.id * 10 + 1).toInt(),
                        taskId = task.id,
                        title = "Upcoming: ${task.title}",
                        message = "Starting in $advanceMinutes min at ${task.startTime}",
                        type = "START_ADVANCE"
                    )
                }

                // Exact start time
                scheduleAlarm(
                    triggerTimeMillis = toEpochMillis(startDateTime),
                    requestCode = (task.id * 10 + 2).toInt(),
                    taskId = task.id,
                    title = "Time to Focus: ${task.title}",
                    message = task.description.ifBlank { "Scheduled focus task starting now." },
                    type = "START_EXACT"
                )
            }
        }

        // 2. Deadline reminder
        if (task.deadlineTime != null) {
            val deadlineTime = try {
                LocalTime.parse(task.deadlineTime, timeFormatter)
            } catch (e: Exception) {
                null
            }

            if (deadlineTime != null) {
                val deadlineDateTime = LocalDateTime.of(taskDate, deadlineTime)
                scheduleAlarm(
                    triggerTimeMillis = toEpochMillis(deadlineDateTime),
                    requestCode = (task.id * 10 + 3).toInt(),
                    taskId = task.id,
                    title = "Deadline Reached: ${task.title}",
                    message = "Task deadline is here! Check off to complete.",
                    type = "DEADLINE"
                )
            }
        }

        // 3. Custom Task Notifications (1st, 2nd, and 3rd reminder if not completed)
        if (!task.notificationTime1.isNullOrBlank()) {
            val notifTime1 = try {
                LocalTime.parse(task.notificationTime1, timeFormatter)
            } catch (e: Exception) {
                null
            }
            if (notifTime1 != null) {
                val dateTime1 = LocalDateTime.of(taskDate, notifTime1)
                scheduleAlarm(
                    triggerTimeMillis = toEpochMillis(dateTime1),
                    requestCode = (task.id * 10 + 4).toInt(),
                    taskId = task.id,
                    title = "Task Reminder: ${task.title}",
                    message = task.description.ifBlank { "Reminder to work on '${task.title}'." },
                    type = "CUSTOM_REMINDER_1"
                )
            }
        }

        if (!task.notificationTime2.isNullOrBlank()) {
            val notifTime2 = try {
                LocalTime.parse(task.notificationTime2, timeFormatter)
            } catch (e: Exception) {
                null
            }
            if (notifTime2 != null) {
                val dateTime2 = LocalDateTime.of(taskDate, notifTime2)
                scheduleAlarm(
                    triggerTimeMillis = toEpochMillis(dateTime2),
                    requestCode = (task.id * 10 + 5).toInt(),
                    taskId = task.id,
                    title = "2nd Reminder: ${task.title}",
                    message = "Task '${task.title}' is still incomplete! Tap to complete or view.",
                    type = "CUSTOM_REMINDER_2"
                )
            }
        }

        if (!task.notificationTime3.isNullOrBlank()) {
            val notifTime3 = try {
                LocalTime.parse(task.notificationTime3, timeFormatter)
            } catch (e: Exception) {
                null
            }
            if (notifTime3 != null) {
                val dateTime3 = LocalDateTime.of(taskDate, notifTime3)
                scheduleAlarm(
                    triggerTimeMillis = toEpochMillis(dateTime3),
                    requestCode = (task.id * 10 + 6).toInt(),
                    taskId = task.id,
                    title = "Final Reminder: ${task.title}",
                    message = "Final alert: '${task.title}' has not been completed yet today!",
                    type = "CUSTOM_REMINDER_3"
                )
            }
        }
    }

    fun cancelTaskReminders(taskId: Long) {
        for (suffix in 1..6) {
            val requestCode = (taskId * 10 + suffix).toInt()
            val intent = Intent(context, TaskAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun scheduleAlarm(
        triggerTimeMillis: Long,
        requestCode: Int,
        taskId: Long,
        title: String,
        message: String,
        type: String
    ) {
        val now = System.currentTimeMillis()
        if (triggerTimeMillis <= now) return // Past event, do not trigger

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_ALARM
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskAlarmReceiver.EXTRA_TITLE, title)
            putExtra(TaskAlarmReceiver.EXTRA_MESSAGE, message)
            putExtra(TaskAlarmReceiver.EXTRA_ALARM_TYPE, type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }

    private fun toEpochMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
