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
            // User closed the sheet; not an error.
            Log.d(TAG, "Sign-in dismissed")
            Result.success(Unit)
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
