package com.naman.focusos.data.db

import androidx.room.TypeConverter
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.RecurrenceType
import com.naman.focusos.data.model.SessionEventType
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.data.model.TaskStatus

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.fromString(value)

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.fromString(value)

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType): String = type.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = try {
        RecurrenceType.valueOf(value)
    } catch (e: Exception) {
        RecurrenceType.NONE
    }

    @TypeConverter
    fun fromHabitFrequency(freq: HabitFrequency): String = freq.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = try {
        HabitFrequency.valueOf(value)
    } catch (e: Exception) {
        HabitFrequency.DAILY
    }

    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.fromString(value)

    @TypeConverter
    fun fromAppClassification(classification: AppClassification): String = classification.name

    @TypeConverter
    fun toAppClassification(value: String): AppClassification = AppClassification.fromString(value)

    @TypeConverter
    fun fromSessionEventType(eventType: SessionEventType): String = eventType.name

    @TypeConverter
    fun toSessionEventType(value: String): SessionEventType = SessionEventType.fromString(value)
}
