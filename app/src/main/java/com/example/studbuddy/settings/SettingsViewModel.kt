package com.example.studbuddy.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studbuddy.core.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val themeMode: StateFlow<Int> = settingsManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1) // Default to system

    val classReminderMode: StateFlow<String> = settingsManager.classReminderMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsManager.MODE_EACH_LECTURE)

    val lectureLeadTime: StateFlow<Int> = settingsManager.lectureLeadTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val dailySummaryTime: StateFlow<String> = settingsManager.dailySummaryTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "08:00")

    val assignmentRemindersEnabled: StateFlow<Boolean> = settingsManager.assignmentRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val assignmentLeadTime: StateFlow<Int> = settingsManager.assignmentLeadTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)

    val examRemindersEnabled: StateFlow<Boolean> = settingsManager.examRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val examLeadTime: StateFlow<Int> = settingsManager.examLeadTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { settingsManager.setThemeMode(mode) }
    }

    fun setClassReminderMode(mode: String) {
        viewModelScope.launch { settingsManager.setClassReminderMode(mode) }
    }

    fun setLectureLeadTime(minutes: Int) {
        viewModelScope.launch { settingsManager.setLectureLeadTime(minutes) }
    }

    fun setDailySummaryTime(time: String) {
        viewModelScope.launch { settingsManager.setDailySummaryTime(time) }
    }

    fun setAssignmentRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setAssignmentRemindersEnabled(enabled) }
    }

    fun setAssignmentLeadTime(hours: Int) {
        viewModelScope.launch { settingsManager.setAssignmentLeadTime(hours) }
    }

    fun setExamRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setExamRemindersEnabled(enabled) }
    }

    fun setExamLeadTime(hours: Int) {
        viewModelScope.launch { settingsManager.setExamLeadTime(hours) }
    }
}
