package com.naman.focusos.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "timetable_entries",
    indices = [Index("categoryId"), Index("startTime")]
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: String,                     // "HH:mm" (24-hour)
    val durationMinutes: Int = 30,             // Duration in minutes
    val categoryId: Long = 1,
    val daysOfWeekMask: Int = 127,             // Default all 7 days
    val isEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 5,        // Advance reminder minutes
    val sortOrder: Int = 0
) {
    fun getEndTime(): String {
        return try {
            val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val end = start.plusMinutes(durationMinutes.toLong())
            end.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            startTime
        }
    }

    fun isScheduledForDay(dayOfWeek: Int): Boolean {
        val bit = 1 shl (dayOfWeek - 1)
        return (daysOfWeekMask and bit) != 0
    }

    companion object {
        fun defaultTimetable(): List<TimetableEntryEntity> = listOf(
            TimetableEntryEntity(title = "Wake up & Hydrate", startTime = "06:00", durationMinutes = 30, categoryId = 4, sortOrder = 1),
            TimetableEntryEntity(title = "Workout & Cardio", startTime = "06:30", durationMinutes = 60, categoryId = 3, sortOrder = 2),
            TimetableEntryEntity(title = "Healthy Breakfast", startTime = "07:30", durationMinutes = 30, categoryId = 4, sortOrder = 3),
            TimetableEntryEntity(title = "Deep Focus / College / Work", startTime = "08:00", durationMinutes = 300, categoryId = 1, sortOrder = 4),
            TimetableEntryEntity(title = "Lunch & Rest", startTime = "13:00", durationMinutes = 60, categoryId = 4, sortOrder = 5),
            TimetableEntryEntity(title = "Project Work & DSA", startTime = "17:00", durationMinutes = 150, categoryId = 5, sortOrder = 6),
            TimetableEntryEntity(title = "Dinner & Wind Down", startTime = "19:30", durationMinutes = 60, categoryId = 4, sortOrder = 7),
            TimetableEntryEntity(title = "Reading & Sleep", startTime = "22:00", durationMinutes = 60, categoryId = 2, sortOrder = 8)
        )
    }
}
