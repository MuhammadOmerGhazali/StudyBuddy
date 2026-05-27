package com.example.studbuddy.attendance

import androidx.lifecycle.*
import com.example.studbuddy.core.models.AttendanceRecord
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttendanceUiState(
    val courses: List<Course> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val activeSemester: Semester? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AttendanceViewModel @Inject constructor(private val repository: StudBuddyRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<AttendanceUiState> = combine(
        repository.getCoursesFlow(),
        repository.getAttendanceFlow(),
        repository.getActiveSemesterFlow(),
        _isLoading
    ) { courses, attendance, activeSemester, loading ->
        val filteredCourses = if (activeSemester != null) {
            courses.filter { it.semesterId == activeSemester.id }
        } else {
            courses
        }
        AttendanceUiState(filteredCourses, attendance, activeSemester, loading)
    }.onEach {
        if (it.courses.isNotEmpty() || it.attendance.isNotEmpty() || it.activeSemester != null) {
            _isLoading.value = false
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AttendanceUiState()
    )

    fun addAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            repository.addAttendanceRecord(record)
        }
    }

    fun updateAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            repository.updateAttendanceRecord(record)
        }
    }

    fun deleteAttendanceRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteAttendanceRecord(recordId)
        }
    }
}
