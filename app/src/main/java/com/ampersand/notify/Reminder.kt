package com.ampersand.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ampersand.MainActivity
import com.ampersand.R
import java.util.Calendar

private const val CHANNEL = "daily"
private const val REQUEST = 4711
private const val PREFS = "reminder"
private const val KEY_HOUR = "hour"
private const val KEY_MINUTE = "minute"
private const val KEY_ASKED = "asked"
private const val KEY_QUESTION = "questionPending"
private const val KEY_DECKS = "decksWaiting"
private const val KEY_PARTNER = "partnerName"

/**
 * A daily local reminder at a user-picked time, stored per device in SharedPreferences.
 *
 * To test, move the clock: the receiver is not exported, so `adb shell am broadcast`
 * is silently dropped.
 *
 * ```
 * adb shell cmd alarm set-time <millis just before the chosen hour>
 * adb shell dumpsys notification --noredact | grep -A20 ampersand
 * adb shell settings put global auto_time 1
 * ```
 */
object Reminder {

    /** Whether we have already asked, so the setup screen is shown once. */
    fun hasBeenAsked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) {
        prefs(context).edit().putBoolean(KEY_ASKED, true).apply()
    }

    /**
     * Cache what is outstanding for the receiver, which cannot query Firestore.
     * Can be stale by one app launch.
     */
    fun cacheOutstanding(
        context: Context,
        questionPending: Boolean,
        decksWaiting: Int,
        partnerName: String,
    ) {
        prefs(context).edit()
            .putBoolean(KEY_QUESTION, questionPending)
            .putInt(KEY_DECKS, decksWaiting)
            .putString(KEY_PARTNER, partnerName)
            .apply()
    }

    internal fun outstanding(context: Context): Triple<Boolean, Int, String> {
        val p = prefs(context)
        return Triple(
            p.getBoolean(KEY_QUESTION, true),
            p.getInt(KEY_DECKS, 0),
            p.getString(KEY_PARTNER, null) ?: "your partner",
        )
    }

    /** The chosen time, or null when reminders are off. */
    fun time(context: Context): Pair<Int, Int>? {
        val p = prefs(context)
        if (!p.contains(KEY_HOUR)) return null
        return p.getInt(KEY_HOUR, 20) to p.getInt(KEY_MINUTE, 0)
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        prefs(context).edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .putBoolean(KEY_ASKED, true)
            .apply()

        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Time already passed today: start tomorrow, or it fires immediately.
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        // Inexact: exact alarms need a special permission on recent Android.
        alarms.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending(context),
        )
    }

    /** Stop the alarm and clear all cached state, including the asked flag. Called on sign-out and unpair. */
    fun cancel(context: Context) {
        prefs(context).edit().clear().apply()
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context))
    }

    /** Re-arm after a reboot, which clears every alarm the system holds. */
    fun restore(context: Context) {
        time(context)?.let { (h, m) -> schedule(context, h, m) }
    }

    internal fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "Daily question",
                // Default importance: no heads-up popup.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "A daily reminder when a question is waiting." },
        )
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Fires the reminder, and re-arms it after a reboot. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminder.restore(context)
            return
        }
        val (questionPending, decksWaiting, partner) = Reminder.outstanding(context)

        // Nothing outstanding: no notification.
        if (!questionPending && decksWaiting == 0) return

        Reminder.ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // One short message about what is waiting.
        val decks = if (decksWaiting == 1) "a deck" else "$decksWaiting decks"
        val title = when {
            questionPending && decksWaiting > 0 -> "Today's question and $decks"
            decksWaiting > 0 -> "$partner finished $decks"
            else -> "Today's question is waiting"
        }
        val body = when {
            // What is waiting, never who is waiting on whom.
            decksWaiting > 0 -> "It opens when you've played it too."
            else -> "Opens when you both answer."
        }

        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        runCatching {
            // Throws if notifications are denied.
            androidx.core.app.NotificationManagerCompat.from(context).notify(1, note)
        }
    }
}
