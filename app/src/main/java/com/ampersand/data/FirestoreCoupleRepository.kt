package com.ampersand.data

import android.util.Log
import com.ampersand.auth.COUPLES
import com.ampersand.auth.USERS
import com.ampersand.model.Answer
import com.ampersand.model.Block
import com.ampersand.model.BlockFace
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Member
import com.ampersand.model.Placement
import com.ampersand.model.Question
import com.ampersand.model.Reaction
import com.ampersand.model.Rewards
import com.ampersand.model.Slope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

private const val TAG = "Couple"
private const val DAYS = "days"
private const val ANSWERS = "answers"
private const val PLATES = "plates"

/** Ten weeks, which is what the Shelf draws. */
private const val HistoryDays = 70L

/**
 * The real one.
 *
 * Everything a person creates lives in Firestore and comes back keyed to their
 * account: answers, reactions, colours, bricks and sealed sculptures. Two
 * phones signed into two profiles see each other's contributions, which is what
 * makes this testable at all.
 *
 * **Questions are still mock.** The pool arrives as JSON later, so today's
 * question is picked out of [MockQuestions] by date. Picking by date rather
 * than storing it matters more than it looks: both phones must land on the same
 * question without either having written it first, or whoever opens the app
 * second gets a different one.
 *
 * ```
 * couples/{cid}/days/{yyyy-mm-dd}   questionId, revealed, reactions{uid:name}
 *   .../answers/{uid}               values[], guess[], submittedAt
 * couples/{cid}/plates/{pid}        startedOn, periodDays, gridW, gridH,
 *                                   completedOn, blocks[], placed[]
 * ```
 *
 * Four listeners: both user documents, today, the last ten weeks, and the
 * plates. They each write into [_state] through [recompose], because a
 * [CoupleState] is a whole picture and updating it field by field from four
 * callbacks arriving in any order is how you get a screen that is briefly
 * wrong.
 */
