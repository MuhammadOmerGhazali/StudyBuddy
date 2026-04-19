package com.example.studbuddy.core.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
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
data class AttendanceRecord(
    @PrimaryKey
    val id: String,
    val courseId: String,
    val dateTime: Long,
    val status: String, // PRESENT, ABSENT, LATE
    val lastModified: Long = System.currentTimeMillis()
)
