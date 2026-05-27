package com.example.studbuddy.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.studbuddy.core.models.*

@Database(
    entities = [
        Semester::class,
        Course::class,
        TimetableEntry::class,
        AttendanceRecord::class,
        Assignment::class,
        Exam::class,
        Note::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudBuddyDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun examDao(): ExamDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: StudBuddyDatabase? = null

        fun getDatabase(context: Context): StudBuddyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudBuddyDatabase::class.java,
                    "studbuddy_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
