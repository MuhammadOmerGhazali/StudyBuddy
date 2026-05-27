package com.example.studbuddy.assignments

import androidx.lifecycle.*
import com.example.studbuddy.core.models.Assignment
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignmentsUiState(
    val assignments: List<Assignment> = emptyList(),
    val courses: List<Course> = emptyList(),
    val activeSemester: Semester? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AssignmentsViewModel @Inject constructor(private val repository: StudBuddyRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<AssignmentsUiState> = combine(
        repository.getAssignmentsFlow(),
        repository.getCoursesFlow(),
        repository.getActiveSemesterFlow(),
        _isLoading
    ) { assignments, courses, activeSemester, loading ->
        val filteredCourses = if (activeSemester != null) {
            courses.filter { it.semesterId == activeSemester.id }
        } else {
            courses
        }
        AssignmentsUiState(assignments, filteredCourses, activeSemester, loading)
    }.onEach {
        if (it.assignments.isNotEmpty() || it.courses.isNotEmpty() || it.activeSemester != null) {
            _isLoading.value = false
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AssignmentsUiState()
    )

    fun updateAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.updateAssignment(assignment)
            updateCourseMarks(assignment.courseId)
        }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.deleteAssignment(assignment.id)
            updateCourseMarks(assignment.courseId)
        }
    }

    private suspend fun updateCourseMarks(courseId: String) {
        val courses = repository.getCourses()
        val course = courses.find { it.id == courseId } ?: return
        
        val allAssignments = repository.getAssignments()
        val courseAssignments = allAssignments.filter { it.courseId == courseId && it.isCompleted }
        
        val totalMarksFromAssignments = courseAssignments.sumOf { 
            if (it.totalMarks > 0) (it.obtainedMarks ?: 0.0) / it.totalMarks * it.weightage else 0.0 
        }
        
        // This only updates assignment marks. Exams and Attendance also contribute.
        // For a full implementation, we'd need to sum all components.
        // For now, we'll keep the logic as it was (partially updating).
        
        repository.updateCourse(course.copy(marks = totalMarksFromAssignments))
    }
}
