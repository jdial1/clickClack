package com.ampersand.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real auth when there is a Firebase project behind it, the fake when there is
 * not.
 *
 * Detection is whether the build found an OAuth web client in
 * google-services.json. Dropping the file in and rebuilding is still the entire
 * setup step -- nothing to remember to flip -- but the answer is now a compiled
 * constant rather than a resource looked up by name, which the release build's
 * resource shrinker was free to remove.
 */
fun createAuthRepository(context: Context): AuthRepository =
    if (hasFirebaseConfigFor(context)) GoogleAuthRepository(context) else FakeAuthRepository()

fun hasFirebaseConfigFor(@Suppress("UNUSED_PARAMETER") context: Context): Boolean =
    com.ampersand.BuildConfig.WEB_CLIENT_ID.isNotBlank()

/**
 * The hosting Activity, which Credential Manager requires.
 *
 * `LocalContext.current` inside a composable is not reliably an Activity -- it
 * can be a ContextThemeWrapper -- so this unwraps rather than casting.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Who is using the app.
 *
 * Three states rather than a nullable user, because "we have not checked yet"
 * and "checked, nobody is signed in" have to look different on screen. Collapse
 * them and every cold start flashes the sign-in screen for a frame before
 * Firebase reports the cached session.
 */
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

/**
 * The seam Google sign-in slots into.
 *
 * Same shape as [com.ampersand.data.CoupleRepository] and for the same reason:
 * the UI is written against this, so the app can be built and designed with no
 * Firebase project in existence, and gains real auth by swapping the
 * implementation rather than by editing a screen.
 */
interface AuthRepository {

    val state: StateFlow<AuthState>

    /**
     * Sign in with Google.
     *
     * Takes an [Activity] because Credential Manager needs one to host its
     * bottom sheet -- it is not a `startActivityForResult` flow and there is no
     * way to run it from a bare context.
     *
     * Returns a [Result] rather than throwing: the overwhelmingly common
     * failure is the user dismissing the sheet, which is not exceptional and
     * should not surface as an error to them.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<Unit>

    fun signOut()
}

/**
 * Signed in as the mock user, always.
 *
 * Keeps the app exactly as it behaves today -- open it and you are Justin -- so
 * that adding the auth seam does not put a sign-in wall in front of every
 * screen that is still being designed. Swapped for [GoogleAuthRepository] when
 * a Firebase project exists.
 */
class FakeAuthRepository(
    private val uid: String = "u_justin",
    private val displayName: String = "Justin",
) : AuthRepository {

    private val signedIn =
        AuthState.SignedIn(uid = uid, displayName = displayName, email = null, photoUrl = null)

    private val _state = MutableStateFlow<AuthState>(signedIn)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Signs straight back in, so sign-out is a loop rather than a dead end. */
    override suspend fun signInWithGoogle(activity: Activity): Result<Unit> {
        _state.value = signedIn
        return Result.success(Unit)
    }

    override fun signOut() {
        _state.value = AuthState.SignedOut
    }
}
