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
 * One reminder a day, at a time the user picked.
 *
 * Local, not push. The thing being reminded about is "a question is waiting",
 * which is true every day at the same time and needs no server to know it --
 * and a personal app for two people should not require a messaging
 * infrastructure to tell you it is Tuesday.
 *
 * Stored per device in SharedPreferences rather than on the couple. It is a
 * property of the phone in your pocket, not a fact about the pair: you and your
 * partner should be able to want reminding at different times, or not at all.
 *
 * To watch one land, move the clock rather than sending the broadcast: the
 * receiver is not exported, so `adb shell am broadcast` is dropped in silence
 * and looks exactly like a reminder that decided it had nothing to say.
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
     * What is actually outstanding, left where the receiver can read it.
     *
     * The alarm fires in a broadcast receiver with no app process, no signed-in
     * Firebase session and a few milliseconds to live, so it cannot go and ask
     * Firestore what is waiting. The app knows, every time its state changes, so
     * it leaves a note instead. Stale by at most one app launch, which is the
     * right trade for a nudge.
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
            // Already gone today, so the first one is tomorrow. Without this the
            // alarm fires the instant it is set, at any hour.
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        // Inexact on purpose. An exact alarm needs a special permission on
        // recent Android and is for things like alarm clocks; a nudge that
        // arrives within a few minutes of eight is a nudge that arrived.
        alarms.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending(context),
        )
    }

    fun cancel(context: Context) {
        prefs(context).edit().remove(KEY_HOUR).remove(KEY_MINUTE)
            .putBoolean(KEY_ASKED, true).apply()
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
                // Default, not high: this is a nudge, not an emergency, and a
                // heads-up notification for a question would be rude.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "One reminder a day that a question is waiting." },
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

        // Nothing outstanding, so nothing to say. An app that reminds you about
        // a thing you have already done is an app you turn notifications off
        // for, and the streak is not a reason to interrupt someone.
        if (!questionPending && decksWaiting == 0) return

        Reminder.ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Says the most useful true thing, and only one of them. No streak, no
        // exclamation, no count of days missed.
        val decks = if (decksWaiting == 1) "a deck" else "$decksWaiting decks"
        val title = when {
            questionPending && decksWaiting > 0 -> "Today's question, and $decks"
            decksWaiting > 0 -> "$partner finished $decks"
            else -> "Today's question is waiting"
        }
        val body = when {
            decksWaiting > 0 -> "Waiting on you."
            else -> "Answer it and it opens when they do."
        }

        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        runCatching {
            // Throws when the user declined the notifications permission, which
            // is a normal outcome and not a crash.
            androidx.core.app.NotificationManagerCompat.from(context).notify(1, note)
        }
    }
}
