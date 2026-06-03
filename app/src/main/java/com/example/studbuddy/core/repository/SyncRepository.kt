package com.example.studbuddy.core.repository

import android.util.Log
import com.example.studbuddy.core.SettingsManager
import com.example.studbuddy.core.db.StudBuddyDatabase
import com.example.studbuddy.core.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG = "SyncRepository"

class SyncRepository(private val db: StudBuddyDatabase, private val settingsManager: SettingsManager) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Pushes all local data to Firestore and pulls any newer remote data back.
     * Throws if the user is not signed in (so the caller can show an error).
     * Individual table errors are logged but do not abort the entire sync.
     */
    suspend fun syncAll() {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("No Firebase user signed in")

        Log.d(TAG, "syncAll: Starting sync for user $userId")

        var anyError = false
        fun runTable(name: String, block: suspend () -> Unit) {
            // wrapper captured here but executed below — use inline suspend pattern
        }

        try { syncSemesters(userId) }   catch (e: Exception) { Log.e(TAG, "syncSemesters failed", e);   anyError = true }
        try { syncCourses(userId) }     catch (e: Exception) { Log.e(TAG, "syncCourses failed", e);     anyError = true }
        try { syncTimetable(userId) }   catch (e: Exception) { Log.e(TAG, "syncTimetable failed", e);   anyError = true }
        try { syncAttendance(userId) }  catch (e: Exception) { Log.e(TAG, "syncAttendance failed", e);  anyError = true }
        try { syncAssignments(userId) } catch (e: Exception) { Log.e(TAG, "syncAssignments failed", e); anyError = true }
        try { syncExams(userId) }       catch (e: Exception) { Log.e(TAG, "syncExams failed", e);       anyError = true }

        if (!anyError) {
            settingsManager.setLastSyncTime(System.currentTimeMillis())
        }

        if (anyError) {
            throw Exception("One or more tables failed to sync — check Logcat for details")
        }

        Log.d(TAG, "syncAll: Sync complete.")
    }

    // ─── Semesters ───────────────────────────────────────────────────────────

    private suspend fun syncSemesters(userId: String) {
        val collection = userCollection(userId, "semesters")
        val localItems = db.semesterDao().getAll()

        // Push all local items (no pre-read — just always set)
        for (item in localItems) {
            Log.d(TAG, "Pushing semester ${item.id}")
            collection.document(item.id).set(semesterToMap(item)).await()
        }

        // Pull remote items that are newer than local
        val localIds = localItems.associateBy { it.id }
        val remoteDocs = collection.get().await()
        for (doc in remoteDocs.documents) {
            val remoteLastModified = doc.getLong("lastModified") ?: 0L
            val localLastModified = localIds[doc.id]?.lastModified ?: 0L
            if (remoteLastModified > localLastModified) {
                mapToSemester(doc.id, doc.data ?: continue)?.let { db.semesterDao().insert(it) }
            }
        }
    }

    private fun semesterToMap(s: Semester): Map<String, Any?> = mapOf(
        "id" to s.id,
        "name" to s.name,
        "startDate" to s.startDate,
        "endDate" to s.endDate,
        "isActive" to s.isActive,
        "gpa" to s.gpa,
        "createdAt" to s.createdAt,
        "lastModified" to s.lastModified
    )

    private fun mapToSemester(id: String, m: Map<String, Any>): Semester? = runCatching {
        Semester(
            id = id,
            name = m["name"] as? String ?: return@runCatching null,
            startDate = (m["startDate"] as? Long) ?: (m["startDate"] as? Number)?.toLong() ?: return@runCatching null,
            endDate = (m["endDate"] as? Long) ?: (m["endDate"] as? Number)?.toLong() ?: return@runCatching null,
            isActive = m["isActive"] as? Boolean ?: false,
            gpa = (m["gpa"] as? Double) ?: (m["gpa"] as? Number)?.toDouble(),
            createdAt = (m["createdAt"] as? Long) ?: (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            lastModified = (m["lastModified"] as? Long) ?: (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }.getOrElse { Log.e(TAG, "mapToSemester failed for id=$id", it); null }

    // ─── Courses ─────────────────────────────────────────────────────────────

    private suspend fun syncCourses(userId: String) {
        val collection = userCollection(userId, "courses")
        val localItems = db.courseDao().getAll()

        for (item in localItems) {
            Log.d(TAG, "Pushing course ${item.id}")
            collection.document(item.id).set(courseToMap(item)).await()
        }

        val localIds = localItems.associateBy { it.id }
        val remoteDocs = collection.get().await()
        for (doc in remoteDocs.documents) {
            val remoteLastModified = doc.getLong("lastModified") ?: 0L
            val localLastModified = localIds[doc.id]?.lastModified ?: 0L
            if (remoteLastModified > localLastModified) {
                mapToCourse(doc.id, doc.data ?: continue)?.let { db.courseDao().insert(it) }
            }
        }
    }

    private fun courseToMap(c: Course): Map<String, Any?> = mapOf(
        "id" to c.id,
        "name" to c.name,
        "description" to c.description,
        "instructor" to c.instructor,
        "creditHours" to c.creditHours,
        "semesterId" to c.semesterId,
        "marks" to c.marks,
        "grade" to c.grade,
        "gradePoints" to c.gradePoints,
        "createdAt" to c.createdAt,
        "lastModified" to c.lastModified
    )

    private fun mapToCourse(id: String, m: Map<String, Any>): Course? = runCatching {
        Course(
            id = id,
            name = m["name"] as? String ?: return@runCatching null,
            description = m["description"] as? String,
            instructor = m["instructor"] as? String,
            creditHours = ((m["creditHours"] as? Long) ?: (m["creditHours"] as? Number)?.toLong())?.toInt() ?: return@runCatching null,
            semesterId = m["semesterId"] as? String ?: return@runCatching null,
            marks = (m["marks"] as? Double) ?: (m["marks"] as? Number)?.toDouble() ?: 0.0,
            grade = m["grade"] as? String,
            gradePoints = (m["gradePoints"] as? Double) ?: (m["gradePoints"] as? Number)?.toDouble() ?: 0.0,
            createdAt = (m["createdAt"] as? Long) ?: (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            lastModified = (m["lastModified"] as? Long) ?: (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }.getOrElse { Log.e(TAG, "mapToCourse failed for id=$id", it); null }

    // ─── Timetable ───────────────────────────────────────────────────────────

    private suspend fun syncTimetable(userId: String) {
        val collection = userCollection(userId, "timetable")
        val localItems = db.timetableDao().getAll()

        for (item in localItems) {
            Log.d(TAG, "Pushing timetable entry ${item.id}")
            collection.document(item.id).set(timetableEntryToMap(item)).await()
        }

        val localIds = localItems.associateBy { it.id }
        val remoteDocs = collection.get().await()
        for (doc in remoteDocs.documents) {
            val remoteLastModified = doc.getLong("lastModified") ?: 0L
            val localLastModified = localIds[doc.id]?.lastModified ?: 0L
            if (remoteLastModified > localLastModified) {
                mapToTimetableEntry(doc.id, doc.data ?: continue)?.let { db.timetableDao().insert(it) }
            }
        }
    }

    private fun timetableEntryToMap(t: TimetableEntry): Map<String, Any?> = mapOf(
        "id" to t.id,
        "courseId" to t.courseId,
        "dayOfWeek" to t.dayOfWeek,
        "startTime" to t.startTime,
        "endTime" to t.endTime,
        "room" to t.room,
        "color" to t.color,
        "lastModified" to t.lastModified
    )

    private fun mapToTimetableEntry(id: String, m: Map<String, Any>): TimetableEntry? = runCatching {
        TimetableEntry(
            id = id,
            courseId = m["courseId"] as? String ?: return@runCatching null,
            dayOfWeek = ((m["dayOfWeek"] as? Long) ?: (m["dayOfWeek"] as? Number)?.toLong())?.toInt() ?: return@runCatching null,
            startTime = m["startTime"] as? String ?: return@runCatching null,
            endTime = m["endTime"] as? String ?: return@runCatching null,
            room = m["room"] as? String ?: "",
            color = m["color"] as? String ?: "#FF5722",
            lastModified = (m["lastModified"] as? Long) ?: (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }.getOrElse { Log.e(TAG, "mapToTimetableEntry failed for id=$id", it); null }

    // ─── Attendance ──────────────────────────────────────────────────────────

    private suspend fun syncAttendance(userId: String) {
        val collection = userCollection(userId, "attendance")
        val localItems = db.attendanceDao().getAll()

        for (item in localItems) {
            Log.d(TAG, "Pushing attendance ${item.id}")
            collection.document(item.id).set(attendanceRecordToMap(item)).await()
        }

        val localIds = localItems.associateBy { it.id }
        val remoteDocs = collection.get().await()
        for (doc in remoteDocs.documents) {
            val remoteLastModified = doc.getLong("lastModified") ?: 0L
            val localLastModified = localIds[doc.id]?.lastModified ?: 0L
            if (remoteLastModified > localLastModified) {
                mapToAttendanceRecord(doc.id, doc.data ?: continue)?.let { db.attendanceDao().insert(it) }
            }
        }
    }

    private fun attendanceRecordToMap(a: AttendanceRecord): Map<String, Any?> = mapOf(
        "id" to a.id,
        "courseId" to a.courseId,
        "dateTime" to a.dateTime,
        "status" to a.status,
        "lastModified" to a.lastModified
    )

    private fun mapToAttendanceRecord(id: String, m: Map<String, Any>): AttendanceRecord? = runCatching {
        AttendanceRecord(
            id = id,
            courseId = m["courseId"] as? String ?: return@runCatching null,
            dateTime = (m["dateTime"] as? Long) ?: (m["dateTime"] as? Number)?.toLong() ?: return@runCatching null,
            status = m["status"] as? String ?: return@runCatching null,
            lastModified = (m["lastModified"] as? Long) ?: (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }.getOrElse { Log.e(TAG, "mapToAttendanceRecord failed for id=$id", it); null }

    // ─── Assignments ─────────────────────────────────────────────────────────

    private suspend fun syncAssignments(userId: String) {
        val collection = userCollection(userId, "assignments")
        val localItems = db.assignmentDao().getAll()

        for (item in localItems) {
            Log.d(TAG, "Pushing assignment ${item.id}")
            collection.document(item.id).set(assignmentToMap(item)).await()
        }

        val localIds = localItems.associateBy { it.id }
        val remoteDocs = collection.get().await()
        for (doc in remoteDocs.documents) {
            val remoteLastModified = doc.getLong("lastModified") ?: 0L
            val localLastModified = localIds[doc.id]?.lastModified ?: 0L
            if (remoteLastModified > localLastModified) {
                mapToAssignment(doc.id, doc.data ?: continue)?.let { db.assignmentDao().insert(it) }
            }
        }
    }

    private fun assignmentToMap(a: Assignment): Map<String, Any?> = mapOf(
        "id" to a.id,
        "name" to a.name,
        "courseId" to a.courseId,
        "dueDate" to a.dueDate,
        "totalMarks" to a.totalMarks,
        "obtainedMarks" to a.obtainedMarks,
        "weightage" to a.weightage,
        "isCompleted" to a.isCompleted,
        "lastModified" to a.lastModified
    )

    private fun mapToAssignment(id: String, m: Map<String, Any>): Assignment? = runCatching {
        Assignment(
            id = id,
            name = m["name"] as? String ?: return@runCatching null,
            courseId = m["courseId"] as? String ?: return@runCatching null,
            dueDate = (m["dueDate"] as? Long) ?: (m["dueDate"] as? Number)?.toLong() ?: return@runCatching null,
            totalMarks = (m["totalMarks"] as? Double) ?: (m["totalMarks"] as? Number)?.toDouble() ?: 0.0,
            obtainedMarks = (m["obtainedMarks"] as? Double) ?: (m["obtainedMarks"] as? Number)?.toDouble(),
            weightage = (m["weightage"] as? Double) ?: (m["weightage"] as? Number)?.toDouble() ?: 0.0,
            isCompleted = m["isCompleted"] as? Boolean ?: false,
            lastModified = (m["lastModified"] as? Long) ?: (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }.getOrElse { Log.e(TAG, "mapToAssignment failed for id=$id", it); null }

    // ─── Exams ───────────────────────────────────────────────────────────────

    private suspend fun syncExams(userId: String) {
        val collection = userCollection(userId, "exams")
        val localItems = db.examDao().getAll()

        for (item in localItems) {
            Log.d(TAG, "Pushing exam ${item.id}")
            collection.document(item.id).set(examToMap(item)).await()
        }

        val localIds = localItems.associateBy { it.id }
        val remoteDocs = collection.get().await()
        for (doc in remoteDocs.documents) {
            val remoteLastModified = doc.getLong("lastModified") ?: 0L
            val localLastModified = localIds[doc.id]?.lastModified ?: 0L
            if (remoteLastModified > localLastModified) {
                mapToExam(doc.id, doc.data ?: continue)?.let { db.examDao().insert(it) }
            }
        }
    }

    private fun examToMap(e: Exam): Map<String, Any?> = mapOf(
        "id" to e.id,
        "courseId" to e.courseId,
        "type" to e.type.name,
        "date" to e.date,
        "venue" to e.venue,
        "totalMarks" to e.totalMarks,
        "obtainedMarks" to e.obtainedMarks,
        "weightage" to e.weightage,
        "isCompleted" to e.isCompleted,
        "lastModified" to e.lastModified
    )

    private fun mapToExam(id: String, m: Map<String, Any>): Exam? = runCatching {
        Exam(
            id = id,
            courseId = m["courseId"] as? String ?: return@runCatching null,
            type = ExamType.valueOf(m["type"] as? String ?: return@runCatching null),
            date = (m["date"] as? Long) ?: (m["date"] as? Number)?.toLong() ?: return@runCatching null,
            venue = m["venue"] as? String,
            totalMarks = (m["totalMarks"] as? Double) ?: (m["totalMarks"] as? Number)?.toDouble() ?: 0.0,
            obtainedMarks = (m["obtainedMarks"] as? Double) ?: (m["obtainedMarks"] as? Number)?.toDouble(),
            weightage = (m["weightage"] as? Double) ?: (m["weightage"] as? Number)?.toDouble() ?: 0.0,
            isCompleted = m["isCompleted"] as? Boolean ?: false,
            lastModified = (m["lastModified"] as? Long) ?: (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }.getOrElse { Log.e(TAG, "mapToExam failed for id=$id", it); null }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun userCollection(userId: String, collectionName: String) =
        firestore.collection("users").document(userId).collection(collectionName)
}
