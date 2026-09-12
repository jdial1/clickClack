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
 * Google sign-in through Credential Manager, exchanged for a Firebase session.
 *
 * Credential Manager rather than the old `GoogleSignIn` API: that one is
 * deprecated, and its `startActivityForResult` flow needs a launcher plumbed
 * through the composable tree for what is conceptually one suspending call.
 *
 * The flow is two hops and it is worth being clear which is which. Credential
 * Manager returns a **Google ID token**, which proves who the person is to
 * Google. That is then exchanged with Firebase for a **Firebase session**,
 * which is what the Firestore security rules actually check. Skipping the
 * second hop leaves you with a signed-in user the database has never heard of.
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
        // Fires immediately with the cached session, which is why AuthState has
        // a Loading case: without it the sign-in screen flashes on every cold
        // start before this first callback lands.
        auth.addAuthStateListener(listener)
    }

    private fun FirebaseUser?.publish() {
        _state.value = if (this == null) {
            AuthState.SignedOut
        } else {
            AuthState.SignedIn(
                uid = uid,
                // A Google account can genuinely have no display name set.
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
            // They closed the sheet. Not a failure worth telling them about.
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

    /**
     * Exchange the Google ID token for a Firebase session.
     *
     * Wrapped by hand rather than pulling in kotlinx-coroutines-play-services
     * for the single `await()` this file would use it for.
     */
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
         * The OAuth **web** client id, baked in at build time.
         *
         * Read straight out of google-services.json by the build script, so an
         * absent config gives an empty string rather than a missing symbol.
         *
         * This used to be a `Resources.getIdentifier` lookup, for the same
         * reason: the generated string only exists when a Firebase config does.
         * But a reflective lookup is invisible to the resource shrinker, which
         * duly removed the string from the release build -- and the app then
         * reported "No default_web_client_id" on a build that had one. A
         * constant cannot be shrunk away.
         */
        fun webClientId(context: Context): String? =
            com.ampersand.BuildConfig.WEB_CLIENT_ID.takeIf { it.isNotBlank() }
    }
}
