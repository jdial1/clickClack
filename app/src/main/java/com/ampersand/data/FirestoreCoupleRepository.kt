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
import com.ampersand.model.QuizResult
import com.ampersand.model.QuizRun
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
private const val QUIZZES = "quizzes"
private const val RUNS = "runs"

/** Ten weeks, which is what the Shelf draws. */
private const val HistoryDays = 70L

/** A sculpture is a week. Short enough that you finish one and start again. */
private const val PeriodDays = 7

/**
 * The real one.
 *
 * Everything a person creates lives in Firestore and comes back keyed to their
 * account: answers, reactions, colours, bricks and sealed sculptures. Two
 * phones signed into two profiles see each other's contributions, which is what
 * makes this testable at all.
 *
 * Today's question comes from the bundled [QuestionPool], picked by date.
 * Picking rather than storing matters more than it looks: both phones must land
 * on the same question before either has written anything, or whoever opens the
 * app second gets a different one.
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
    private val pool: QuestionPool,
    private val today: LocalDate = LocalDate.now(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CoupleRepository {

    private val _state = MutableStateFlow(
        // A believable empty picture, so the UI has something to draw before
        // the first snapshot lands rather than flashing a spinner.
        CoupleState(
            you = Member(myUid, "You", 0xFFF54E4F),
            them = Member(partnerUid, "Them", 0xFF108BE2),
            today = DayEntry(today, pool.daily(today)),
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
    private var anniversary: LocalDate? = null

    private val couple get() = db.collection(COUPLES).document(coupleId)
    private val dayRef get() = couple.collection(DAYS).document(today.toString())

    init {
        regs += db.collection(USERS).document(myUid)
            .addSnapshotListener { d, _ -> me = d; recompose() }
        regs += db.collection(USERS).document(partnerUid)
            .addSnapshotListener { d, _ -> them = d; recompose() }

        regs += couple.addSnapshotListener { d, _ ->
            anniversary = d?.getString("anniversary")
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            recompose()
        }

        regs += dayRef.addSnapshotListener { d, _ ->
            todayDoc = d
            // Only once the seal is off is the other answer readable at all,
            // so that is the first moment the listener for it can exist.
            if (d?.getBoolean("revealed") == true) watchPartnerAnswer()
            liftSealIfBothIn()
            recompose()
        }

        // Your own answer document by name -- never the collection.
        //
        // A query over the answers is refused outright while the day is sealed,
        // even when the only document in it is your own. The rule turns on
        // *which* document is being asked for, and Firestore will not run a
        // query whose safety it cannot establish before looking at the data. So
        // the listener died on its very first snapshot and never came back:
        // answering wrote the document successfully, and then nothing on the
        // screen moved, with no error anywhere to say why.
        regs += dayRef.collection(ANSWERS).document(myUid)
            .addSnapshotListener { d, _ -> putAnswer(myUid, d) }

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
                    .mapNotNull { it.toDayEntry(pool) }
                    .filter { it.date != today }
                    .sortedByDescending { it.date }
                recompose()
            }

        // Every run either of you has written. Before a deck is revealed the
        // rules deny the partner's document, so this returns one run per deck;
        // afterwards it returns two. Nothing in the UI has to know the
        // difference -- a deck with one run is simply still waiting.
        regs += couple.collection(QUIZZES)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "quiz listener failed", error)
                    return@addSnapshotListener
                }
                snap?.documents.orEmpty().forEach { quiz ->
                    quizDocs = quizDocs + (quiz.id to quiz)
                    watchRuns(quiz.id)
                    if (quiz.getBoolean("revealed") == true) watchPartnerRun(quiz.id)
                    liftQuizSealIfBothIn(quiz.id)
                }
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

        val question = pool.question(todayDoc?.getString("questionId"))
            ?: pool.daily(today, anniversary)

        val todayEntry = DayEntry(
            date = today,
            question = question,
            answers = todayAnswers,
            reactions = todayDoc.toReactions(),
        )

        val live = plates.filter { !it.isSealed }.maxByOrNull { it.startedOn }
            ?: emptyPlate(today)

        // The period is over, so the sculpture is dated. Driven by the calendar
        // rather than by a button: a week ends whether or not anyone opened the
        // app that day.
        if (live.isSealed.not() && live.placed.isNotEmpty() && live.endsOn() < today) {
            sealPlate()
        }

        val next = current.copy(
            you = you,
            them = partner,
            today = todayEntry,
            history = history,
            plate = live,
            shelf = plates.filter { it.isSealed }.sortedByDescending { it.completedOn },
            quizRuns = runs.values.flatMap { it.values },
            anniversary = anniversary,
            streak = streakOf(history, todayEntry),
        )

        // The assignment. Without it every listener in this class computed the
        // right answer and dropped it on the floor: the screen kept the
        // placeholder the constructor built and nothing anyone did ever showed
        // up, with no error to explain it.
        _state.value = next

        // Earning is done here rather than in the write that caused it, because
        // a day or a deck opens when the *other* person submits -- which this
        // phone learns from a listener, not from anything it did.
        if (todayDoc?.getBoolean("revealed") == true) awardForToday()
        awardForQuizzes(next)
    }

    private val runRegs = mutableMapOf<String, ListenerRegistration>()
    private val partnerRunRegs = mutableMapOf<String, ListenerRegistration>()

    /** quiz id -> uid -> run, so one listener per person cannot clobber the other. */
    private var runs: Map<String, Map<String, QuizRun>> = emptyMap()

    /** The deck documents themselves, so a run landing can ask to open one. */
    private var quizDocs: Map<String, DocumentSnapshot> = emptyMap()

    /** Your own run, by name. A query over the runs is denied while sealed. */
    private fun watchRuns(quizId: String) {
        if (quizId in runRegs) return
        runRegs[quizId] = runRef(quizId, myUid)
            .addSnapshotListener { d, _ -> putRun(quizId, myUid, d) }
    }

    /** Theirs, once the deck has opened and there is anything to read. */
    private fun watchPartnerRun(quizId: String) {
        if (quizId in partnerRunRegs) return
        partnerRunRegs[quizId] = runRef(quizId, partnerUid)
            .addSnapshotListener { d, _ -> putRun(quizId, partnerUid, d) }
    }

    private fun runRef(quizId: String, uid: String) =
        couple.collection(QUIZZES).document(quizId).collection(RUNS).document(uid)

    private fun putRun(quizId: String, uid: String, doc: DocumentSnapshot?) {
        val run = doc?.toQuizRun(quizId)
        val forQuiz = runs[quizId].orEmpty() - uid +
            listOfNotNull(run).associateBy { it.uid }
        runs = runs + (quizId to forQuiz)
        liftQuizSealIfBothIn(quizId)
        recompose()
    }

    /** The deck's own version of [liftSealIfBothIn], asked for the same way. */
    private fun liftQuizSealIfBothIn(quizId: String) {
        val quiz = quizDocs[quizId] ?: return
        if (quiz.getBoolean("revealed") == true) return
        if (runs[quizId]?.get(myUid) == null) return

        couple.collection(QUIZZES).document(quizId)
            .set(mapOf("revealed" to true), SetOptions.merge())
            .addOnFailureListener { Log.d(TAG, "deck not ready to open yet", it) }
    }

    private var partnerAnswerReg: ListenerRegistration? = null

    private fun watchPartnerAnswer() {
        if (partnerAnswerReg != null) return
        partnerAnswerReg = dayRef.collection(ANSWERS).document(partnerUid)
            .addSnapshotListener { d, _ -> putAnswer(partnerUid, d) }
            .also { regs += it }
    }

    private fun putAnswer(uid: String, doc: DocumentSnapshot?) {
        val answer = doc?.toAnswer()
        todayAnswers = todayAnswers - uid + listOfNotNull(answer).associateBy { it.uid }
        // The second of the two moments a day can become ready to open. Without
        // it, the phone that answers second asks too early -- off its own local
        // echo, before its answer has reached the server -- is refused, and
        // then has nothing left to ask from.
        liftSealIfBothIn()
        recompose()
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
                // A breadcrumb, not the mechanism. Neither of you can see the
                // other's answer before the reveal, so this is the only record
                // on a readable document of who is in -- but it is the client's
                // account of the truth rather than the truth, and trusting it
                // is what stranded two people who had both answered.
                "submitted" to FieldValue.arrayUnion(myUid),
            ),
            SetOptions.merge(),
        )

        dayRef.collection(ANSWERS).document(myUid).set(
            mapOf(
                "values" to values,
                "guess" to guess,
                "submittedAt" to FieldValue.serverTimestamp(),
            ),
        )
    }

    /**
     * Ask for the day to be opened, and let the rules decide.
     *
     * It used to decide here, by checking the day's `submitted` list for both
     * names -- and a list the client maintains is a second copy of the truth
     * that is free to be wrong. It was: a day document written before that list
     * existed can never come to hold both names, so two people who had both
     * genuinely answered sat forever on "has not answered yet", each looking at
     * their own answer, with nothing in the data able to fix itself.
     *
     * The rules already hold the real answer and are the only thing that can be
     * trusted with it: `revealed = true` is refused unless both answer
     * documents actually exist. So the client asks whenever it has skin in the
     * game, and a refusal is not an error -- it is "not yet".
     *
     * Asking cannot loop, because a refused write changes no document and so
     * produces no new snapshot to ask again from. The two moments that *do*
     * produce one are exactly the two that matter: the other person touching
     * the day, and your own answer landing.
     *
     * Properly this belongs in a Cloud Function. The rule is what makes the
     * client version honest.
     */
    private fun liftSealIfBothIn() {
        val doc = todayDoc ?: return
        if (doc.getBoolean("revealed") == true) return
        // Nothing to ask for until I am actually in.
        if (todayAnswers[myUid] == null) return

        dayRef.set(mapOf("revealed" to true), SetOptions.merge())
            .addOnFailureListener { Log.d(TAG, "day not ready to open yet", it) }
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
            Rewards.forDay(s.today, s.you, s.them, s.streak) +
                Rewards.forDay(s.today, s.them, s.you, s.streak)
            ).filter { b -> plate.blocks.none { it.id == b.id } }
        if (fresh.isEmpty()) return

        plateRef(plate).set(
            plate.toMap() + ("blocks" to (plate.blocks + fresh).map { it.toMap() }),
            SetOptions.merge(),
        )
    }

    /**
     * Both people earn for a finished deck, each sized by their own accuracy.
     *
     * Called from recompose, because a deck reveals when the *partner* submits
     * and this phone finds out through a listener rather than through an action
     * it took.
     */
    private fun awardForQuizzes(s: CoupleState) {
        val plate = s.plate
        if (plate.isSealed) return

        val fresh = pool.decks.flatMap { quiz ->
            val mine = s.myRun(quiz.id) ?: return@flatMap emptyList()
            val theirs = s.theirRun(quiz.id) ?: return@flatMap emptyList()
            val result = QuizResult.of(quiz, mine, theirs)
            Rewards.forQuiz(result, s.you, result.youKnewThem, today) +
                Rewards.forQuiz(result, s.them, result.theyKnewYou, today)
        }.filter { b -> plate.blocks.none { it.id == b.id } }

        if (fresh.isEmpty()) return
        plateRef(plate).set(
            plate.toMap() + ("blocks" to (plate.blocks + fresh).map { it.toMap() }),
            SetOptions.merge(),
        )
    }

    override fun setColor(colorArgb: Long) {
        db.collection(USERS).document(myUid).set(
            mapOf("colorArgb" to colorArgb),
            SetOptions.merge(),
        )
    }

    /**
     * On the couple document, not yours: it is a fact about the pair, and both
     * of you have to see the same one.
     */
    override fun setAnniversary(date: LocalDate) {
        couple.set(mapOf("anniversary" to date.toString()), SetOptions.merge())
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

    /**
     * Submit a run through a deck.
     *
     * Same shape as a day, and sealed the same way: your run is its own
     * document, the partner's is unreadable until the quiz document says both
     * are in, and the rules enforce that rather than the app.
     */
    override fun submitQuizRun(
        quizId: String,
        answers: Map<String, List<String>>,
        guesses: Map<String, List<String>>,
    ) {
        if (_state.value.myRun(quizId) != null) return
        val quiz = couple.collection(QUIZZES).document(quizId)

        quiz.set(
            mapOf(
                "revealed" to false,
                "submitted" to FieldValue.arrayUnion(myUid),
            ),
            SetOptions.merge(),
        )
        quiz.collection(RUNS).document(myUid).set(
            mapOf(
                "answers" to answers,
                "guesses" to guesses,
                "completedAt" to FieldValue.serverTimestamp(),
            ),
        )
    }

    fun close() {
        regs.forEach { it.remove() }
        regs.clear()
        runRegs.values.forEach { it.remove() }
        runRegs.clear()
        partnerRunRegs.values.forEach { it.remove() }
        partnerRunRegs.clear()
    }

    private fun plateRef(plate: BuildPlate) = couple.collection(PLATES).document(plate.id)

    private fun emptyPlate(from: LocalDate) = BuildPlate(
        id = "plate_${from}",
        startedOn = from,
        periodDays = PeriodDays,
        gridW = BuildPlate.GRID_W,
        gridH = BuildPlate.GRID_H,
    )
}

// --- Mapping -----------------------------------------------------------------

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

private fun DocumentSnapshot.toDayEntry(pool: QuestionPool): DayEntry? {
    val date = runCatching { LocalDate.parse(id) }.getOrNull() ?: return null
    val question = pool.question(getString("questionId")) ?: pool.daily(date)
    // History carries no answers: they live in a subcollection that would need
    // a read each. The Shelf shows questions, dates and reactions, so this is
    // what it needs; the Reveal screen loads a single day in full.
    return DayEntry(date = date, question = question, reactions = toReactions())
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toQuizRun(quizId: String): QuizRun? {
    if (!exists()) return null
    val answers = (get("answers") as? Map<String, List<String>>).orEmpty()
    if (answers.isEmpty()) return null
    return QuizRun(
        quizId = quizId,
        uid = id,
        answers = answers,
        guesses = (get("guesses") as? Map<String, List<String>>).orEmpty(),
        completedAtMillis = getTimestamp("completedAt")?.toDate()?.time,
    )
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
        gridW = (getLong("gridW") ?: BuildPlate.GRID_W.toLong()).toInt(),
        gridH = (getLong("gridH") ?: BuildPlate.GRID_H.toLong()).toInt(),
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