class FirestoreCoupleRepository(
    private val coupleId: String,
    private val myUid: String,
    private val partnerUid: String,
    private val today: LocalDate = LocalDate.now(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CoupleRepository {

    private val _state = MutableStateFlow(
        // A believable empty picture, so the UI has something to draw before
        // the first snapshot lands rather than flashing a spinner.
        CoupleState(
            you = Member(myUid, "You", 0xFFF54E4F),
            them = Member(partnerUid, "Them", 0xFF108BE2),
            today = DayEntry(today, questionFor(today)),
            plate = emptyPlate(today),
        ),
    )
    override val state: StateFlow<CoupleState> = _state.asStateFlow()

    private val regs = mutableListOf<ListenerRegistration>()

    // Raw pieces, assembled by recompose(). Nullable so "not arrived yet" is
    // distinguishable from "arrived and empty".
    private var me: DocumentSnapshot? = null
    private var them: DocumentSnapshot? = null
    private var todayDoc: DocumentSnapshot? = null
    private var todayAnswers: Map<String, Answer> = emptyMap()
    private var history: List<DayEntry> = emptyList()
    private var plates: List<BuildPlate> = emptyList()

    private val couple get() = db.collection(COUPLES).document(coupleId)
    private val dayRef get() = couple.collection(DAYS).document(today.toString())

    init {
        regs += db.collection(USERS).document(myUid)
            .addSnapshotListener { d, _ -> me = d; recompose() }
        regs += db.collection(USERS).document(partnerUid)
            .addSnapshotListener { d, _ -> them = d; recompose() }

        regs += dayRef.addSnapshotListener { d, _ -> todayDoc = d; recompose() }

        // The answers subcollection, not the day document. A partner's answer
        // is unreadable until `revealed` is true, so this listener simply
        // returns one document until the seal lifts and then two -- the rules
        // do the hiding, and the app never has to.
        regs += dayRef.collection(ANSWERS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Expected before the reveal: the query is denied rather
                    // than filtered, so fall back to reading only our own.
                    readOwnAnswer()
                    return@addSnapshotListener
                }
                todayAnswers = snap?.documents.orEmpty().mapNotNull { it.toAnswer() }
                    .associateBy { it.uid }
                recompose()
            }

        regs += couple.collection(DAYS)
            .whereGreaterThanOrEqualTo(
                com.google.firebase.firestore.FieldPath.documentId(),
                today.minusDays(HistoryDays).toString(),
            )
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "history listener failed", error)
                    return@addSnapshotListener
                }
                history = snap?.documents.orEmpty()
                    .mapNotNull { it.toDayEntry() }
                    .filter { it.date != today }
                    .sortedByDescending { it.date }
                recompose()
            }

        regs += couple.collection(PLATES)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "plates listener failed", error)
                    return@addSnapshotListener
                }
                plates = snap?.documents.orEmpty().mapNotNull { it.toPlate() }
                recompose()
            }
    }

    /**
     * Assemble the whole picture from whatever has arrived.
     *
     * Safe to call from any listener at any time; missing pieces fall back to
     * their defaults rather than blanking the screen.
     */
    private fun recompose() {
        val current = _state.value

        val you = me.toMember(myUid, current.you)
        val partner = them.toMember(partnerUid, current.them)

        val question = todayDoc?.getString("questionId")
            ?.let { MockQuestions.pool.firstOrNull { q -> q.id == it } }
            ?: questionFor(today)

        val todayEntry = DayEntry(
            date = today,
            question = question,
            answers = todayAnswers,
            reactions = todayDoc.toReactions(),
        )

        val live = plates.filter { !it.isSealed }.maxByOrNull { it.startedOn }
            ?: emptyPlate(today)

        _state.value = current.copy(
            you = you,
            them = partner,
            today = todayEntry,
            history = history,
            plate = live,
            shelf = plates.filter { it.isSealed }.sortedByDescending { it.completedOn },
            streak = streakOf(history, todayEntry),
        )
    }

    /** Before the reveal the collection query is denied; our own doc is not. */
    private fun readOwnAnswer() {
        dayRef.collection(ANSWERS).document(myUid).get()
            .addOnSuccessListener { doc ->
                todayAnswers = listOfNotNull(doc.toAnswer()).associateBy { it.uid }
                recompose()
            }
    }

    // --- Writes --------------------------------------------------------------

    override fun bindLocalUser(uid: String, displayName: String) {
        // Firestore already knows who you are; this only keeps the name fresh
        // for the partner, who reads it off your user document.
        db.collection(USERS).document(myUid).set(
            mapOf("displayName" to displayName),
            SetOptions.merge(),
        )
    }

    override fun bindPartner(uid: String, displayName: String) = Unit

    override fun submitToday(values: List<String>, guess: List<String>?) {
        if (_state.value.today.answerFor(myUid) != null) return

        // The day document first, so the question is pinned before either
        // answer exists. Without it, a day answered either side of midnight in
        // two timezones could be two different questions.
        dayRef.set(
            mapOf(
                "questionId" to _state.value.today.question.id,
                "revealed" to (todayDoc?.getBoolean("revealed") ?: false),
            ),
            SetOptions.merge(),
        )

        dayRef.collection(ANSWERS).document(myUid).set(
            mapOf(
                "values" to values,
                "guess" to guess,
                "submittedAt" to FieldValue.serverTimestamp(),
            ),
        ).addOnSuccessListener { liftSealIfBothIn() }
    }

    /**
     * Flip `revealed` once both answers exist.
     *
     * Done by whichever client notices, and safe because the rules will not
     * accept `revealed = true` unless both answer documents are really there --
     * so a client cannot peek by writing it early. Properly this belongs in a
     * Cloud Function; the rule is what makes the client version honest.
     */
    private fun liftSealIfBothIn() {
        dayRef.collection(ANSWERS).get().addOnSuccessListener { snap ->
            if (snap.size() >= 2) {
                dayRef.set(mapOf("revealed" to true), SetOptions.merge())
                    .addOnSuccessListener { awardForToday() }
            }
        }
    }

    /**
     * Both answered, so both earn a brick.
     *
     * Idempotent through the block id, which is `date_uid`: running this twice
     * on the same day writes the same two entries. It has to be, because both
     * clients will call it the moment the seal lifts.
     */
    private fun awardForToday() {
        val s = _state.value
        val plate = s.plate
        val fresh = (
            Rewards.forDay(s.today, s.you, s.streak) +
                Rewards.forDay(s.today, s.them, s.streak)
            ).filter { b -> plate.blocks.none { it.id == b.id } }
        if (fresh.isEmpty()) return

        plateRef(plate).set(
            plate.toMap() + ("blocks" to (plate.blocks + fresh).map { it.toMap() }),
            SetOptions.merge(),
        )
    }

    override fun setColor(uid: String, colorArgb: Long) {
        // Only your own. A colour is part of a profile, and the rules would
        // refuse a write to the partner's document anyway.
        if (uid != myUid) return
        db.collection(USERS).document(myUid).set(
            mapOf("colorArgb" to colorArgb),
            SetOptions.merge(),
        )
    }

    override fun placeBlock(blockId: String, x: Int, y: Int) {
        val plate = _state.value.plate
        if (plate.isSealed) return
        val block = plate.block(blockId) ?: return
        if (plate.placed.any { it.blockId == blockId }) return
        if (!plate.canPlace(block.shape, x, y)) return

        val placed = plate.placed + Placement(blockId, x, y)
        plateRef(plate).set(
            plate.toMap() + ("placed" to placed.map { it.toMap() }),
            SetOptions.merge(),
        )
    }

    override fun react(date: LocalDate, reaction: Reaction?) {
        val ref = if (date == today) dayRef else couple.collection(DAYS).document(date.toString())
        ref.set(
            mapOf("reactions" to mapOf(myUid to reaction?.name)),
            SetOptions.merge(),
        )
    }

    override fun sealPlate() {
        val plate = _state.value.plate
        if (plate.isSealed || plate.placed.isEmpty()) return

        plateRef(plate).set(
            plate.toMap() + ("completedOn" to today.toString()),
            SetOptions.merge(),
        )
        // Unplaced bricks carry over. They were earned by answering.
        val carried = plate.tray(myUid) + plate.tray(partnerUid)
        val next = emptyPlate(today).copy(blocks = carried)
        couple.collection(PLATES).document().set(next.toMap())
    }

    // --- Scaffolding ---------------------------------------------------------
    // Deliberately no-ops against the real backend: there is no honest way to
    // fake your partner from your own phone once the rules only let you write
    // your own documents. Use a second account.

    override fun simulatePartnerAnswer() = Unit
    override fun simulatePartnerReaction(date: LocalDate) = Unit
    override fun previewQuestion(question: Question) = Unit
    override fun resetToday() = Unit

    fun close() {
        regs.forEach { it.remove() }
        regs.clear()
    }

    private fun plateRef(plate: BuildPlate) = couple.collection(PLATES).document(plate.id)

    private fun emptyPlate(from: LocalDate) = BuildPlate(
        id = "plate_${from}",
        startedOn = from,
        periodDays = MockCouple.PeriodDays,
        gridW = 12,
        gridH = 14,
    )
}

