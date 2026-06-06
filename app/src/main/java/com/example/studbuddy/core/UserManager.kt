package com.example.studbuddy.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.studbuddy.core.models.AuthStatus
import com.example.studbuddy.core.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserManager(private val context: Context) {

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
        private val KEY_AUTH_STATUS = stringPreferencesKey("auth_status")
    }

    val userFlow: Flow<User> = context.userDataStore.data.map { preferences ->
        User(
            id = preferences[KEY_USER_ID] ?: "guest",
            displayName = preferences[KEY_DISPLAY_NAME] ?: "Guest User",
            email = preferences[KEY_EMAIL],
            profileImageUri = preferences[KEY_PROFILE_IMAGE_URI],
            authStatus = AuthStatus.valueOf(preferences[KEY_AUTH_STATUS] ?: AuthStatus.GUEST.name)
        )
    }

    suspend fun updateDisplayName(name: String) {
        context.userDataStore.edit { it[KEY_DISPLAY_NAME] = name }
    }

    suspend fun updateProfileImage(uri: String?) {
        context.userDataStore.edit { 
            if (uri == null) it.remove(KEY_PROFILE_IMAGE_URI)
            else it[KEY_PROFILE_IMAGE_URI] = uri 
        }
    }

    suspend fun signOut() {
        context.userDataStore.edit { it.clear() }
    }

    suspend fun setUser(user: User) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_DISPLAY_NAME] = user.displayName
            user.email?.let { prefs[KEY_EMAIL] = it } ?: prefs.remove(KEY_EMAIL)
            user.profileImageUri?.let { prefs[KEY_PROFILE_IMAGE_URI] = it } ?: prefs.remove(KEY_PROFILE_IMAGE_URI)
            prefs[KEY_AUTH_STATUS] = user.authStatus.name
        }
    }
}
