package com.example.studbuddy.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_NAME = "studbuddy_settings"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFS_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, PREFS_NAME))
    }
)

class SettingsManager(private val context: Context) {

    companion object {
        const val MODE_EACH_LECTURE = "EACH_LECTURE"
        const val MODE_DAILY_SUMMARY = "DAILY_SUMMARY"

        private val KEY_CLASS_REMINDER_MODE = stringPreferencesKey("class_reminder_mode")
        private val KEY_LECTURE_LEAD_TIME = intPreferencesKey("lecture_lead_time")
        private val KEY_DAILY_SUMMARY_TIME = stringPreferencesKey("daily_summary_time")
        private val KEY_ASSIGNMENT_REMINDERS = booleanPreferencesKey("assignment_reminders")
        private val KEY_ASSIGNMENT_LEAD_TIME = intPreferencesKey("assignment_lead_time")
        private val KEY_EXAM_REMINDERS = booleanPreferencesKey("exam_reminders")
        private val KEY_EXAM_LEAD_TIME = intPreferencesKey("exam_lead_time")
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    val classReminderMode: Flow<String> = context.dataStore.data.map { it[KEY_CLASS_REMINDER_MODE] ?: MODE_EACH_LECTURE }
    suspend fun setClassReminderMode(value: String) {
        context.dataStore.edit { it[KEY_CLASS_REMINDER_MODE] = value }
    }

    val lectureLeadTime: Flow<Int> = context.dataStore.data.map { it[KEY_LECTURE_LEAD_TIME] ?: 15 }
    suspend fun setLectureLeadTime(value: Int) {
        context.dataStore.edit { it[KEY_LECTURE_LEAD_TIME] = value }
    }

    val dailySummaryTime: Flow<String> = context.dataStore.data.map { it[KEY_DAILY_SUMMARY_TIME] ?: "08:00" }
    suspend fun setDailySummaryTime(value: String) {
        context.dataStore.edit { it[KEY_DAILY_SUMMARY_TIME] = value }
    }

    val assignmentRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_ASSIGNMENT_REMINDERS] ?: true }
    suspend fun setAssignmentRemindersEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_ASSIGNMENT_REMINDERS] = value }
    }

    val assignmentLeadTime: Flow<Int> = context.dataStore.data.map { it[KEY_ASSIGNMENT_LEAD_TIME] ?: 24 }
    suspend fun setAssignmentLeadTime(value: Int) {
        context.dataStore.edit { it[KEY_ASSIGNMENT_LEAD_TIME] = value }
    }

    val examRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_EXAM_REMINDERS] ?: true }
    suspend fun setExamRemindersEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_EXAM_REMINDERS] = value }
    }

    val examLeadTime: Flow<Int> = context.dataStore.data.map { it[KEY_EXAM_LEAD_TIME] ?: 24 }
    suspend fun setExamLeadTime(value: Int) {
        context.dataStore.edit { it[KEY_EXAM_LEAD_TIME] = value }
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
    suspend fun setThemeMode(value: Int) {
        context.dataStore.edit { it[KEY_THEME_MODE] = value }
    }

    val lastSyncTime: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SYNC_TIME] ?: 0L }
    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC_TIME] = time }
    }
}
