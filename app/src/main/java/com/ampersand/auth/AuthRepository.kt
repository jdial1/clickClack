package com.ampersand.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google auth when the build has a Firebase config, otherwise the fake.
 * Checks a BuildConfig constant, not a resource by name, which the release shrinker could remove.
 */
fun createAuthRepository(context: Context): AuthRepository =
    if (hasFirebaseConfigFor(context)) GoogleAuthRepository(context) else FakeAuthRepository()

fun hasFirebaseConfigFor(@Suppress("UNUSED_PARAMETER") context: Context): Boolean =
    com.ampersand.BuildConfig.WEB_CLIENT_ID.isNotBlank()

/** Unwraps to the hosting Activity (Credential Manager needs one; LocalContext may be a wrapper). */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/** Auth state. [Loading] is separate from [SignedOut] so cold starts do not flash the sign-in screen. */
sealed interface AuthState {

    /** Before the first answer from the auth layer. Show nothing. */
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(
        val uid: String,
        val displayName: String,
        val email: String?,
        val photoUrl: String?,
    ) : AuthState
}

/** Auth interface the UI uses, so the app runs with or without Firebase. */
interface AuthRepository {

    val state: StateFlow<AuthState>

    /**
     * Sign in with Google. Needs an [Activity] to host the Credential Manager sheet.
     * Returns a [Result] instead of throwing; dismissing the sheet is not an error.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<Unit>

    fun signOut()

    /** Delete the account and everything that is yours (the deleteAccount Cloud Function), then sign out. */
    suspend fun deleteAccount(): Result<Unit>
}

/** Always signed in as the mock user, for builds without Firebase. */
class FakeAuthRepository(
    private val uid: String = "u_justin",
    private val displayName: String = "Justin",
) : AuthRepository {

    private val signedIn =
        AuthState.SignedIn(uid = uid, displayName = displayName, email = null, photoUrl = null)

    private val _state = MutableStateFlow<AuthState>(signedIn)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Signs straight back in. */
    override suspend fun signInWithGoogle(activity: Activity): Result<Unit> {
        _state.value = signedIn
        return Result.success(Unit)
    }

    override fun signOut() {
        _state.value = AuthState.SignedOut
    }

    override suspend fun deleteAccount(): Result<Unit> {
        signOut()
        return Result.success(Unit)
    }
}
