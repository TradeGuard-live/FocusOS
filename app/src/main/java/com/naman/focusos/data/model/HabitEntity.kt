package com.naman.focusos.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [Index("categoryId"), Index("isActive")]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val categoryId: Long = 4,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val daysOfWeekMask: Int = 127,              // 127 = 1111111 (all 7 days)
    val targetValue: Float = 1.0f,
    val unit: String = "times",
    val colorHex: String = "#38BDF8",
    val iconName: String = "CheckCircle",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun isScheduledForDay(dayOfWeek: Int): Boolean {
        // 1 = Monday .. 7 = Sunday
        val bit = 1 shl (dayOfWeek - 1)
        return (daysOfWeekMask and bit) != 0
    }
}

@Entity(
    tableName = "habit_completions",
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index("date")
    ],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String,                          // "YYYY-MM-DD"
    val isCompleted: Boolean = true,
    val actualValue: Float = 1.0f,
    val notes: String = "",
    val completedAt: Long = System.currentTimeMillis()
)
