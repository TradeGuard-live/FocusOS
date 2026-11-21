package com.naman.focusos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.naman.focusos.data.dao.CategoryDao
import com.naman.focusos.data.dao.FocusSessionDao
import com.naman.focusos.data.dao.HabitDao
import com.naman.focusos.data.dao.RecurrenceRuleDao
import com.naman.focusos.data.dao.TaskDao
import com.naman.focusos.data.dao.TimetableDao
import com.naman.focusos.data.model.AppClassificationEntity
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
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
        CategoryEntity::class,
        FocusSessionEntity::class,
        FocusPauseEntity::class,
        FocusAppUsageEntity::class,
        FocusSessionEventEntity::class,
        AppClassificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FocusDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun recurrenceRuleDao(): RecurrenceRuleDao
    abstract fun habitDao(): HabitDao
    abstract fun timetableDao(): TimetableDao
    abstract fun categoryDao(): CategoryDao
    abstract fun focusSessionDao(): FocusSessionDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. focus_sessions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `linkedTaskId` INTEGER,
                        `category` TEXT,
                        `targetDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                        `startTimestamp` INTEGER NOT NULL DEFAULT 0,
                        `endTimestamp` INTEGER,
                        `totalElapsedSeconds` INTEGER NOT NULL DEFAULT 0,
                        `focusedDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                        `pausedDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                        `distractedDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                        `productiveAppDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                        `sessionStatus` TEXT NOT NULL DEFAULT 'IDLE',
                        `monitoringEnabled` INTEGER NOT NULL DEFAULT 1,
                        `monitoringStatus` TEXT NOT NULL DEFAULT 'NOT_STARTED',
                        `distractionCount` INTEGER NOT NULL DEFAULT 0,
                        `appSwitchCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`linkedTaskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_linkedTaskId` ON `focus_sessions` (`linkedTaskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startTimestamp` ON `focus_sessions` (`startTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_sessionStatus` ON `focus_sessions` (`sessionStatus`)")

                // 2. focus_session_pauses
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_session_pauses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `pauseTimestamp` INTEGER NOT NULL,
                        `resumeTimestamp` INTEGER,
                        `pauseDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                        `reason` TEXT,
                        FOREIGN KEY(`sessionId`) REFERENCES `focus_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_pauses_sessionId` ON `focus_session_pauses` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_pauses_pauseTimestamp` ON `focus_session_pauses` (`pauseTimestamp`)")

                // 3. focus_app_usages
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_app_usages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `applicationName` TEXT NOT NULL,
                        `classification` TEXT NOT NULL,
                        `startTimestamp` INTEGER NOT NULL,
                        `endTimestamp` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL DEFAULT 0,
                        `sessionState` TEXT NOT NULL,
                        `monitoringConfidence` TEXT NOT NULL DEFAULT 'HIGH',
                        FOREIGN KEY(`sessionId`) REFERENCES `focus_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_app_usages_sessionId` ON `focus_app_usages` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_app_usages_packageName` ON `focus_app_usages` (`packageName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_app_usages_startTimestamp` ON `focus_app_usages` (`startTimestamp`)")

                // 4. focus_session_events
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_session_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `eventTimestamp` INTEGER NOT NULL,
                        `relatedAppPackage` TEXT,
                        `metadata` TEXT,
                        FOREIGN KEY(`sessionId`) REFERENCES `focus_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_events_sessionId` ON `focus_session_events` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_events_eventTimestamp` ON `focus_session_events` (`eventTimestamp`)")

                // 5. app_classifications
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_classifications` (
                        `packageName` TEXT PRIMARY KEY NOT NULL,
                        `applicationName` TEXT NOT NULL,
                        `classification` TEXT NOT NULL,
                        `isExcluded` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focus_os_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
