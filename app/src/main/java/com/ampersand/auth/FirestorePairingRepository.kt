package com.ampersand.auth

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import kotlin.coroutines.resume

private const val TAG = "Pairing"

internal const val USERS = "users"
internal const val INVITES = "invites"
internal const val COUPLES = "couples"

/**
 * Pairing in Firestore.
 *
 * ```
 * users/{uid}      displayName, colorArgb, coupleId, inviteCode
 * invites/{code}   fromUid, fromName, createdAt, expiresAt
 * couples/{cid}    members[2], createdAt, endedAt
 * ```
 *
 * Two listeners, not one. The user document says which couple you are in; the
 * couple document says whether it is still going. Watching only the first is
 * what made unpairing one-sided -- the person who did not tap it kept a
 * `coupleId` pointing at a couple that had been abandoned, and nothing on their
 * phone ever told them.
 */
class FirestorePairingRepository(
    private val uid: String,
    private val displayName: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PairingRepository {

    private val _state = MutableStateFlow<PairState>(PairState.Loading)
    override val state: StateFlow<PairState> = _state.asStateFlow()

    private var userReg: ListenerRegistration? = null
    private var coupleReg: ListenerRegistration? = null
    private var watching: String? = null

    init {
        userReg = db.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "user listener failed", error)
                    return@addSnapshotListener
                }
                onUserChanged(snapshot)
            }
    }

    private fun onUserChanged(snapshot: DocumentSnapshot?) {
        val coupleId = snapshot?.getString("coupleId")
        val code = snapshot?.getString("inviteCode")
        val expires = snapshot?.getTimestamp("inviteExpiresAt")?.toDate()?.time

        if (coupleId.isNullOrBlank()) {
            stopWatchingCouple()
            _state.value = when {
                code.isNullOrBlank() -> PairState.Solo
                // A code that has run out is not an invitation any more, and
                // leaving it on screen invites someone to read out a dead one.
                expires != null && expires <= System.currentTimeMillis() -> PairState.Solo
                else -> PairState.Inviting(code, expires)
            }
            return
        }
        watchCouple(coupleId)
    }

    /** Live, because the other person can end it and this phone must notice. */
    private fun watchCouple(coupleId: String) {
        if (watching == coupleId) return
        stopWatchingCouple()
        watching = coupleId

        coupleReg = db.collection(COUPLES).document(coupleId)
            .addSnapshotListener { couple, error ->
                if (error != null || couple == null || !couple.exists()) {
                    _state.value = PairState.Solo
                    return@addSnapshotListener
                }
                if (couple.getTimestamp("endedAt") != null) {
                    // They unpaired. Tidy our own document so a reinstall does
                    // not come back pointing at a finished couple.
                    db.collection(USERS).document(uid)
                        .set(mapOf("coupleId" to null), SetOptions.merge())
                    _state.value = PairState.Solo
                    return@addSnapshotListener
                }

                @Suppress("UNCHECKED_CAST")
                val members = couple.get("members") as? List<String> ?: emptyList()
                val partnerUid = members.firstOrNull { it != uid }
                if (partnerUid == null) {
                    _state.value = PairState.Solo
                    return@addSnapshotListener
                }
                db.collection(USERS).document(partnerUid).get()
                    .addOnSuccessListener { partner ->
                        _state.value = PairState.Paired(
                            coupleId = coupleId,
                            partnerUid = partnerUid,
                            partnerName = partner.getString("displayName") ?: "Them",
                        )
                    }
                    .addOnFailureListener {
                        _state.value = PairState.Paired(coupleId, partnerUid, "Them")
                    }
            }
    }

    private fun stopWatchingCouple() {
        coupleReg?.remove()
        coupleReg = null
        watching = null
    }

    override suspend fun createInvite(): Result<String> {
        (_state.value as? PairState.Inviting)?.let { live ->
            val ms = live.expiresAtMillis
            // Reuse a code that is still good; mint a new one if it has run
            // out, rather than handing back something already dead.
            if (ms == null || ms > System.currentTimeMillis()) return Result.success(live.code)
        }

        val code = InviteCode.next()
        val expiresAt = Timestamp(
            Date(System.currentTimeMillis() + INVITE_TTL_MINUTES * 60_000L),
        )

        return runCatching {
            // The invite first. An orphan invite is harmless; a code shown on
            // screen that was never written is not.
            await(
                db.collection(INVITES).document(code).set(
                    mapOf(
                        "fromUid" to uid,
                        "fromName" to displayName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "expiresAt" to expiresAt,
                    ),
                ),
            )
            await(
                db.collection(USERS).document(uid).set(
                    mapOf(
                        "displayName" to displayName,
                        "inviteCode" to code,
                        "inviteExpiresAt" to expiresAt,
                    ),
                    SetOptions.merge(),
                ),
            )
            code
        }
    }

    /**
     * Join the couple behind [code], in one transaction.
     *
     * Two people can be tapping at the same moment. Two redemptions of one code
     * racing outside a transaction is how you get two couples, each holding one
     * person, both users pointing at whichever write landed last.
     */
    override suspend fun redeem(code: String): Result<Unit> {
        val clean = InviteCode.normalise(code)
        if (clean.length < 8) return Result.failure(RedeemException(RedeemError.UNKNOWN_CODE))
        if (_state.value is PairState.Paired) {
            return Result.failure(RedeemException(RedeemError.ALREADY_PAIRED))
        }

        val inviteRef = db.collection(INVITES).document(clean)
        val coupleRef = db.collection(COUPLES).document()

        return runCatching {
            await(
                db.runTransaction { txn ->
                    val invite = txn.get(inviteRef)
                    if (!invite.exists()) throw RedeemException(RedeemError.UNKNOWN_CODE)

                    val expiresAt = invite.getTimestamp("expiresAt")
                    if (expiresAt == null || expiresAt.toDate().time <= System.currentTimeMillis()) {
                        // Expired codes are consumed rather than left lying
                        // around: the only thing a dead invite can do now is
                        // confuse whoever types it next.
                        txn.delete(inviteRef)
                        throw RedeemException(RedeemError.EXPIRED)
                    }

                    val fromUid = invite.getString("fromUid")
                        ?: throw RedeemException(RedeemError.UNKNOWN_CODE)
                    if (fromUid == uid) throw RedeemException(RedeemError.OWN_CODE)

                    txn.set(
                        coupleRef,
                        mapOf(
                            "members" to listOf(fromUid, uid),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "endedAt" to null,
                        ),
                    )
                    txn.set(
                        db.collection(USERS).document(uid),
                        mapOf(
                            "displayName" to displayName,
                            "coupleId" to coupleRef.id,
                            "inviteCode" to null,
                            "inviteExpiresAt" to null,
                        ),
                        SetOptions.merge(),
                    )
                    txn.set(
                        db.collection(USERS).document(fromUid),
                        mapOf(
                            "coupleId" to coupleRef.id,
                            "inviteCode" to null,
                            "inviteExpiresAt" to null,
                        ),
                        SetOptions.merge(),
                    )
                    // Consumed on a valid match. An invite is for one person.
                    txn.delete(inviteRef)
                    null
                },
            )
        }.map { }.recoverCatching { e ->
            throw if (e is RedeemException) e else RedeemException(RedeemError.FAILED)
        }
    }

    /**
     * End the couple for both of you.
     *
     * Writes `endedAt` on the **couple**, which both members may update, rather
     * than clearing your own `coupleId`, which only you may write. That is the
     * whole fix: the partner's listener sees the shared document change and
     * their app leaves too. Clearing your own pointer afterwards is tidying, not
     * the mechanism.
     *
     * The couple document is kept rather than deleted. It is the parent of every
     * answer and sculpture the two of you made, and deleting a parent in
     * Firestore orphans the subcollections rather than removing them.
     */
    override suspend fun unpair(): Result<Unit> {
        val coupleId = (_state.value as? PairState.Paired)?.coupleId
            ?: return Result.success(Unit)

        return runCatching {
            await(
                db.collection(COUPLES).document(coupleId).set(
                    mapOf("endedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge(),
                ),
            )
            await(
                db.collection(USERS).document(uid).set(
                    mapOf("coupleId" to null),
                    SetOptions.merge(),
                ),
            )
        }.map { }
    }

    fun close() {
        userReg?.remove()
        userReg = null
        stopWatchingCouple()
    }

    /** One place to bridge Play Services Tasks, instead of a whole dependency. */
    private suspend fun <T> await(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.cancel(it) }
        }
}
