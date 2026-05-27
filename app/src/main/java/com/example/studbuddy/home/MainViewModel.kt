package com.example.studbuddy.home

import androidx.lifecycle.*
import com.example.studbuddy.core.models.*
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val semester: Semester? = null,
    val courses: List<Course> = emptyList(),
    val timetable: List<TimetableEntry> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val exams: List<Exam> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: StudBuddyRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getActiveSemesterFlow(),
        repository.getCoursesFlow(),
        repository.getTimetableFlow(),
        repository.getAttendanceFlow(),
        repository.getAssignmentsFlow(),
        repository.getExamsFlow()
    ) { array ->
        DashboardUiState(
            semester = array[0] as Semester?,
            courses = array[1] as List<Course>,
            timetable = array[2] as List<TimetableEntry>,
            attendance = array[3] as List<AttendanceRecord>,
            assignments = array[4] as List<Assignment>,
            exams = array[5] as List<Exam>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun saveSemester(semester: Semester) {
        viewModelScope.launch {
            repository.saveSemester(semester)
        }
    }
}
