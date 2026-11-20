package com.naman.focusos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurrence_rules")
data class RecurrenceRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: RecurrenceType,
    val interval: Int = 1,                 // every N days/weeks/months/years
    val daysOfWeekMask: Int = 0,           // Bitmask: bit 1 = Monday ... bit 7 = Sunday
    val dayOfMonth: Int? = null,           // e.g. 15 for 15th of month
    val nthWeekday: Int? = null,           // 1 for 1st, 2 for 2nd, -1 for last
    val weekdayForNth: Int? = null,        // 1=Mon .. 7=Sun
    val monthOfYear: Int? = null,          // 1=Jan .. 12=Dec (for yearly)
    val startDate: String,                 // "YYYY-MM-DD"
    val endDate: String? = null,           // optional "YYYY-MM-DD"
    val exceptionDates: String = ""        // comma-separated "YYYY-MM-DD" dates to skip
) {
    fun isDayOfWeekSelected(dayOfWeek: Int): Boolean {
        // dayOfWeek: 1 = Monday .. 7 = Sunday
        val bit = 1 shl (dayOfWeek - 1)
        return (daysOfWeekMask and bit) != 0
    }

    fun hasException(dateStr: String): Boolean {
        if (exceptionDates.isBlank()) return false
        return exceptionDates.split(",").map { it.trim() }.contains(dateStr)
    }

    companion object {
        fun createWeeklyMask(days: List<Int>): Int {
            var mask = 0
            for (d in days) {
                if (d in 1..7) {
                    mask = mask or (1 shl (d - 1))
                }
            }
            return mask
        }

        fun getSelectedDays(mask: Int): List<Int> {
            val list = mutableListOf<Int>()
            for (i in 1..7) {
                if ((mask and (1 shl (i - 1))) != 0) {
                    list.add(i)
                }
            }
            return list
        }

        fun parseWeeklyMask(mask: Int): List<Int> = getSelectedDays(mask)
    }
}
