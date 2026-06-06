package com.example.studbuddy.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studbuddy.core.SettingsManager
import com.example.studbuddy.core.repository.SyncRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR, NOT_SIGNED_IN }

@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val lastSyncTime: StateFlow<Long> = settingsManager.lastSyncTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val isSignedIn: Boolean
        get() = FirebaseAuth.getInstance().currentUser != null

    fun triggerSync() {
        if (!isSignedIn) {
            _syncStatus.value = SyncStatus.NOT_SIGNED_IN
            return
        }

        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                syncRepository.syncAll()
                _syncStatus.value = SyncStatus.SUCCESS
                Log.d("CloudSyncViewModel", "Manual sync succeeded")
            } catch (e: Exception) {
                Log.e("CloudSyncViewModel", "Manual sync failed", e)
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }
}
