package com.example.studbuddy.timetable

import androidx.lifecycle.*
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.core.models.TimetableEntry
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimetableUiState(
    val timetable: List<TimetableEntry> = emptyList(),
    val courses: List<Course> = emptyList(),
    val activeSemester: Semester? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class TimetableViewModel @Inject constructor(private val repository: StudBuddyRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<TimetableUiState> = combine(
        repository.getTimetableFlow(),
        repository.getCoursesFlow(),
        repository.getActiveSemesterFlow(),
        _isLoading
    ) { timetable, courses, activeSemester, loading ->
        val filteredCourses = if (activeSemester != null) {
            courses.filter { it.semesterId == activeSemester.id }
        } else {
            courses
        }
        val courseIds = filteredCourses.map { it.id }.toSet()
        val filteredTimetable = timetable.filter { courseIds.contains(it.courseId) }

        TimetableUiState(filteredTimetable, filteredCourses, activeSemester, loading)
    }.onEach {
        if (it.timetable.isNotEmpty() || it.courses.isNotEmpty() || it.activeSemester != null) {
            _isLoading.value = false
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState()
    )

    fun addTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.addTimetableEntry(entry)
        }
    }

    fun updateTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.updateTimetableEntry(entry)
        }
    }

    fun deleteTimetableEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteTimetableEntry(entryId)
        }
    }
}
