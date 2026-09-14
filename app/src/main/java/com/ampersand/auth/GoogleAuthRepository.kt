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

        // The "Sign in with Google" sheet first. If Google rejects it after an account is picked
        // ("[16] Account reauth failed"), try the older account picker, which takes another path.
        val button = GetSignInWithGoogleOption.Builder(clientId).build()
        val picker = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        return try {
            val response = try {
                credentials.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(button).build())
            } catch (e: GetCredentialCancellationException) {
                if (e.isUserCancel()) return Result.success(Unit)
                Log.w(TAG, "Sign in with Google rejected, trying the account picker: ${e.errorMessage}")
                credentials.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(picker).build())
            }
            val token = GoogleIdTokenCredential.createFrom(response.credential.data).idToken

            signInToFirebase(token).recoverCatching { throw diagnose("Firebase sign-in", it) }
        } catch (e: GetCredentialCancellationException) {
            // Closing the sheet is not an error. A rejection reported as a cancellation is.
            if (e.isUserCancel()) Result.success(Unit) else signInInBrowser(activity, e)
        } catch (e: NoCredentialException) {
            // No account Google will hand this install, which is also what an unmatched signing key looks like.
            signInInBrowser(activity, e)
        } catch (e: GetCredentialException) {
            Result.failure(diagnose("Credential Manager", e))
        } catch (e: Exception) {
            Result.failure(diagnose("Unexpected", e))
        }
    }

    /**
     * Last resort: Firebase's own Google sign-in in a browser tab. It goes through the web client
     * and firebaseapp.com, so it works whatever key signed this install, where Credential Manager
     * needs Google to match the signing key to an Android OAuth client.
     */
    private suspend fun signInInBrowser(activity: Activity, earlier: Throwable): Result<Unit> {
        Log.w(TAG, "Credential Manager failed, signing in through the browser", earlier)
        return suspendCancellableCoroutine { cont ->
            val provider = com.google.firebase.auth.OAuthProvider.newBuilder("google.com").build()
            val pending = auth.pendingAuthResult
            val task = pending ?: auth.startActivityForSignInWithProvider(activity, provider)
            task.addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener {
                    val cancelled = it is com.google.firebase.auth.FirebaseAuthException &&
                        it.errorCode == "ERROR_WEB_CONTEXT_CANCELED"
                    cont.resume(if (cancelled) Result.success(Unit) else Result.failure(diagnose("Browser sign-in", it.also { f -> f.addSuppressed(earlier) })))
                }
        }
    }

    private fun GetCredentialCancellationException.isUserCancel(): Boolean {
        val message = errorMessage?.toString().orEmpty()
        return message.isBlank() || message.contains("cancelled by the user", ignoreCase = true)
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

    /**
     * A failed sign-in, spelled out for a bug report: where it failed, the whole cause chain,
     * and the facts Google matches on (package, signing key, web client id, installer).
     * Logged under "GoogleAuth" and shown, selectable, on the sign-in screen.
     */
    private fun diagnose(stage: String, e: Throwable): Throwable {
        val causes = generateSequence(e) { it.cause }.take(5).joinToString("\n") { t ->
            val type = (t as? GetCredentialException)?.type?.let { " [$it]" }.orEmpty()
            "- ${t.javaClass.simpleName}$type: ${(t as? GetCredentialException)?.errorMessage ?: t.message}"
        }
        val pm = appContext.packageManager
        val installer = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(appContext.packageName).installingPackageName
            else @Suppress("DEPRECATION") pm.getInstallerPackageName(appContext.packageName)
        }.getOrNull() ?: "sideloaded"
        val playServices = runCatching { pm.getPackageInfo("com.google.android.gms", 0).versionName }.getOrNull() ?: "missing"
        val text = buildString {
            appendLine("Google sign-in didn't finish ($stage).")
            appendLine(causes)
            appendLine("App: ${appContext.packageName} ${com.ampersand.BuildConfig.VERSION_NAME} (${com.ampersand.BuildConfig.BUILD_TYPE})")
            appendLine("Installed by: $installer")
            appendLine("App key SHA-1: ${signingSha1()}")
            appendLine("Web client: ${com.ampersand.BuildConfig.WEB_CLIENT_ID}")
            append("Play services: $playServices, Android ${android.os.Build.VERSION.RELEASE}")
        }
        Log.e(TAG, text, e)
        return IllegalStateException(text, e)
    }

    /**
     * The SHA-1 this install is signed with, the value Google matches against the OAuth clients.
     * Shown on a rejected sign-in: Play, internal app sharing and sideloads each sign differently.
     */
    private fun signingSha1(): String = runCatching {
        val info = appContext.packageManager.getPackageInfo(
            appContext.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES,
        )
        val signing = info.signingInfo!!
        fun sha(sig: android.content.pm.Signature) =
            java.security.MessageDigest.getInstance("SHA-1").digest(sig.toByteArray()).joinToString(":") { "%02X".format(it) }
        val current = sha(signing.apkContentsSigners.first())
        // With key rotation, Google may match on an older key in the lineage, not the current one.
        val history = if (signing.hasPastSigningCertificates()) signing.signingCertificateHistory.map(::sha) else emptyList()
        if (history.size > 1) "$current (history: ${history.joinToString(", ")})" else current
    }.getOrDefault("unknown")

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
