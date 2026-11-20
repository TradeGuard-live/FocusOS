package com.naman.focusos.domain.excel

import android.content.Context
import android.net.Uri
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.data.model.TimetableEntryEntity
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HabitImportPreview(
    val sheetName: String,
    val habitTitles: List<String>,
    val totalCompletionsFound: Int,
    val parsedEntries: List<ParsedHabitData>
)

data class ParsedHabitData(
    val habitTitle: String,
    val completedDays: List<Int> // 1..31
)

data class FullBackupData(
    val exportDate: String,
    val tasks: List<TaskEntity>,
    val recurrenceRules: List<RecurrenceRuleEntity>,
    val habits: List<HabitEntity>,
    val habitCompletions: List<HabitCompletionEntity>,
    val timetableEntries: List<TimetableEntryEntity>,
    val categories: List<CategoryEntity>
)

object ExcelManager {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Inspect and parse Habit_Tracker.xlsx workbook
     */
    fun parseHabitTrackerWorkbook(inputStream: InputStream): HabitImportPreview {
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheet("Habit Tracker - Month") ?: workbook.getSheetAt(0)
        val sheetName = sheet.sheetName

        val parsedData = mutableListOf<ParsedHabitData>()
        val habitTitles = mutableListOf<String>()
        var totalCompletions = 0

        // Find header row with day numbers 1..31
        val headerRow = sheet.getRow(0) ?: throw IllegalArgumentException("Workbook sheet has no header row")
        val dayColMap = mutableMapOf<Int, Int>() // Col index -> Day number (1..31)

        for (colIdx in 1 until headerRow.lastCellNum) {
            val cell = headerRow.getCell(colIdx) ?: continue
            val num = when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue.toInt()
                CellType.STRING -> cell.stringCellValue.trim().toIntOrNull()
                else -> null
            }
            if (num != null && num in 1..31) {
                dayColMap[colIdx] = num
            }
        }

        // Iterate through habit rows (row 1 to last)
        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val firstCell = row.getCell(0) ?: continue
            val habitTitle = when (firstCell.cellType) {
                CellType.STRING -> firstCell.stringCellValue.trim()
                CellType.NUMERIC -> firstCell.numericCellValue.toString()
                else -> ""
            }
            if (habitTitle.isBlank() || habitTitle.startsWith("Habit", ignoreCase = true)) continue

            habitTitles.add(habitTitle)
            val completedDays = mutableListOf<Int>()

            for ((colIdx, dayNum) in dayColMap) {
                val cell = row.getCell(colIdx) ?: continue
                val isCompleted = when (cell.cellType) {
                    CellType.STRING -> {
                        val txt = cell.stringCellValue.trim()
                        txt == "✔" || txt.equals("x", ignoreCase = true) || txt.equals("yes", ignoreCase = true) || txt == "1" || txt.isNotBlank()
                    }
                    CellType.NUMERIC -> cell.numericCellValue > 0.0
                    CellType.BOOLEAN -> cell.booleanCellValue
                    else -> false
                }
                if (isCompleted) {
                    completedDays.add(dayNum)
                    totalCompletions++
                }
            }

