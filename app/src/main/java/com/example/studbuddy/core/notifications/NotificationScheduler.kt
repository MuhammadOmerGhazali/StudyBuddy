package com.example.studbuddy.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.studbuddy.core.SettingsManager
import com.example.studbuddy.core.models.Assignment
import com.example.studbuddy.core.models.Exam
import com.example.studbuddy.core.models.TimetableEntry
import com.example.studbuddy.core.notifications.di.NotificationEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private fun getSettingsManager(context: Context): SettingsManager {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationEntryPoint::class.java
        )
        return entryPoint.settingsManager()
    }

    suspend fun scheduleAssignmentReminder(context: Context, assignment: Assignment, courseName: String) {
        val settingsManager = getSettingsManager(context)
        val enabled = settingsManager.assignmentRemindersEnabled.first()
        if (!enabled) return

        val leadTime = settingsManager.assignmentLeadTime.first()
        val triggerTime = assignment.dueDate - TimeUnit.HOURS.toMillis(leadTime.toLong())
        if (triggerTime <= System.currentTimeMillis()) return

        val intent = createBaseIntent(context, NotificationType.ASSIGNMENT_DUE, assignment.id).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, "Assignment Due Soon")
            putExtra(AlarmReceiver.EXTRA_BODY, "${assignment.name} ($courseName) is due in $leadTime hours.")
        }

        setAlarm(context, triggerTime, intent, getRequestCode(NotificationType.ASSIGNMENT_DUE, assignment.id))
    }

    suspend fun scheduleExamReminders(context: Context, exam: Exam, courseName: String) {
        val settingsManager = getSettingsManager(context)
        val enabled = settingsManager.examRemindersEnabled.first()
        if (!enabled) return

        val leadTime = settingsManager.examLeadTime.first()
        // User lead time
        scheduleExamAlarm(context, exam, courseName, TimeUnit.HOURS.toMillis(leadTime.toLong()), "${leadTime}h")
        
        // 1h before as a fail-safe (unless user lead time is already <= 1h)
        if (leadTime > 1) {
            scheduleExamAlarm(context, exam, courseName, TimeUnit.HOURS.toMillis(1), "1h")
        }
    }

    private fun scheduleExamAlarm(context: Context, exam: Exam, courseName: String, offsetMs: Long, suffix: String) {
        val triggerTime = exam.date - offsetMs
        if (triggerTime <= System.currentTimeMillis()) return

        val intent = createBaseIntent(context, NotificationType.EXAM_REMINDER, "${exam.id}_$suffix").apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, "Upcoming Exam")
            val hours = offsetMs / 3_600_000
            val leadTime = if (hours >= 24) "${hours / 24} day" else "$hours hour"
            putExtra(AlarmReceiver.EXTRA_BODY, "${exam.type} for $courseName in $leadTime.")
        }

        setAlarm(context, triggerTime, intent, getRequestCode(NotificationType.EXAM_REMINDER, "${exam.id}_$suffix"))
    }

    suspend fun scheduleTimetableReminder(context: Context, entry: TimetableEntry, courseName: String) {
        val settingsManager = getSettingsManager(context)
        val mode = settingsManager.classReminderMode.first()
        if (mode != SettingsManager.MODE_EACH_LECTURE) return

        val nextClassTime = getNextClassTime(entry.dayOfWeek, entry.startTime) ?: return
        val leadTime = settingsManager.lectureLeadTime.first()
        val triggerTime = nextClassTime - TimeUnit.MINUTES.toMillis(leadTime.toLong())
        
        // If lead time before class is already passed for TODAY, getNextClassTime will already return next week.
        // But double check
        if (triggerTime <= System.currentTimeMillis()) return

        val intent = createBaseIntent(context, NotificationType.TIMETABLE_CLASS, entry.id).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, "Class Reminder")
            val timeUnit = if (leadTime >= 60) "hour" else "minutes"
            val displayTime = if (leadTime >= 60) leadTime / 60 else leadTime
            putExtra(AlarmReceiver.EXTRA_BODY, "$courseName starts in $displayTime $timeUnit in room ${entry.room}.")
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(NotificationType.TIMETABLE_CLASS, entry.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, type: NotificationType, itemId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(type, itemId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    suspend fun rescheduleAllClassReminders(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationEntryPoint::class.java
        )
        val repository = entryPoint.studBuddyRepository()
        val settingsManager = entryPoint.settingsManager()

        val timetable = repository.getTimetable()
        val courses = repository.getCourses().associateBy { it.id }
        val mode = settingsManager.classReminderMode.first()

        timetable.forEach { entry ->
            // Always cancel first to avoid duplicates or orphaned alarms
            cancelReminder(context, NotificationType.TIMETABLE_CLASS, entry.id)
            
            if (mode == SettingsManager.MODE_EACH_LECTURE) {
                scheduleTimetableReminder(context, entry, courses[entry.courseId]?.name ?: "Unknown")
            }
        }
    }

    suspend fun rescheduleAllAssignmentReminders(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationEntryPoint::class.java
        )
        val repository = entryPoint.studBuddyRepository()
        val settingsManager = entryPoint.settingsManager()

        val assignments = repository.getAssignments()
        val courses = repository.getCourses().associateBy { it.id }
        val enabled = settingsManager.assignmentRemindersEnabled.first()

        assignments.forEach { assignment ->
            cancelReminder(context, NotificationType.ASSIGNMENT_DUE, assignment.id)
            if (enabled && !assignment.isCompleted) {
                scheduleAssignmentReminder(context, assignment, courses[assignment.courseId]?.name ?: "Unknown")
            }
        }
    }

    suspend fun rescheduleAllExamReminders(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationEntryPoint::class.java
        )
        val repository = entryPoint.studBuddyRepository()
        val settingsManager = entryPoint.settingsManager()

        val exams = repository.getExams()
        val courses = repository.getCourses().associateBy { it.id }
        val enabled = settingsManager.examRemindersEnabled.first()
        val leadTime = settingsManager.examLeadTime.first()

        exams.forEach { exam ->
            // Cancel all potential suffixes
            cancelReminder(context, NotificationType.EXAM_REMINDER, "${exam.id}_24h")
            cancelReminder(context, NotificationType.EXAM_REMINDER, "${exam.id}_1h")
            // Also cancel using current setting to be safe
            cancelReminder(context, NotificationType.EXAM_REMINDER, "${exam.id}_${leadTime}h")
            
            if (enabled && !exam.isCompleted) {
                scheduleExamReminders(context, exam, courses[exam.courseId]?.name ?: "Unknown")
            }
        }
    }

    private fun createBaseIntent(context: Context, type: NotificationType, itemId: String): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_TYPE, type.name)
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, itemId)
        }
    }

    private fun setAlarm(context: Context, triggerTime: Long, intent: Intent, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun getRequestCode(type: NotificationType, itemId: String): Int {
        return (type.ordinal * 100_000 + itemId.hashCode()).and(0x7FFFFFFF)
    }

    private fun getNextClassTime(dayOfWeek: Int, startTime: String): Long? {
        return try {
            val parts = startTime.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                // Calendar.MONDAY = 2, SUNDAY = 1.
                // Our dayOfWeek: 1=Mon...7=Sun
                var calendarDay = dayOfWeek + 1
                if (calendarDay > 7) calendarDay = 1
                
                set(Calendar.DAY_OF_WEEK, calendarDay)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                if (before(now) || timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 7)
                }
            }
            target.timeInMillis
        } catch (e: Exception) {
            null
        }
    }
}
