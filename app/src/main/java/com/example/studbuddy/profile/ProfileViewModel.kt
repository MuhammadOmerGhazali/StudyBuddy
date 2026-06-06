package com.example.studbuddy.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studbuddy.core.UserManager
import com.example.studbuddy.core.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userManager: UserManager
) : ViewModel() {

    val userState: StateFlow<User?> = userManager.userFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            userManager.updateDisplayName(name)
        }
    }

    fun updateProfileImage(uri: String?) {
        viewModelScope.launch {
            userManager.updateProfileImage(uri)
        }
    }

    fun setUser(user: User) {
        viewModelScope.launch {
            userManager.setUser(user)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userManager.signOut()
        }
    }
}
