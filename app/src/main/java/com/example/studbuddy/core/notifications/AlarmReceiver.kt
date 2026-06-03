package com.example.studbuddy.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val typeStr = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: return
        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (e: IllegalArgumentException) {
            return
        }
        
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "You have a task pending."

        NotificationHelper.showNotification(context, type, itemId, title, body)
    }

    companion object {
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}
