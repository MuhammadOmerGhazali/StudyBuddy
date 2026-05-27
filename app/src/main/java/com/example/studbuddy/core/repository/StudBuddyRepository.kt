package com.example.studbuddy.core.repository

import com.example.studbuddy.core.db.StudBuddyDatabase
import com.example.studbuddy.core.models.*
import kotlinx.coroutines.flow.Flow

class StudBuddyRepository(private val db: StudBuddyDatabase) {

    // --- Semester ---
    fun getAllSemestersFlow(): Flow<List<Semester>> = db.semesterDao().getAllFlow()
    fun getActiveSemesterFlow(): Flow<Semester?> = db.semesterDao().getActiveSemesterFlow()
    suspend fun getActiveSemester(): Semester? = db.semesterDao().getActiveSemester()
    suspend fun saveSemester(semester: Semester) = db.semesterDao().insert(semester)
    suspend fun updateSemester(semester: Semester) = db.semesterDao().update(semester)
    suspend fun deleteSemester(semester: Semester) = db.semesterDao().delete(semester)
    suspend fun setActiveSemester(semesterId: String) = db.semesterDao().setActiveSemester(semesterId)

    suspend fun clearAll() {
        db.semesterDao().deleteAll()
        db.courseDao().deleteAll()
        db.timetableDao().deleteAll()
        db.attendanceDao().deleteAll()
        db.assignmentDao().deleteAll()
        db.examDao().deleteAll()
    }

    // --- Courses ---
    fun getCoursesFlow(): Flow<List<Course>> = db.courseDao().getAllFlow()
    fun getCoursesBySemesterFlow(semesterId: String): Flow<List<Course>> = db.courseDao().getCoursesBySemesterFlow(semesterId)
    suspend fun getCourses(): List<Course> = db.courseDao().getAll()
    suspend fun getCoursesBySemester(semesterId: String): List<Course> = db.courseDao().getCoursesBySemester(semesterId)
    suspend fun addCourse(course: Course) = db.courseDao().insert(course)
    suspend fun updateCourse(course: Course) = db.courseDao().update(course)
    suspend fun deleteCourse(course: Course) = db.courseDao().delete(course)

    suspend fun addCourseWithGpaUpdate(course: Course) {
        addCourse(course)
        recalculateSemesterGpa(course.semesterId)
    }

    suspend fun updateCourseWithGpaUpdate(course: Course) {
        updateCourse(course)
        recalculateSemesterGpa(course.semesterId)
    }

    suspend fun deleteCourseWithGpaUpdate(course: Course) {
        deleteCourse(course)
        recalculateSemesterGpa(course.semesterId)
    }

    private suspend fun recalculateSemesterGpa(semesterId: String) {
        val semester = db.semesterDao().getAll().find { it.id == semesterId } ?: return
        val courses = db.courseDao().getCoursesBySemester(semesterId)
        
        val coursesWithGrades = courses.filter { it.grade != null }
        val totalPoints = coursesWithGrades.sumOf { it.gradePoints }
        val totalCredits = coursesWithGrades.sumOf { it.creditHours }
        
        val calculatedGpa = if (totalCredits > 0) totalPoints / totalCredits else null
        
        if (calculatedGpa != semester.gpa) {
            db.semesterDao().update(semester.copy(gpa = calculatedGpa))
        }
    }

    // --- Timetable ---
    fun getTimetableFlow(): Flow<List<TimetableEntry>> = db.timetableDao().getAllFlow()
    suspend fun getTimetable(): List<TimetableEntry> = db.timetableDao().getAll()
    suspend fun addTimetableEntry(entry: TimetableEntry) = db.timetableDao().insert(entry)
    suspend fun updateTimetableEntry(entry: TimetableEntry) = db.timetableDao().update(entry)
    suspend fun deleteTimetableEntry(entryId: String) = db.timetableDao().deleteById(entryId)

    // --- Attendance ---
    fun getAttendanceFlow(): Flow<List<AttendanceRecord>> = db.attendanceDao().getAllFlow()
    suspend fun getAttendance(): List<AttendanceRecord> = db.attendanceDao().getAll()
    suspend fun addAttendanceRecord(record: AttendanceRecord) = db.attendanceDao().insert(record)
    suspend fun updateAttendanceRecord(record: AttendanceRecord) = db.attendanceDao().update(record)
    suspend fun deleteAttendanceRecord(recordId: String) = db.attendanceDao().deleteById(recordId)

    // --- Assignments ---
    fun getAssignmentsFlow(): Flow<List<Assignment>> = db.assignmentDao().getAllFlow()
    suspend fun getAssignments(): List<Assignment> = db.assignmentDao().getAll()
    suspend fun updateAssignment(assignment: Assignment) = db.assignmentDao().insert(assignment)
    suspend fun deleteAssignment(assignmentId: String) = db.assignmentDao().deleteById(assignmentId)

    // --- Exams ---
    fun getExamsFlow(): Flow<List<Exam>> = db.examDao().getAllFlow()
    suspend fun getExams(): List<Exam> = db.examDao().getAll()
    suspend fun updateExam(exam: Exam) = db.examDao().insert(exam)
    suspend fun deleteExam(examId: String) = db.examDao().deleteById(examId)

    // --- Notes ---
    fun getNotesByCourseFlow(courseId: String): Flow<List<Note>> = db.noteDao().getNotesByCourseFlow(courseId)
    suspend fun addNote(note: Note) = db.noteDao().insert(note)
    suspend fun deleteNote(note: Note) = db.noteDao().delete(note)
}
