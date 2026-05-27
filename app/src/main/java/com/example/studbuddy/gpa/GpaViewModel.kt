package com.example.studbuddy.gpa

import androidx.lifecycle.*
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GpaUiState(
    val activeCourses: List<Course> = emptyList(),
    val previousCourses: List<Course> = emptyList(),
    val semester: Semester? = null,
    val semesterGpa: Double = 0.0,
    val cgpa: Double = 0.0
)

@HiltViewModel
class GpaViewModel @Inject constructor(private val repository: StudBuddyRepository) : ViewModel() {

    val uiState: StateFlow<GpaUiState> = combine(
        repository.getCoursesFlow(),
        repository.getActiveSemesterFlow()
    ) { allCourses, activeSemester ->
        // 1. Calculate Semester GPA (only for active semester)
        val activeSemesterCourses = allCourses.filter { it.semesterId == activeSemester?.id }
        val activeCoursesWithGrades = activeSemesterCourses.filter { it.grade != null }
        val semPoints = activeCoursesWithGrades.sumOf { it.gradePoints }
        val semCredits = activeCoursesWithGrades.sumOf { it.creditHours }
        val semesterGpa = if (semCredits > 0) semPoints / semCredits else 0.0

        // 2. Calculate CGPA (all courses across all semesters)
        val coursesWithGrades = allCourses.filter { it.grade != null }
        val totalPoints = coursesWithGrades.sumOf { it.gradePoints }
        val totalCredits = coursesWithGrades.sumOf { it.creditHours }
        val cgpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0

        val previousCourses = allCourses.filter { it.semesterId != activeSemester?.id }
        
        GpaUiState(activeSemesterCourses, previousCourses, activeSemester, semesterGpa, cgpa)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GpaUiState()
    )

    fun updateCourseGrade(course: Course, grade: String, gradePoints: Double) {
        viewModelScope.launch {
            val updatedGrade = if (grade == "Select Grade" || grade == "N/A") null else grade
            val updatedPoints = if (updatedGrade == null) 0.0 else gradePoints
            repository.updateCourseWithGpaUpdate(course.copy(grade = updatedGrade, gradePoints = updatedPoints))
        }
    }
}
