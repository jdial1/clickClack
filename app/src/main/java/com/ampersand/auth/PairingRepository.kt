package com.ampersand.auth

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.ampersand.model.SculptureNames
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
/**
 * Join codes built from the sculpture-name words: a describing word, a thing
 * and three digits, like WOBBLYCASTLE427, shown as "Wobbly Castle 427".
 * About 1.6 million codes, each live for an hour.
 */
internal object InviteCode {

    fun next(random: Random = Random.Default): String {
        val describing = SculptureNames.Describing[random.nextInt(SculptureNames.Describing.size)]
        val thing = SculptureNames.Things[random.nextInt(SculptureNames.Things.size)]
        return "$describing$thing${random.nextInt(100, 1000)}".uppercase()
    }

    /** Uppercases and keeps only letters and digits, so "wobbly castle 427" and "Wobbly-Castle-427" both work. */
    fun normalise(input: String): String = input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    /** "WOBBLYCASTLE427" as "Wobbly Castle 427"; anything else is returned as is. */
    fun display(code: String): String {
        val word = SculptureNames.Describing.firstOrNull { code.startsWith(it.uppercase()) } ?: return code
        val rest = code.removePrefix(word.uppercase())
        val thing = SculptureNames.Things.firstOrNull { rest.startsWith(it.uppercase()) } ?: return code
        return "$word $thing ${rest.removePrefix(thing.uppercase())}"
    }

    /** A link that opens the app with the code filled in. */
    fun link(code: String): String = "clickclack://join/$code"

    /** The code from a [link], or null. */
    fun fromLink(uri: String?): String? =
        uri?.takeIf { it.startsWith("clickclack://join/") }?.removePrefix("clickclack://join/")?.let(::normalise)?.takeIf { it.isNotEmpty() }
}

/** A join code handed to the app by a link, waiting for the pairing screen. */
object PendingJoin {
    var code by androidx.compose.runtime.mutableStateOf<String?>(null)
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
