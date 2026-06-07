package com.example.studbuddy.courses.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studbuddy.core.models.*
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseSummaryUiState(
    val course: Course? = null,
    val semester: Semester? = null,
    val attendance: List<AttendanceRecord> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CourseSummaryViewModel @Inject constructor(
    private val repository: StudBuddyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseId: String? = savedStateHandle["courseId"]

    val allSemesters: StateFlow<List<Semester>> = repository.getAllSemestersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CourseSummaryUiState> = if (courseId != null) {
        combine(
            repository.getCoursesFlow().map { list -> list.find { it.id == courseId } },
            repository.getAllSemestersFlow(),
            repository.getAttendanceFlow().map { list -> list.filter { it.courseId == courseId } },
            repository.getAssignmentsFlow().map { list -> list.filter { it.courseId == courseId } },
            repository.getExamsFlow().map { list -> list.filter { it.courseId == courseId } }
        ) { course, semesters, attendance, assignments, exams ->
            val semester = semesters.find { it.id == course?.semesterId }
            CourseSummaryUiState(course, semester, attendance, assignments, exams, false)
        }
    } else {
        flowOf(CourseSummaryUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CourseSummaryUiState()
    )

    fun deleteCourse(onComplete: () -> Unit) {
        viewModelScope.launch {
            val course = uiState.value.course ?: return@launch
            repository.deleteCourseWithGpaUpdate(course)
            onComplete()
        }
    }
    
    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.updateCourseWithGpaUpdate(course)
        }
    }
}
