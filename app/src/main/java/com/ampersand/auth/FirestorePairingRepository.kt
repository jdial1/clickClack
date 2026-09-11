package com.ampersand.auth

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "Pairing"

private const val USERS = "users"
private const val INVITES = "invites"
private const val COUPLES = "couples"

/**
 * Pairing in Firestore.
 *
 * The document layout is the one the security rules are written against:
 *
 * ```
 * users/{uid}      displayName, coupleId, inviteCode
 * invites/{code}   fromUid, fromName, createdAt
 * couples/{cid}    members[2], createdAt
 * ```
 *
 * The local user document is the source of truth for "am I paired", and it is
 * watched rather than fetched. That is what makes pairing land on *both*
 * phones: the person who made the code never touches their own document again,
 * so without a listener their app would sit on the invite screen until it was
 * restarted.
 */
class FirestorePairingRepository(
    private val uid: String,
    private val displayName: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PairingRepository {

    private val _state = MutableStateFlow<PairState>(PairState.Loading)
    override val state: StateFlow<PairState> = _state.asStateFlow()

    private var registration: ListenerRegistration? = null

    init {
        registration = db.collection(USERS).document(uid)
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

        if (coupleId.isNullOrBlank()) {
            _state.value = if (code.isNullOrBlank()) PairState.Solo else PairState.Inviting(code)
            return
        }
        loadCouple(coupleId)
    }

    private fun loadCouple(coupleId: String) {
        db.collection(COUPLES).document(coupleId).get()
            .addOnSuccessListener { couple ->
                @Suppress("UNCHECKED_CAST")
                val members = couple.get("members") as? List<String> ?: emptyList()
                val partnerUid = members.firstOrNull { it != uid }
                if (partnerUid == null) {
                    _state.value = PairState.Solo
                    return@addOnSuccessListener
                }
                // One extra read for a name we could have denormalised onto the
                // couple. Kept separate so a partner renaming themselves is not
                // a write to a document the rules make immutable.
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
            .addOnFailureListener {
                Log.w(TAG, "couple load failed", it)
                _state.value = PairState.Solo
            }
    }

    override suspend fun createInvite(): Result<String> {
        (_state.value as? PairState.Inviting)?.let { return Result.success(it.code) }

        val code = InviteCode.next()
        return runCatching {
            // The invite first. If writing the code onto the user document
            // fails, an orphan invite is harmless; the other way round leaves a
            // code on screen that nobody can redeem.
            await(
                db.collection(INVITES).document(code).set(
                    mapOf(
                        "fromUid" to uid,
                        "fromName" to displayName,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ),
            )
            await(
                db.collection(USERS).document(uid).set(
                    mapOf("displayName" to displayName, "inviteCode" to code),
                    com.google.firebase.firestore.SetOptions.merge(),
                ),
            )
            code
        }
    }

    /**
     * Join the couple behind [code], in one transaction.
     *
     * A transaction rather than three writes because both people can be tapping
     * at the same moment. Two redemptions of the same code racing outside a
     * transaction is how you get two couples, each holding one person, and both
     * users pointing at whichever write landed last.
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

                    val fromUid = invite.getString("fromUid")
                        ?: throw RedeemException(RedeemError.UNKNOWN_CODE)
                    if (fromUid == uid) throw RedeemException(RedeemError.OWN_CODE)

                    txn.set(
                        coupleRef,
                        mapOf(
                            "members" to listOf(fromUid, uid),
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                    txn.set(
                        db.collection(USERS).document(uid),
                        mapOf(
                            "displayName" to displayName,
                            "coupleId" to coupleRef.id,
                            "inviteCode" to null,
                        ),
                        com.google.firebase.firestore.SetOptions.merge(),
                    )
                    txn.set(
                        db.collection(USERS).document(fromUid),
                        mapOf("coupleId" to coupleRef.id, "inviteCode" to null),
                        com.google.firebase.firestore.SetOptions.merge(),
                    )
                    // Consumed. An invite is for one person.
                    txn.delete(inviteRef)
                    null
                },
            )
            Unit
        }.recoverCatching { e ->
            throw if (e is RedeemException) e else RedeemException(RedeemError.FAILED)
        }
    }

    override suspend fun unpair(): Result<Unit> = runCatching {
        await(
            db.collection(USERS).document(uid).set(
                mapOf("coupleId" to null),
                com.google.firebase.firestore.SetOptions.merge(),
            ),
        )
        // The partner's document is deliberately left alone: the rules only let
        // you write your own, and a one-sided unpair is the honest outcome of a
        // model where nobody can edit anyone else.
        Unit
    }

    fun close() {
        registration?.remove()
        registration = null
    }

    /** One place to bridge Play Services Tasks, instead of a whole dependency. */
    private suspend fun <T> await(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.cancel(it) }
        }
}
