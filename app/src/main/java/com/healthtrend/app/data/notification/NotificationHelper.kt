package com.healthtrend.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.healthtrend.app.MainActivity
import com.healthtrend.app.R
import com.healthtrend.app.data.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for notification channel creation and reminder notification display.
 *
 * - Creates "Reminders" channel with default importance (gentle notifications).
 * - Builds and shows one notification per [TimeSlot].
 * - Tap opens [MainActivity] which navigates to today's Day Card.
 * - Auto-cancel on tap.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Create the "Reminders" notification channel.
     * Safe to call multiple times — no-op if channel already exists.
     * Call once at app startup (Application.onCreate).
     */
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Show a reminder notification for a specific [TimeSlot].
     *
     * - Content: "Time to log your [displayName] entry" (AC #1)
     * - Tap: opens MainActivity → Day Card for today (AC #2)
     * - Auto-cancel on tap (AC #3 — no follow-up)
     * - Each slot uses a unique notification ID (AC #4 — no overwriting)
     */
    fun showReminderNotification(timeSlot: TimeSlot) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            getNotificationId(timeSlot),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(getReminderText(timeSlot))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(getNotificationId(timeSlot), notification)
    }

    companion object {
        const val CHANNEL_ID = "healthtrend_reminders"
        const val CHANNEL_NAME = "Reminders"
        const val CHANNEL_DESCRIPTION = "Daily symptom logging reminders"
        const val NOTIFICATION_TITLE = "HealthTrend"
    }
}