// --- Mapping -----------------------------------------------------------------

/**
 * Today's question, chosen by date.
 *
 * Deterministic so both phones land on the same one before either has written
 * anything. Replaced when the real pool arrives; the scheduling problem it
 * solves does not go away.
 */
private fun questionFor(date: LocalDate): Question {
    val pool = MockQuestions.pool
    return pool[(date.toEpochDay().mod(pool.size))]
}

private fun DocumentSnapshot?.toMember(uid: String, fallback: Member): Member = Member(
    uid = uid,
    name = this?.getString("displayName")?.takeIf { it.isNotBlank() } ?: fallback.name,
    colorArgb = this?.getLong("colorArgb") ?: fallback.colorArgb,
)

private fun DocumentSnapshot?.toReactions(): Map<String, Reaction> {
    val raw = this?.get("reactions") as? Map<*, *> ?: return emptyMap()
    return raw.entries.mapNotNull { (k, v) ->
        val uid = k as? String ?: return@mapNotNull null
        val name = v as? String ?: return@mapNotNull null
        Reaction.entries.firstOrNull { it.name == name }?.let { uid to it }
    }.toMap()
}

private fun DocumentSnapshot.toAnswer(): Answer? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val values = get("values") as? List<String> ?: return null
    @Suppress("UNCHECKED_CAST")
    val guess = get("guess") as? List<String>
    return Answer(uid = id, values = values, guess = guess)
}

