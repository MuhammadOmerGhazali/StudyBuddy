package com.example.studbuddy.core.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class TimetableEntry(
    @PrimaryKey
    val id: String,
    val courseId: String,       // Reference to Course
    val dayOfWeek: Int,         // 1=Monday ... 7=Sunday
    val startTime: String,      // "HH:mm"
    val endTime: String,        // "HH:mm"
    val room: String,
    val color: String,
    val lastModified: Long = System.currentTimeMillis()
)
