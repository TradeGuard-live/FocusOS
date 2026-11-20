package com.naman.focusos.data.model

enum class Priority(val title: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4);

    companion object {
        fun fromString(value: String?): Priority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.title.equals(value, ignoreCase = true) }
                ?: MEDIUM
        }
    }
}

enum class TaskStatus(val title: String) {
    UPCOMING("Upcoming"),
    IN_PROGRESS("In Progress"),
    COMPLETED_ON_TIME("Completed on Time"),
    COMPLETED_LATE("Completed Late"),
    MISSED("Missed"),
    SKIPPED("Skipped");

    fun isCompleted(): Boolean = this == COMPLETED_ON_TIME || this == COMPLETED_LATE
    fun isFinished(): Boolean = isCompleted() || this == MISSED || this == SKIPPED

    companion object {
        fun fromString(value: String?): TaskStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.title.equals(value, ignoreCase = true) }
                ?: UPCOMING
        }
    }
}

enum class RecurrenceType(val title: String) {
    NONE("One-Time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY_DATE("Monthly on Day"),
    MONTHLY_DAY_OF_WEEK("Monthly on Weekday"),
    YEARLY("Yearly")
}

enum class HabitFrequency(val title: String) {
    DAILY("Daily"),
    SPECIFIC_DAYS("Specific Weekdays"),
    CUSTOM("Custom Frequency")
}
