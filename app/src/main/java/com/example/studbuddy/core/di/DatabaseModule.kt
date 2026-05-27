package com.example.studbuddy.core.di

import android.content.Context
import com.example.studbuddy.core.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StudBuddyDatabase {
        return StudBuddyDatabase.getDatabase(context)
    }

    @Provides
    fun provideSemesterDao(database: StudBuddyDatabase): SemesterDao = database.semesterDao()

    @Provides
    fun provideCourseDao(database: StudBuddyDatabase): CourseDao = database.courseDao()

    @Provides
    fun provideTimetableDao(database: StudBuddyDatabase): TimetableDao = database.timetableDao()

    @Provides
    fun provideAttendanceDao(database: StudBuddyDatabase): AttendanceDao = database.attendanceDao()

    @Provides
    fun provideAssignmentDao(database: StudBuddyDatabase): AssignmentDao = database.assignmentDao()

    @Provides
    fun provideExamDao(database: StudBuddyDatabase): ExamDao = database.examDao()
}
