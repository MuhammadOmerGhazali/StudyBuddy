package com.example.studbuddy.core.models

enum class AuthStatus {
    GUEST,
    SIGNED_IN
}

data class User(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val profileImageUri: String? = null,
    val authStatus: AuthStatus = AuthStatus.GUEST
)
