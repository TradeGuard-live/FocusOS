package com.naman.focusos.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_os_prefs")

data class UserSettings(
    val themeMode: String = "SYSTEM",          // "DARK", "LIGHT", "SYSTEM" (Default: SYSTEM)
    val timeFormat: String = "12H",            // "12H", "24H"
    val firstDayOfWeek: String = "MONDAY",     // "MONDAY", "SUNDAY"
    val defaultDurationMinutes: Int = 30,
    val defaultReminderMinutes: Int = 10,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val timezone: String = "Asia/Kolkata"
)

class UserPreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val DEFAULT_DURATION = intPreferencesKey("default_duration")
        val DEFAULT_REMINDER = intPreferencesKey("default_reminder")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val TIMEZONE = stringPreferencesKey("timezone")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            timeFormat = prefs[Keys.TIME_FORMAT] ?: "12H",
            firstDayOfWeek = prefs[Keys.FIRST_DAY_OF_WEEK] ?: "MONDAY",
            defaultDurationMinutes = prefs[Keys.DEFAULT_DURATION] ?: 30,
            defaultReminderMinutes = prefs[Keys.DEFAULT_REMINDER] ?: 10,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            timezone = prefs[Keys.TIMEZONE] ?: "Asia/Kolkata"
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setTimeFormat(format: String) {
        context.dataStore.edit { it[Keys.TIME_FORMAT] = format }
    }

    suspend fun setFirstDayOfWeek(day: String) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = day }
    }

    suspend fun setDefaultDuration(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_DURATION] = minutes }
    }

    suspend fun setDefaultReminder(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_REMINDER] = minutes }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
