package com.example.studbuddy.courses

import androidx.lifecycle.*
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.semesters.SortOrder
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CourseSortBy { NAME, CREATION, CREDITS }

data class CourseUiState(
    val courses: List<Course> = emptyList(),
    val semester: Semester? = null,
    val isLoading: Boolean = false,
    val sortBy: CourseSortBy = CourseSortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASC
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val repository: StudBuddyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val semesterId: String? = savedStateHandle["semesterId"]
    private val _isLoading = MutableStateFlow(true)
    private val _sortBy = MutableStateFlow(CourseSortBy.NAME)
    private val _sortOrder = MutableStateFlow(SortOrder.ASC)

    private val selectedSemesterFlow = if (semesterId != null) {
        repository.getAllSemestersFlow().map { list -> list.find { it.id == semesterId } }
    } else {
        repository.getActiveSemesterFlow()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CourseUiState> = combine(
        selectedSemesterFlow.flatMapLatest { sem ->
            if (sem != null) repository.getCoursesBySemesterFlow(sem.id)
            else flowOf(emptyList())
        },
        selectedSemesterFlow,
        _sortBy,
        _sortOrder,
        _isLoading
    ) { courses, semester, sortBy, sortOrder, loading ->
        val sortedList = when (sortBy) {
            CourseSortBy.NAME -> {
                if (sortOrder == SortOrder.ASC) courses.sortedBy { it.name }
                else courses.sortedByDescending { it.name }
            }
            CourseSortBy.CREATION -> {
                if (sortOrder == SortOrder.ASC) courses.sortedBy { it.createdAt }
                else courses.sortedByDescending { it.createdAt }
            }
            CourseSortBy.CREDITS -> {
                // Same credit hours will be sorted by creation time
                if (sortOrder == SortOrder.ASC) courses.sortedWith(compareBy({ it.creditHours }, { it.createdAt }))
                else courses.sortedWith(compareByDescending<Course> { it.creditHours }.thenByDescending { it.createdAt })
            }
        }
        CourseUiState(sortedList, semester, loading, sortBy, sortOrder)
    }.onEach {
        _isLoading.value = false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CourseUiState(isLoading = true)
    )

    fun setSortBy(sortBy: CourseSortBy) {
        _sortBy.value = sortBy
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun addCourse(course: Course) {
        viewModelScope.launch {
            repository.addCourseWithGpaUpdate(course)
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.updateCourseWithGpaUpdate(course)
        }
    }
}
