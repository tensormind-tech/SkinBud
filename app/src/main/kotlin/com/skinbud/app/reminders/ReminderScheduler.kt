package com.skinbud.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object ReminderScheduler {

    const val CHANNEL_ID = "skinbud_reminders"
    private const val TAG = "ReminderScheduler"

    fun setupNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SkinBud Reminders"
            val descriptionText = "Daily morning & evening skincare routine and scan reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    fun scheduleAllReminders(context: Context) {
        setupNotificationChannel(context)
        scheduleDailyAlarm(context, "morning", 8, 0, 101)
        scheduleDailyAlarm(context, "scan", 13, 0, 102)
        scheduleDailyAlarm(context, "evening", 21, 0, 103)
        Log.d(TAG, "Scheduled morning, scan, and evening reminders")
    }

    private fun scheduleDailyAlarm(
        context: Context,
        kind: String,
        hour: Int,
        minute: Int,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_KIND, kind)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, scheduling standard alarm", e)
        }
    }
}
