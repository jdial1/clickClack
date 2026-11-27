package com.ampersand.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ampersand.MainActivity
import com.ampersand.R

/**
 * The one way the app shows a notification: one channel, the app's icon, and a
 * tap that opens the app. The daily reminder and partner pushes both use it.
 */
internal object Notifier {

    /** The one notification channel. Its id is stored by Android, so it never changes. */
    const val CHANNEL = "daily"

    /** Which notification a new one replaces: one reminder and one push at a time. */
    const val REMINDER = 1
    const val PUSH = 2

    fun ensureChannel(context: Context) {
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

    /** Shows [title] and [body] as notification [id]. Does nothing if notifications are denied. */
    fun show(context: Context, id: Int, title: String, body: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // Throws if notifications are denied; there is nothing to do about that here.
        runCatching { NotificationManagerCompat.from(context).notify(id, note) }
    }
}
