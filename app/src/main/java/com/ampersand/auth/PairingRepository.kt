package com.ampersand.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Whether the signed-in person has a partner yet.
 *
 * [Inviting] is a distinct state rather than a flag on [Solo] because an
 * outstanding code has to survive leaving the screen. Someone reads the code
 * out, closes the app, and comes back -- if the code were regenerated each time
 * the screen opened, the one they already sent would be dead.
 */
sealed interface PairState {

    data object Loading : PairState

    /** Signed in, no partner, no outstanding invite. */
    data object Solo : PairState

    /** [expiresAtMillis] is null only in the fake, which does not time out. */
    data class Inviting(val code: String, val expiresAtMillis: Long? = null) : PairState

    data class Paired(
        val coupleId: String,
        val partnerUid: String,
        val partnerName: String,
    ) : PairState
}

/** Why a code did not work, in terms a person can act on. */
enum class RedeemError(val message: String) {
    UNKNOWN_CODE("No invite with that code. Check it and try again."),
    EXPIRED("That code has expired. Ask them for a fresh one."),
    OWN_CODE("That is your own code. Send it to them instead."),
    ALREADY_PAIRED("You are already paired with someone."),
    FAILED("Could not join. Try again in a moment."),
}

/**
 * How long a code lives.
 *
 * An hour, because the realistic use is reading it out to the person next to
 * you or sending it while you are both looking at your phones. A code with no
 * expiry is a code that leaks: it sits in a message thread and still works
 * months later, and anyone who scrolls back can pair with you.
 *
 * The ceiling is enforced in the security rules as well, so a client cannot
 * mint a long-lived one.
 */
const val INVITE_TTL_MINUTES = 60L

class RedeemException(val reason: RedeemError) : Exception(reason.message)

interface PairingRepository {

    val state: StateFlow<PairState>

    /**
     * Make a code for your partner to type in.
     *
     * Returns the existing one if there already is an outstanding invite: two
     * live codes for one person is a way to have the wrong one written on a
     * piece of paper.
     */
    suspend fun createInvite(): Result<String>

    suspend fun redeem(code: String): Result<Unit>

    /** Ends the pairing for both of you. */
    suspend fun unpair(): Result<Unit>
}

/**
 * Firestore pairing when there is a project behind it, in-memory when not.
 *
 * Same test as [createAuthRepository], so the two never disagree about which
 * world the app is running in.
 */
fun createPairingRepository(
    context: Context,
    uid: String,
    displayName: String,
): PairingRepository =
    if (hasFirebaseConfigFor(context)) {
        FirestorePairingRepository(uid, displayName)
    } else {
        FakePairingRepository(uid)
    }

/**
 * Codes people read out loud.
 *
 * No I, O, 0, 1 -- they are the pairs that get misheard and mistyped, and a
 * code is worthless if it cannot survive being said down a phone. Eight
 * characters from a 32-symbol alphabet is 2^40, which matters because the
 * security rules deliberately allow any signed-in user to `get` an invite by
 * id: the code is the secret, so it has to be one.
 */
internal object InviteCode {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 8

    fun next(random: Random = Random.Default): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    /** Accepts what a person actually types: spaces, dashes, lower case. */
    fun normalise(input: String): String =
        input.uppercase().filter { it in ALPHABET }.take(LENGTH)
}

/**
 * In-memory pairing, for designing the screens.
 *
 * Starts **paired**, with the couple the mock data describes. The fake exists
 * so the whole product is reachable with no backend, and starting solo would
 * put a pairing wall in front of every screen that is still being designed.
 * The scaffold panel has an unpair chip for reaching this screen on purpose.
 */
class FakePairingRepository(
    private val selfUid: String = "u_justin",
) : PairingRepository {

    private val _state = MutableStateFlow<PairState>(
        PairState.Paired(coupleId = "c_fake", partnerUid = "u_maya", partnerName = "Maya"),
    )
    override val state: StateFlow<PairState> = _state.asStateFlow()

    override suspend fun createInvite(): Result<String> {
        val current = _state.value
        if (current is PairState.Inviting) return Result.success(current.code)
        val code = InviteCode.next()
        _state.value = PairState.Inviting(code)
        return Result.success(code)
    }

    override suspend fun redeem(code: String): Result<Unit> {
        val clean = InviteCode.normalise(code)
        if (clean.length < 8) return Result.failure(RedeemException(RedeemError.UNKNOWN_CODE))
        if (_state.value is PairState.Paired) {
            return Result.failure(RedeemException(RedeemError.ALREADY_PAIRED))
        }
        _state.value = PairState.Paired(
            coupleId = "c_fake",
            partnerUid = "u_maya",
            partnerName = "Maya",
        )
        return Result.success(Unit)
    }

    override suspend fun unpair(): Result<Unit> {
        _state.value = PairState.Solo
        return Result.success(Unit)
    }
}
