package com.studyflow.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.studyflow.logic.Reminder

private const val CHANNEL_ID = "deadline_reminders"
private const val PREFS = "studyflow_reminders"
private const val KEY_IDS = "scheduled_ids"
private const val TEST_ID = 424242

/**
 * Schedules each reminder with the system alarm service, so it is delivered even when the app is
 * closed. The alarms are re-planned every time the app opens (the phone forgets them on restart).
 */
class AndroidNotifier(private val context: Context) : Notifier {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun sync(reminders: List<Reminder>) {
        try {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // forget what was scheduled before
            val old = prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
            for (id in old) {
                alarms.cancel(pendingIntent(context, id.toInt(), "", ""))
            }

            val ids = HashSet<String>()
            for (reminder in reminders) {
                alarms.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.at,
                    pendingIntent(context, reminder.id, reminder.title, reminder.text),
                )
                ids += reminder.id.toString()
            }
            prefs.edit().putStringSet(KEY_IDS, ids).apply()
        } catch (e: Exception) {
            // a failure to schedule must never crash the app
        }
    }

    override fun showNow(title: String, text: String) {
        showReminder(context, TEST_ID, title, text)
    }
}

private fun pendingIntent(context: Context, id: Int, title: String, text: String): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java)
        .putExtra("id", id)
        .putExtra("title", title)
        .putExtra("text", text)
    return PendingIntent.getBroadcast(
        context,
        id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Called by the system when a reminder is due. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        showReminder(context, intent.getIntExtra("id", 0), title, intent.getStringExtra("text") ?: "")
    }
}

fun showReminder(context: Context, id: Int, title: String, text: String) {
    if (Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return // the student has not allowed notifications
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Deadline reminders", NotificationManager.IMPORTANCE_DEFAULT),
    )

    val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val tap = if (openApp != null) PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE) else null

    val builder = Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
    if (tap != null) builder.setContentIntent(tap)
    manager.notify(id, builder.build())
}
