package com.ampersand.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "GoogleAuth"

/**
 * Google sign-in via Credential Manager, then exchanged for a Firebase session.
 * The Firebase session is what Firestore rules check; the Google ID token alone is not enough.
 */
class GoogleAuthRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthRepository {

    private val appContext = context.applicationContext
    private val credentials = CredentialManager.create(appContext)

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    private val listener = FirebaseAuth.AuthStateListener { it.currentUser.publish() }

    init {
        // Fires immediately with the cached session, ending AuthState.Loading.
        auth.addAuthStateListener(listener)
    }

    private fun FirebaseUser?.publish() {
        _state.value = if (this == null) {
            AuthState.SignedOut
        } else {
            AuthState.SignedIn(
                uid = uid,
                // A Google account may have no display name.
                displayName = displayName?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore('@')
                    ?: "You",
                email = email,
                photoUrl = photoUrl?.toString(),
            )
        }
    }

    override suspend fun signInWithGoogle(activity: Activity): Result<Unit> {
        val clientId = webClientId(appContext)
            ?: return Result.failure(
                IllegalStateException(
                    "No default_web_client_id. Drop google-services.json into app/ " +
                        "and rebuild; the plugin generates it from the OAuth web client.",
                ),
            )

        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                .build()

            val response = credentials.getCredential(activity, request)
            val token = GoogleIdTokenCredential.createFrom(response.credential.data).idToken

            signInToFirebase(token)
        } catch (e: GetCredentialCancellationException) {
            // Closing the sheet is not an error. But Google also reports a rejected sign-in after
            // an account is picked (a signing key it does not know: "[16] Account reauth failed")
            // as a cancellation, and swallowing that made the button look dead.
            val message = e.errorMessage?.toString().orEmpty()
            Log.w(TAG, "Sign-in cancelled: $message")
            if (message.isBlank() || message.contains("cancelled by the user", ignoreCase = true)) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Google sign-in didn't finish. $message"))
            }
        } catch (e: NoCredentialException) {
            // No Google account on the device at all.
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager failed", e)
            Result.failure(e)
        }
    }

    /** Exchange the Google ID token for a Firebase session. */
    private suspend fun signInToFirebase(idToken: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }

    override fun signOut() {
        auth.signOut()
        _state.value = AuthState.SignedOut
    }

    /**
     * Calls the deleteAccount callable over plain HTTPS with the Firebase ID token, which is all
     * the callable protocol is; cheaper than adding the Functions SDK for one call.
     */
    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        val token = suspendCancellableCoroutine { cont ->
            user.getIdToken(true)
                .addOnSuccessListener { cont.resume(Result.success(it.token.orEmpty())) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }.getOrThrow()
        val project = com.google.firebase.FirebaseApp.getInstance().options.projectId
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val connection = java.net.URL("https://us-central1-$project.cloudfunctions.net/deleteAccount")
                .openConnection() as java.net.HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.outputStream.use { it.write("{\"data\":null}".toByteArray()) }
                check(connection.responseCode == 200) { "Couldn't delete your account (${connection.responseCode}). Try again." }
            } finally {
                connection.disconnect()
            }
        }
        signOut()
    }

    /** Call when the owning scope dies, or the listener outlives this object. */
    fun close() = auth.removeAuthStateListener(listener)

    private companion object {
        /**
         * The OAuth web client id from google-services.json, baked into BuildConfig (empty if absent).
         * Not a `getIdentifier` lookup: the resource shrinker removed that string in release.
         */
        fun webClientId(context: Context): String? =
            com.ampersand.BuildConfig.WEB_CLIENT_ID.takeIf { it.isNotBlank() }
    }
}
