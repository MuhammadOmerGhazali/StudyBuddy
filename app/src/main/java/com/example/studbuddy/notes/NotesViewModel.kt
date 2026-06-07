package com.example.studbuddy.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studbuddy.core.models.Note
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: StudBuddyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val courseId: String? = savedStateHandle["courseId"]
    private val manualCourseId = MutableStateFlow<String?>(null)

    fun setManualCourseId(id: String) {
        manualCourseId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotesUiState> = combine(
        manualCourseId,
        flowOf(courseId)
    ) { manual, saved ->
        manual ?: saved
    }.flatMapLatest { id ->
        if (id != null) {
            repository.getNotesByCourseFlow(id).map { NotesUiState(it, false) }
        } else {
            flowOf(NotesUiState(emptyList(), false))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun addNote(title: String, localPath: String, fileType: String, fileSize: Long, explicitCourseId: String? = null) {
        val id = explicitCourseId ?: courseId ?: return
        viewModelScope.launch {
            val note = Note(
                courseId = id,
                title = title,
                localPath = localPath,
                fileType = fileType,
                fileSize = fileSize
            )
            repository.addNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}
