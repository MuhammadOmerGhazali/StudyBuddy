package com.example.studbuddy.courses.details

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CourseDetailViewModel @Inject constructor() : ViewModel() {
    private val _courseId = MutableStateFlow<String?>(null)
    val courseId: StateFlow<String?> = _courseId

    fun setCourseId(id: String?) {
        _courseId.value = id
    }
}
