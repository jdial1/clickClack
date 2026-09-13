package com.ampersand.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** Whether the signed-in person has a partner yet. [Inviting] persists an outstanding code across screens. */
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

/** Why a code did not work, with a user-facing message. */
enum class RedeemError(val message: String) {
    UNKNOWN_CODE("We could not find that code. Check it and try again."),
    EXPIRED("That code has expired. Ask for a new one."),
    OWN_CODE("That is your own code. Send it to your partner."),
    ALREADY_PAIRED("You are already paired with someone."),
    THEY_ARE_PAIRED("That code's owner is already paired. Ask for a new one."),
    FAILED("Could not connect. Try again soon."),
}

/** How long an invite code lives. The security rules enforce the same ceiling. */
const val INVITE_TTL_MINUTES = 60L

class RedeemException(val reason: RedeemError) : Exception(reason.message)

interface PairingRepository {

    val state: StateFlow<PairState>

    /** Make a code for your partner to type in, or return the outstanding one. */
    suspend fun createInvite(): Result<String>

    suspend fun redeem(code: String): Result<Unit>

    /** Ends the pairing for both of you. */
    suspend fun unpair(): Result<Unit>
}

/** Firestore pairing when there is a Firebase config, in-memory otherwise. Same check as [createAuthRepository]. */
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
 * Invite codes: 8 chars from a 32-symbol alphabet without I, O, 0, 1 (2^40).
 * Rules let any signed-in user `get` an invite by id, so the code is the secret.
 */
internal object InviteCode {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 8

    fun next(random: Random = Random.Default): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    /** Uppercases and strips spaces, dashes and other non-alphabet characters. */
    fun normalise(input: String): String =
        input.uppercase().filter { it in ALPHABET }.take(LENGTH)
}

/** In-memory pairing for builds without Firebase. Starts paired with the mock couple. */
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
