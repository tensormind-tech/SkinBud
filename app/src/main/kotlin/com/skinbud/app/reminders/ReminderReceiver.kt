package com.skinbud.app.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.skinbud.app.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_KIND = "kind"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: "morning"

        val (title, body, notificationId, deepLink) = when (kind) {
            "morning" -> Quad(
                "Morning Skincare Protocol",
                "Cleanse, hydrate, and don't forget your broad-spectrum SPF 50+!",
                1001,
                "skinbud://routine?time=morning"
            )
            "evening" -> Quad(
                "Evening Barrier Restoration",
                "Wash away airborne pollutants and nourish with restorative night ceramides.",
                1002,
                "skinbud://routine?time=evening"
            )
            else -> Quad(
                "Daily Skin Health Check",
                "Consistency reveals true progress. Take 5 seconds for today's scan.",
                1003,
                "skinbud://scan"
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
