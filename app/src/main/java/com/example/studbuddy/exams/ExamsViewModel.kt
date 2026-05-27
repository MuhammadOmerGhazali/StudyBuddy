package com.example.studbuddy.exams

import androidx.lifecycle.*
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Exam
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamsUiState(
    val exams: List<Exam> = emptyList(),
    val courses: List<Course> = emptyList(),
    val activeSemester: Semester? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ExamsViewModel @Inject constructor(private val repository: StudBuddyRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<ExamsUiState> = combine(
        repository.getExamsFlow(),
        repository.getCoursesFlow(),
        repository.getActiveSemesterFlow(),
        _isLoading
    ) { exams, courses, activeSemester, loading ->
        val filteredCourses = if (activeSemester != null) {
            courses.filter { it.semesterId == activeSemester.id }
        } else {
            courses
        }
        ExamsUiState(exams, filteredCourses, activeSemester, loading)
    }.onEach {
        if (it.exams.isNotEmpty() || it.courses.isNotEmpty() || it.activeSemester != null) {
            _isLoading.value = false
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExamsUiState()
    )

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            repository.updateExam(exam)
            updateCourseMarks(exam.courseId)
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            repository.deleteExam(exam.id)
            updateCourseMarks(exam.courseId)
        }
    }

    private suspend fun updateCourseMarks(courseId: String) {
        val courses = repository.getCourses()
        val course = courses.find { it.id == courseId } ?: return
        
        val allAssignments = repository.getAssignments()
        val allExams = repository.getExams()
        
        val assignmentScore = allAssignments.filter { it.courseId == courseId && it.isCompleted }.sumOf { 
            if (it.totalMarks > 0) (it.obtainedMarks ?: 0.0) / it.totalMarks * it.weightage else 0.0 
        }
        val examScore = allExams.filter { it.courseId == courseId && it.isCompleted }.sumOf {
            if (it.totalMarks > 0) (it.obtainedMarks ?: 0.0) / it.totalMarks * it.weightage else 0.0
        }
        
        repository.updateCourse(course.copy(marks = assignmentScore + examScore))
    }
}
