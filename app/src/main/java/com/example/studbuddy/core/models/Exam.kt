package com.example.studbuddy.core.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExamType { QUIZ, MIDTERM, FINAL }

@Entity(
    tableName = "exams",
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
data class Exam(
    @PrimaryKey
    val id: String,
    val courseId: String,
    val type: ExamType,
    val date: Long,
    val venue: String?,
    val totalMarks: Double,
    val obtainedMarks: Double?,
    val weightage: Double,
    val isCompleted: Boolean,
    val lastModified: Long = System.currentTimeMillis()
)
