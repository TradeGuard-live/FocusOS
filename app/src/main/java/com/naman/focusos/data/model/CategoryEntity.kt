package com.naman.focusos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String = "Bookmark"
) {
    companion object {
        fun defaultCategories(): List<CategoryEntity> = listOf(
            CategoryEntity(id = 1, name = "Focus Work", colorHex = "#38BDF8", iconName = "Work"),
            CategoryEntity(id = 2, name = "Learning & Study", colorHex = "#8B5CF6", iconName = "School"),
            CategoryEntity(id = 3, name = "Health & Fitness", colorHex = "#10B981", iconName = "FitnessCenter"),
            CategoryEntity(id = 4, name = "Personal & Habit", colorHex = "#F59E0B", iconName = "SelfImprovement"),
            CategoryEntity(id = 5, name = "Project Dev", colorHex = "#06B6D4", iconName = "Code"),
            CategoryEntity(id = 6, name = "Routine", colorHex = "#EC4899", iconName = "Schedule")
        )
    }
}
