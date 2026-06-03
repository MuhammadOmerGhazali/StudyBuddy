package com.example.studbuddy.core.notifications.di

import com.example.studbuddy.core.SettingsManager
import com.example.studbuddy.core.repository.StudBuddyRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun studBuddyRepository(): StudBuddyRepository
    fun settingsManager(): SettingsManager
}
