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
import kotlin.coroutines.resumeWithException

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
 * Listens to both the user doc (which couple) and the couple doc (whether it ended),
 * so an unpair by the partner is noticed on this phone.
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
                // Expired codes are not shown.
                expires != null && expires <= System.currentTimeMillis() -> PairState.Solo
                else -> PairState.Inviting(code, expires)
            }
            return
        }
        watchCouple(coupleId)
    }

    /** Live, so this phone notices when the partner ends the couple. */
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
                    // Partner unpaired. Clear our own coupleId too.
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
                            partnerName = partner.getString("displayName") ?: "Partner",
                        )
                    }
                    .addOnFailureListener {
                        _state.value = PairState.Paired(coupleId, partnerUid, "Partner")
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
            // Reuse an unexpired code; otherwise make a new one.
            if (ms == null || ms > System.currentTimeMillis()) return Result.success(live.code)
        }

        val code = InviteCode.next()
        val expiresAt = Timestamp(
            Date(System.currentTimeMillis() + INVITE_TTL_MINUTES * 60_000L),
        )

        return runCatching {
            // Write the invite first: an orphan invite is harmless, an unwritten code is not.
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

    /** Join the couple behind [code] in one transaction, so concurrent redeems cannot create two couples. */
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
                        // Delete expired codes. Returned, not thrown: throwing aborts the transaction and the delete.
                        txn.delete(inviteRef)
                        return@runTransaction RedeemError.EXPIRED
                    }

                    val fromUid = invite.getString("fromUid")
                        ?: throw RedeemException(RedeemError.UNKNOWN_CODE)
                    if (fromUid == uid) throw RedeemException(RedeemError.OWN_CODE)

                    // The code's owner may have paired with someone else since.
                    if (txn.get(db.collection(USERS).document(fromUid)).getString("coupleId") != null) {
                        throw RedeemException(RedeemError.THEY_ARE_PAIRED)
                    }

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
                    // One use per invite.
                    txn.delete(inviteRef)
                    null
                },
            )?.let { throw RedeemException(it) }
        }.map { }.recoverCatching { e ->
            val reason = e as? RedeemException ?: e.cause as? RedeemException
            Log.w(TAG, "redeem failed", e)
            throw reason ?: RedeemException(RedeemError.FAILED)
        }
    }

    /**
     * End the couple for both of you.
     *
     * Sets `endedAt` on the couple doc (both members may write it), which the partner's
     * listener picks up; rules only let you clear your own `coupleId`. The couple doc is
     * kept, since deleting it would orphan its subcollections.
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

    /**
     * Awaits a Play Services Task.
     * Uses [resumeWithException], not `cancel`, so the original [RedeemException] reaches the caller.
     */
    private suspend fun <T> await(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
