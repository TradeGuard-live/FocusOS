package com.naman.focusos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.naman.focusos.data.dao.CategoryDao
import com.naman.focusos.data.dao.HabitDao
import com.naman.focusos.data.dao.RecurrenceRuleDao
import com.naman.focusos.data.dao.TaskDao
import com.naman.focusos.data.dao.TimetableDao
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TimetableEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        RecurrenceRuleEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        TimetableEntryEntity::class,
        CategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FocusDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun recurrenceRuleDao(): RecurrenceRuleDao
    abstract fun habitDao(): HabitDao
    abstract fun timetableDao(): TimetableDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN notificationTime1 TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN notificationTime2 TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN notificationTime3 TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focus_os_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default categories and timetable in background
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.categoryDao().insertCategories(CategoryEntity.defaultCategories())
                                database.timetableDao().insertEntries(TimetableEntryEntity.defaultTimetable())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
