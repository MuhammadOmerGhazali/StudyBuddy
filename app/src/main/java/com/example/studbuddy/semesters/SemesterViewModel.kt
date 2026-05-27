package com.example.studbuddy.semesters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SemesterSortBy { CREATION, DATE }
enum class SortOrder { ASC, DESC }

data class SemesterUiState(
    val semesters: List<Semester> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortBy: SemesterSortBy = SemesterSortBy.CREATION,
    val sortOrder: SortOrder = SortOrder.DESC
)

@HiltViewModel
class SemesterViewModel @Inject constructor(
    private val repository: StudBuddyRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _sortBy = MutableStateFlow(SemesterSortBy.CREATION)
    private val _sortOrder = MutableStateFlow(SortOrder.DESC)

    val uiState: StateFlow<SemesterUiState> = combine(
        repository.getAllSemestersFlow(),
        _sortBy,
        _sortOrder,
        _isLoading,
        _error
    ) { semesters, sortBy, sortOrder, loading, error ->
        val sortedList = when (sortBy) {
            SemesterSortBy.CREATION -> {
                if (sortOrder == SortOrder.ASC) semesters.sortedBy { it.createdAt }
                else semesters.sortedByDescending { it.createdAt }
            }
            SemesterSortBy.DATE -> {
                if (sortOrder == SortOrder.ASC) semesters.sortedBy { it.startDate }
                else semesters.sortedByDescending { it.startDate }
            }
        }
        SemesterUiState(sortedList, loading, error, sortBy, sortOrder)
    }.onEach {
        _isLoading.value = false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SemesterUiState(isLoading = true)
    )

    fun setSortBy(sortBy: SemesterSortBy) {
        _sortBy.value = sortBy
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun addSemester(name: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val semester = Semester(
                name = name,
                startDate = startDate,
                endDate = endDate,
                isActive = repository.getActiveSemester() == null // Make active if it's the first one
            )
            repository.saveSemester(semester)
        }
    }

    fun updateSemester(semester: Semester) {
        viewModelScope.launch {
            repository.updateSemester(semester.copy(lastModified = System.currentTimeMillis()))
        }
    }

    fun deleteSemester(semester: Semester, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val courses = repository.getCoursesBySemester(semester.id)
            if (courses.isNotEmpty()) {
                onResult(false, "First delete its courses")
            } else {
                repository.deleteSemester(semester)
                onResult(true, null)
            }
        }
    }

    fun setActiveSemester(semesterId: String) {
        viewModelScope.launch {
            repository.setActiveSemester(semesterId)
        }
    }
}
