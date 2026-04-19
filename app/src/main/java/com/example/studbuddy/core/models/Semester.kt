package com.example.studbuddy.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean = true,
    val gpa: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