            parsedData.add(ParsedHabitData(habitTitle = habitTitle, completedDays = completedDays))
        }

        workbook.close()
        return HabitImportPreview(
            sheetName = sheetName,
            habitTitles = habitTitles,
            totalCompletionsFound = totalCompletions,
            parsedEntries = parsedData
        )
    }

    /**
     * Export Tasks to CSV format
     */
    fun exportTasksToCsv(tasks: List<TaskEntity>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        writer.appendLine("Task ID,Title,Description,Priority,Scheduled Date,Start Time,Duration (Min),Deadline,Status,Completed At,Notes")

        for (t in tasks) {
            val completedAtStr = t.actualCompletionTime?.toString() ?: ""
            val line = listOf(
                t.id.toString(),
                escapeCsv(t.title),
                escapeCsv(t.description),
                t.priority.name,
                t.scheduledDate,
                t.startTime ?: "",
                t.durationMinutes.toString(),
                t.deadlineTime ?: "",
                t.status.name,
                completedAtStr,
                escapeCsv(t.notes)
            ).joinToString(",")
            writer.appendLine(line)
        }
        writer.flush()
    }

    /**
     * Export Habit Grid to XLSX matching Habit_Tracker.xlsx format
     */
    fun exportHabitsToXlsx(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        yearMonth: java.time.YearMonth,
        outputStream: OutputStream
    ) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Habit Tracker - Month")

        // Header row
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Habit / Day")
        val daysInMonth = yearMonth.lengthOfMonth()
        for (d in 1..daysInMonth) {
            header.createCell(d).setCellValue(d.toDouble())
        }

        // Map completions by habitId and day
        val completionMap = mutableSetOf<Pair<Long, Int>>() // Pair(habitId, dayOfMonth)
        for (c in completions) {
            if (c.isCompleted) {
                try {
                    val date = LocalDate.parse(c.date, dateFormatter)
                    if (date.year == yearMonth.year && date.monthValue == yearMonth.monthValue) {
                        completionMap.add(Pair(c.habitId, date.dayOfMonth))
                    }
                } catch (e: Exception) {}
            }
        }

        // Data rows
        for ((idx, habit) in habits.withIndex()) {
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(habit.title)
            for (d in 1..daysInMonth) {
                val cell = row.createCell(d)
                if (completionMap.contains(Pair(habit.id, d))) {
                    cell.setCellValue("✔")
                } else {
                    cell.setCellValue("")
                }
            }
        }

        workbook.write(outputStream)
        workbook.close()
    }

    /**
     * Full JSON Backup Export
     */
    fun exportFullBackup(backupData: FullBackupData, outputStream: OutputStream) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "FOCUS OS")
        root.put("exportDate", backupData.exportDate)

        // Tasks array
        val tasksArray = JSONArray()
        for (t in backupData.tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("categoryId", t.categoryId)
            obj.put("priority", t.priority.name)
            obj.put("scheduledDate", t.scheduledDate)
            obj.put("startTime", t.startTime ?: JSONObject.NULL)
            obj.put("durationMinutes", t.durationMinutes)
            obj.put("deadlineTime", t.deadlineTime ?: JSONObject.NULL)
            obj.put("reminderMinutesBefore", t.reminderMinutesBefore)
            obj.put("recurrenceRuleId", t.recurrenceRuleId ?: JSONObject.NULL)
            obj.put("status", t.status.name)
            obj.put("actualCompletionTime", t.actualCompletionTime ?: JSONObject.NULL)
            obj.put("notes", t.notes)
            obj.put("notificationTime1", t.notificationTime1 ?: JSONObject.NULL)
            obj.put("notificationTime2", t.notificationTime2 ?: JSONObject.NULL)
            obj.put("notificationTime3", t.notificationTime3 ?: JSONObject.NULL)
            obj.put("createdAt", t.createdAt)
            obj.put("updatedAt", t.updatedAt)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        // Habits array
        val habitsArray = JSONArray()
        for (h in backupData.habits) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("title", h.title)
            obj.put("description", h.description)
            obj.put("categoryId", h.categoryId)
            obj.put("frequency", h.frequency.name)
            obj.put("daysOfWeekMask", h.daysOfWeekMask)
            obj.put("targetValue", h.targetValue.toDouble())
            obj.put("unit", h.unit)
            obj.put("colorHex", h.colorHex)
            obj.put("iconName", h.iconName)
            obj.put("createdAt", h.createdAt)
            obj.put("isActive", h.isActive)
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        // Habit completions
        val completionsArray = JSONArray()
        for (c in backupData.habitCompletions) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("habitId", c.habitId)
            obj.put("date", c.date)
            obj.put("isCompleted", c.isCompleted)
            obj.put("actualValue", c.actualValue.toDouble())
            obj.put("notes", c.notes)
            obj.put("completedAt", c.completedAt)
            completionsArray.put(obj)
        }
        root.put("habitCompletions", completionsArray)

        // Timetable entries
        val timetableArray = JSONArray()
        for (tt in backupData.timetableEntries) {
            val obj = JSONObject()
            obj.put("id", tt.id)
            obj.put("title", tt.title)
            obj.put("description", tt.description)
            obj.put("startTime", tt.startTime)
            obj.put("durationMinutes", tt.durationMinutes)
            obj.put("categoryId", tt.categoryId)
            obj.put("daysOfWeekMask", tt.daysOfWeekMask)
            obj.put("isEnabled", tt.isEnabled)
            obj.put("reminderMinutesBefore", tt.reminderMinutesBefore)
            obj.put("sortOrder", tt.sortOrder)
            timetableArray.put(obj)
        }
        root.put("timetableEntries", timetableArray)

        val writer = outputStream.bufferedWriter()
        writer.write(root.toString(2))
        writer.flush()
    }

    /**
     * Restore Full Backup from JSON
     */
    fun parseFullBackup(inputStream: InputStream): FullBackupData {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)

        val exportDate = root.optString("exportDate", LocalDate.now().toString())

        val tasks = mutableListOf<TaskEntity>()
        val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)
            tasks.add(
                TaskEntity(
                    id = obj.optLong("id", 0),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    categoryId = obj.optLong("categoryId", 1),
                    priority = Priority.fromString(obj.optString("priority")),
                    scheduledDate = obj.getString("scheduledDate"),
                    startTime = if (obj.isNull("startTime")) null else obj.getString("startTime"),
                    durationMinutes = obj.optInt("durationMinutes", 30),
                    deadlineTime = if (obj.isNull("deadlineTime")) null else obj.getString("deadlineTime"),
                    reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 10),
                    recurrenceRuleId = if (obj.isNull("recurrenceRuleId")) null else obj.getLong("recurrenceRuleId"),
                    status = TaskStatus.fromString(obj.optString("status")),
                    actualCompletionTime = if (obj.isNull("actualCompletionTime")) null else obj.getLong("actualCompletionTime"),
                    notes = obj.optString("notes", ""),
                    notificationTime1 = if (obj.isNull("notificationTime1")) null else obj.getString("notificationTime1"),
                    notificationTime2 = if (obj.isNull("notificationTime2")) null else obj.getString("notificationTime2"),
                    notificationTime3 = if (obj.isNull("notificationTime3")) null else obj.getString("notificationTime3"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val habits = mutableListOf<HabitEntity>()
        val habitsArray = root.optJSONArray("habits") ?: JSONArray()
        for (i in 0 until habitsArray.length()) {
            val obj = habitsArray.getJSONObject(i)
            habits.add(
                HabitEntity(
                    id = obj.optLong("id", 0),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    categoryId = obj.optLong("categoryId", 4),
                    frequency = try { HabitFrequency.valueOf(obj.optString("frequency", "DAILY")) } catch (e: Exception) { HabitFrequency.DAILY },
                    daysOfWeekMask = obj.optInt("daysOfWeekMask", 127),
                    targetValue = obj.optDouble("targetValue", 1.0).toFloat(),
                    unit = obj.optString("unit", "times"),
                    colorHex = obj.optString("colorHex", "#38BDF8"),
                    iconName = obj.optString("iconName", "CheckCircle"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    isActive = obj.optBoolean("isActive", true)
                )
            )
        }

        val completions = mutableListOf<HabitCompletionEntity>()
        val completionsArray = root.optJSONArray("habitCompletions") ?: JSONArray()
        for (i in 0 until completionsArray.length()) {
            val obj = completionsArray.getJSONObject(i)
            completions.add(
                HabitCompletionEntity(
                    id = obj.optLong("id", 0),
                    habitId = obj.getLong("habitId"),
                    date = obj.getString("date"),
                    isCompleted = obj.optBoolean("isCompleted", true),
                    actualValue = obj.optDouble("actualValue", 1.0).toFloat(),
                    notes = obj.optString("notes", ""),
                    completedAt = obj.optLong("completedAt", System.currentTimeMillis())
                )
            )
        }

        val timetable = mutableListOf<TimetableEntryEntity>()
        val timetableArray = root.optJSONArray("timetableEntries") ?: JSONArray()
        for (i in 0 until timetableArray.length()) {
            val obj = timetableArray.getJSONObject(i)
            timetable.add(
                TimetableEntryEntity(
                    id = obj.optLong("id", 0),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    startTime = obj.getString("startTime"),
                    durationMinutes = obj.optInt("durationMinutes", 30),
                    categoryId = obj.optLong("categoryId", 1),
                    daysOfWeekMask = obj.optInt("daysOfWeekMask", 127),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 5),
                    sortOrder = obj.optInt("sortOrder", i)
                )
            )
        }

        return FullBackupData(
            exportDate = exportDate,
            tasks = tasks,
            recurrenceRules = emptyList(),
            habits = habits,
            habitCompletions = completions,
            timetableEntries = timetable,
            categories = emptyList()
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
