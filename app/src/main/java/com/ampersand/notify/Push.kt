package com.ampersand.notify

import com.google.firebase.firestore.FieldValue
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
 * FCM token registration, for pushes when your partner does something.
 *
 * The token is stored on your own user doc (owner-only write). A Cloud Function
 * sends the push, since clients cannot without shipping a server key.
 */
object Push {

    /** Save this phone's token. Runs on every signed-in launch, since tokens can rotate silently. */
    fun register(context: Context, uid: String) {
        if (!hasFirebaseConfigFor(context)) return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> write(uid, token) }
            .addOnFailureListener { Log.w(TAG, "no token", it) }
    }

    /**
     * Remove this phone's token, then run [then] (even on failure).
     * Call before sign-out: the write needs the account's credentials.
     */
    fun unregister(context: Context, uid: String, then: () -> Unit) {
        if (!hasFirebaseConfigFor(context)) return then()
        FirebaseMessaging.getInstance().deleteToken()
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("fcmToken", FieldValue.delete())
            .addOnCompleteListener { then() }
    }

    internal fun write(uid: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }
}

/**
 * Shows incoming pushes. Uses data messages so this app sets the icon, channel
 * and tap target even when backgrounded.
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
            // Throws if notifications are denied.
            NotificationManagerCompat.from(this).notify(2, note)
        }
    }
}
