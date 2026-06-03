package com.example.studbuddy.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.studbuddy.MainActivity
import com.example.studbuddy.R

enum class NotificationType(val channelId: String, val channelName: String) {
    ASSIGNMENT_DUE("channel_assignments", "Assignment Deadlines"),
    EXAM_REMINDER("channel_exams", "Exam Reminders"),
    TIMETABLE_CLASS("channel_timetable", "Class Reminders"),
    ATTENDANCE_WARNING("channel_attendance", "Attendance Warnings")
}

object NotificationHelper {

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            NotificationType.entries.forEach { type ->
                val importance = when (type) {
                    NotificationType.ASSIGNMENT_DUE -> NotificationManager.IMPORTANCE_HIGH
                    NotificationType.EXAM_REMINDER -> NotificationManager.IMPORTANCE_HIGH
                    NotificationType.TIMETABLE_CLASS -> NotificationManager.IMPORTANCE_DEFAULT
                    NotificationType.ATTENDANCE_WARNING -> NotificationManager.IMPORTANCE_HIGH
                }
                val channel = NotificationChannel(type.channelId, type.channelName, importance).apply {
                    description = "StudBuddy ${type.channelName}"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showNotification(
        context: Context,
        type: NotificationType,
        itemId: String,
        title: String,
        body: String
    ) {
        // Build target intent to open MainActivity (Navigation handles deep linking or just landing)
        val targetIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.hashCode(),
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, type.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (type.ordinal * 100_000 + itemId.hashCode()).and(0x7FFFFFFF)
        notificationManager.notify(notificationId, notification)
    }
}