private fun DocumentSnapshot.toDayEntry(): DayEntry? {
    val date = runCatching { LocalDate.parse(id) }.getOrNull() ?: return null
    val question = MockQuestions.pool.firstOrNull { it.id == getString("questionId") }
        ?: questionFor(date)
    // History carries no answers: they live in a subcollection that would need
    // a read each. The Shelf shows questions, dates and reactions, so this is
    // what it needs; the Reveal screen loads a single day in full.
    return DayEntry(date = date, question = question, reactions = toReactions())
}

private fun DocumentSnapshot.toPlate(): BuildPlate? {
    val started = runCatching { LocalDate.parse(getString("startedOn")) }.getOrNull() ?: return null
    @Suppress("UNCHECKED_CAST")
    val blocks = (get("blocks") as? List<Map<String, Any?>>).orEmpty().mapNotNull { it.toBlock() }
    @Suppress("UNCHECKED_CAST")
    val placed = (get("placed") as? List<Map<String, Any?>>).orEmpty().mapNotNull { it.toPlacement() }
    return BuildPlate(
        id = id,
        startedOn = started,
        periodDays = (getLong("periodDays") ?: 7L).toInt(),
        gridW = (getLong("gridW") ?: 12L).toInt(),
        gridH = (getLong("gridH") ?: 14L).toInt(),
        blocks = blocks,
        placed = placed,
        completedOn = getString("completedOn")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    )
}

private fun Map<String, Any?>.toBlock(): Block? {
    val id = this["id"] as? String ?: return null
    val owner = this["ownerUid"] as? String ?: return null
    val shape = BrickShape(
        studs = (this["studs"] as? Number)?.toInt() ?: return null,
        height = BrickHeight.entries.firstOrNull { it.name == this["height"] } ?: BrickHeight.LOW,
        slope = Slope.entries.firstOrNull { it.name == this["slope"] } ?: Slope.NONE,
    )
    return Block(
        id = id,
        shape = shape,
        colorArgb = (this["colorArgb"] as? Number)?.toLong() ?: 0xFFF54E4F,
        ownerUid = owner,
        earnedOn = runCatching { LocalDate.parse(this["earnedOn"] as? String) }.getOrNull()
            ?: LocalDate.now(),
        face = BlockFace.entries.firstOrNull { it.name == this["face"] },
    )
}

private fun Map<String, Any?>.toPlacement(): Placement? {
    val id = this["blockId"] as? String ?: return null
    return Placement(
        blockId = id,
        x = (this["x"] as? Number)?.toInt() ?: return null,
        y = (this["y"] as? Number)?.toInt() ?: return null,
    )
}

private fun Block.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "studs" to shape.studs,
    "height" to shape.height.name,
    "slope" to shape.slope.name,
    "colorArgb" to colorArgb,
    "ownerUid" to ownerUid,
    "earnedOn" to earnedOn.toString(),
    "face" to face?.name,
)

private fun Placement.toMap(): Map<String, Any?> = mapOf("blockId" to blockId, "x" to x, "y" to y)

private fun BuildPlate.toMap(): Map<String, Any?> = mapOf(
    "startedOn" to startedOn.toString(),
    "periodDays" to periodDays,
    "gridW" to gridW,
    "gridH" to gridH,
    "completedOn" to completedOn?.toString(),
)

/**
 * Consecutive answered days ending today, or yesterday.
 *
 * Yesterday counts so that the streak does not read as broken all morning
 * before you have answered.
 */
private fun streakOf(history: List<DayEntry>, today: DayEntry): Int {
    val answered = history.filter { it.answers.isNotEmpty() || it.reactions.isNotEmpty() }
        .mapTo(mutableSetOf()) { it.date }
    if (today.answers.isNotEmpty()) answered += today.date

    var day = if (today.date in answered) today.date else today.date.minusDays(1)
    var count = 0
    while (day in answered) {
        count++
        day = day.minusDays(1)
    }
    return count
}
