package com.example.studbuddy.core.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("semesterId")]
)
data class Course(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val instructor: String?,
    val creditHours: Int,
    val semesterId: String,
    val marks: Double,
    val grade: String?,
    val gradePoints: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
