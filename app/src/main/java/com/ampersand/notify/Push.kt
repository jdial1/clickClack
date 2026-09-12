package com.ampersand.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ampersand.MainActivity
import com.ampersand.R
import com.ampersand.auth.hasFirebaseConfigFor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val TAG = "Push"

/**
 * The one thing the daily reminder cannot do: tell you the *other* person moved.
 *
 * The local alarm knows the time of day and nothing else. It cannot know that
 * your partner answered four minutes ago, because knowing that means something
 * off this phone telling it -- and the whole shape of the app is two people
 * waiting on each other. A seal that lifts in silence is a seal nobody notices
 * until tomorrow.
 *
 * The token lives on your own user document, which only you may write. The
 * thing that reads it and sends the message is a Cloud Function, because a
 * client cannot send a push to another client without shipping a server key,
 * and a server key in an APK is a server key everyone has.
 */
object Push {

    /**
     * Put this phone's token where the sender can find it.
     *
     * Idempotent and cheap, so it runs on every signed-in launch rather than
     * being tracked: a token can be rotated by the system at any time, and a
     * stale one fails silently, which is the worst kind of broken.
     */
    fun register(context: Context, uid: String) {
        if (!hasFirebaseConfigFor(context)) return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> write(uid, token) }
            .addOnFailureListener { Log.w(TAG, "no token", it) }
    }

    internal fun write(uid: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }
}

/**
 * Receives the push and draws it.
 *
 * Deliberately renders a *data* message rather than letting the system draw a
 * notification payload: the copy is decided by the sender, but the icon,
 * channel and tap target are this app's business, and a notification-payload
 * message is drawn by the OS with none of them when the app is in the
 * background.
 */
class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { Push.write(it, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: return
        val body = message.data["body"].orEmpty()

        Reminder.ensureChannel(this)

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val note = NotificationCompat.Builder(this, "daily")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        runCatching {
            // Throws when the user declined notifications, which is a normal
            // outcome and not a crash.
            NotificationManagerCompat.from(this).notify(2, note)
        }
    }
}
