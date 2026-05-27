package com.example.studbuddy.core.db

import androidx.room.TypeConverter
import com.example.studbuddy.core.models.ExamType

class Converters {
    @TypeConverter
    fun fromExamType(value: ExamType): String {
        return value.name
    }

    @TypeConverter
    fun toExamType(value: String): ExamType {
        return ExamType.valueOf(value)
    }
}
