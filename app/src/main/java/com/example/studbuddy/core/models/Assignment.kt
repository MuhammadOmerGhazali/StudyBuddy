package com.example.studbuddy.core.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assignments",
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
data class Assignment(
    @PrimaryKey
    val id: String,
    val name: String,
    val courseId: String,
    val dueDate: Long,
    val totalMarks: Double,
    val obtainedMarks: Double?,
    val weightage: Double,
    val isCompleted: Boolean,
    val lastModified: Long = System.currentTimeMillis()
)
