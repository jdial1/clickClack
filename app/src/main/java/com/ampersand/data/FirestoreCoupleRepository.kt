package com.ampersand.data

import com.ampersand.model.Place
import com.ampersand.model.DayWeather
import com.ampersand.model.SpecialDate
import com.ampersand.model.Topic
import com.ampersand.model.RelationshipStatus
import com.ampersand.model.BucketList
import com.ampersand.model.BucketItem
import com.ampersand.model.BucketGroup
import com.ampersand.model.BrickColor
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
import com.ampersand.model.InstrumentRun
import com.ampersand.model.Knowing
import com.ampersand.model.KnowingMonth
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Member
import com.ampersand.model.Placement
import com.ampersand.model.QuizResult
import com.ampersand.model.QuizRun
import com.ampersand.model.Wildcard
import com.ampersand.model.Reaction
import com.ampersand.model.Rewards
import com.ampersand.model.Slope
import com.ampersand.model.Milestones
import com.ampersand.model.Streaks
import com.ampersand.model.Turns
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth

private const val TAG = "Couple"
private const val DAYS = "days"
private const val ANSWERS = "answers"
private const val PLATES = "plates"
private const val QUIZZES = "quizzes"
private const val RUNS = "runs"
private const val WILDCARDS = "wildcards"
private const val BUCKET = "bucket"
private const val DATES = "dates"
private const val KNOWING = "knowing"
private const val INSTRUMENTS = "instruments"
private const val DRAFTS = "drafts"

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
 * The document layout is in README.md, under "Firestore model".
 *
 * One listener per source, all writing into [_state] through [recompose],
 * because a [CoupleState] is a whole picture and updating it field by field
 * from callbacks arriving in any order is how you get a screen that is briefly
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
    private var historyDocs: List<DocumentSnapshot> = emptyList()
    private var plates: List<BuildPlate> = emptyList()

    /**
     * Whether the plates listener has answered yet.
     *
     * Until it has, an empty [plates] means "not arrived", and awarding or
     * sealing against it laid bricks on a fresh plate that then shadowed the
     * real week.
     */
    private var platesLoaded = false
    private var anniversary: LocalDate? = null
    private var coupleDoc: DocumentSnapshot? = null

    /** Whether the day listener has answered, so "no weather" means none saved. */
    private var todayLoaded = false
    private var specialDates: List<SpecialDate> = emptyList()

    /** Every wildcard either of you has written, used or not. */
    private var allWildcards: List<Wildcard> = emptyList()

    /** The shared bucket list. */
    private var bucket: List<BucketItem> = emptyList()

    /** The day today's question came back from, on a capsule day. */
    private var capsuleSource: DayEntry? = null

    /** Month rollups of who called whom, for as long as the two of you have played. */
    private var knowing: Knowing = Knowing()

    private val couple get() = db.collection(COUPLES).document(coupleId)
    private val dayRef get() = couple.collection(DAYS).document(today.toString())

    init {
        regs += db.collection(USERS).document(myUid)
            .addSnapshotListener { d, _ -> me = d; recompose() }
        regs += db.collection(USERS).document(partnerUid)
            .addSnapshotListener { d, _ -> them = d; recompose() }

        regs += couple.addSnapshotListener { d, _ ->
            coupleDoc = d
            anniversary = d?.getString("anniversary")
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            recompose()
        }

        regs += couple.collection(DATES).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "dates listener failed", error)
                return@addSnapshotListener
            }
            specialDates = snap?.documents.orEmpty().mapNotNull { doc ->
                val label = doc.getString("label")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val date = doc.getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@mapNotNull null
                SpecialDate(doc.id, label, date)
            }
            recompose()
        }

        regs += dayRef.addSnapshotListener { d, _ ->
            todayDoc = d
            todayLoaded = true
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
                historyDocs = snap?.documents.orEmpty()
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

        // Instruments, sealed exactly like decks. One document per person, so
        // the partner's forty ratings are genuinely unreadable until both are
        // in -- and a half-open sitting is simply a run with one set of
        // responses in it, which the badge wall already draws as unclaimed.
        regs += couple.collection(INSTRUMENTS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "instrument listener failed", error)
                    return@addSnapshotListener
                }
                snap?.documents.orEmpty().forEach { doc ->
                    instrumentDocs = instrumentDocs + (doc.id to doc)
                    @Suppress("UNCHECKED_CAST")
                    val counts = doc.get("progress") as? Map<String, Long>
                    if (counts != null) {
                        instrumentProgress = instrumentProgress +
                            (doc.id to counts.mapValues { it.value.toInt() })
                    }
                    watchInstrumentRun(doc.id, myUid)
                    watchInstrumentDraft(doc.id)
                    if (doc.getBoolean("revealed") == true) watchInstrumentRun(doc.id, partnerUid)
                    liftInstrumentSealIfBothIn(doc.id)
                }
            }

        regs += couple.collection(WILDCARDS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "wildcard listener failed", error)
                    return@addSnapshotListener
                }
                allWildcards = snap?.documents.orEmpty().mapNotNull { it.toWildcard() }
                recompose()
            }

        regs += couple.collection(BUCKET)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "bucket listener failed", error)
                    return@addSnapshotListener
                }
                bucket = snap?.documents.orEmpty().mapNotNull { it.toBucketItem() }
                recompose()
            }

        // One document per month, holding a map keyed by day. Writing the same
        // day twice writes the same values, so two phones noticing the same
        // reveal cannot double-count -- which a running total would.
        regs += couple.collection(KNOWING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "knowing listener failed", error)
                    return@addSnapshotListener
                }
                knowing = Knowing(
                    snap?.documents.orEmpty().mapNotNull { it.toKnowingMonth(myUid, partnerUid) },
                )
                recompose()
            }

        loadCapsule()

        regs += couple.collection(PLATES)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "plates listener failed", error)
                    return@addSnapshotListener
                }
                plates = snap?.documents.orEmpty().mapNotNull { it.toPlate() }
                platesLoaded = true
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
        val waiting = allWildcards.filter { it.usedOn == null }.sortedBy { it.createdAtMillis }
        // Only a wildcard written before today can be today's question. One
        // written this morning waits for tomorrow, as promised, instead of
        // replacing the question while somebody is halfway through answering.
        // A pending server timestamp reads as 0 and waits too.
        val startOfToday = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ready = waiting.filter { it.createdAtMillis in 1 until startOfToday }

        // Mapped here rather than in the history listener, so a past wildcard
        // day resolves once the wildcards arrive instead of showing whatever
        // pool question happened to fall on that date.
        history = historyDocs
            .mapNotNull { it.toDayEntry(pool, ::resolveQuestion) }
            .filter { it.date != today }
            .sortedByDescending { it.date }

        val rawYou = me.toMember(myUid, current.you)
        val rawThem = them.toMember(partnerUid, current.them)
        val (yourColor, theirColor) =
            BrickColor.distinct(myUid, rawYou.colorArgb, partnerUid, rawThem.colorArgb)
        val you = rawYou.copy(colorArgb = yourColor)
        val partner = rawThem.copy(colorArgb = theirColor)
        val quizRuns = runs.values.flatMap { it.values }
        val instrumentRuns = (instrumentResponses.keys + instrumentProgress.keys +
            instrumentDrafts.keys).map { id ->
            InstrumentRun(
                instrumentId = id,
                answers = instrumentResponses[id].orEmpty(),
                progress = instrumentProgress[id].orEmpty(),
                draft = instrumentDrafts[id].orEmpty(),
            )
        }
        // What today's question is drawn from: everything that has arrived, not
        // the previous state. Reading the previous state meant a history that
        // landed last was ignored until some unrelated listener fired.
        val basis = current.copy(
            you = you,
            them = partner,
            history = history,
            quizRuns = quizRuns,
            instrumentRuns = instrumentRuns,
            knowing = knowing,
        )

        // Pinned first, always: whoever answered first decided what today is,
        // and re-deciding it on the second phone is how two people end up
        // answering two different questions.
        // What the profile tests found, as a lean on the pool.
        //
        // Computed from shared state, which is the requirement: both phones
        // have to reach the same question before either has written anything.
        // They can disagree for a moment if one has synced a sitting the other
        // has not -- and it self-corrects, because the first answer pins the
        // question and the pin outranks everything below.
        val taste = Profiles.of(basis)
        val recent = recentlyAsked(basis)
        val picked = Turns.themeFor(today, history, myUid, partnerUid)
        val milestone = Milestones.on(today, history) != null
        val hiddenByUid = coupleDoc.topicsByUid("hiddenTopics")
        val hidden = hiddenByUid.values.flatten().toSet()
        val interestsByUid = coupleDoc.topicsByUid("interests")
        val homeZip = coupleDoc?.getString("homeZip")
        val place = homeZip?.let { pool.places.lookup(it) }
        val weather = todayDoc.toWeather()
        maybeFetchWeather(place, weather)

        val pinned = todayDoc?.getString("questionId")
        val question = when {
            pinned != null -> resolveQuestion(pinned) ?: pool.daily(today, anniversary, taste, recent, picked, deeper = milestone, hidden = hidden, weatherKind = weather?.kind, region = place?.region)
            // A question one of you wrote outranks one a stranger wrote.
            ready.isNotEmpty() -> ready.first().asQuestion()
            capsuleSource != null -> capsuleSource!!.question
            else -> pool.daily(today, anniversary, taste, recent, picked, deeper = milestone, hidden = hidden, weatherKind = weather?.kind, region = place?.region)
        }

        val todayEntry = DayEntry(
            date = today,
            question = question,
            answers = todayAnswers,
            reactions = todayDoc.toReactions(),
            talkedAbout = todayDoc.toTalkedAbout(),
            opened = todayDoc?.getBoolean("revealed") == true,
            submitted = todayDoc.toSubmitted(),
            nextTheme = todayDoc?.getString("nextTheme"),
            nextThemeBy = todayDoc?.getString("nextThemeBy"),
        )

        val live = plates.filter { !it.isSealed }.maxByOrNull { it.startedOn }
            ?: emptyPlate(today)

        // The period is over, so the sculpture is dated. Driven by the calendar
        // rather than by a button: a week ends whether or not anyone opened the
        // app that day.
        if (platesLoaded && live.isSealed.not() && live.placed.isNotEmpty() && live.endsOn() < today) {
            sealPlate()
        }

        val next = basis.copy(
            today = todayEntry,
            plate = live,
            shelf = plates.filter { it.isSealed }.sortedByDescending { it.completedOn },
            anniversary = anniversary,
            streak = Streaks.run(history, todayEntry),
            // Only a capsule if today really is asking the old question again.
            // The ladder can find a day whose question the pin then overrode.
            capsule = capsuleSource?.takeIf { it.question.id == question.id },
            wildcards = waiting,
            bucket = bucket,
            relationship = coupleDoc?.getString("relationship")
                ?.let { s -> RelationshipStatus.entries.firstOrNull { it.name == s } },
            marriedOn = coupleDoc?.getString("marriedOn")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            yourHiddenTopics = hiddenByUid[myUid].orEmpty(),
            hiddenTopics = hidden,
            yourInterests = interestsByUid[myUid].orEmpty(),
            interests = interestsByUid.values.flatten().toSet(),
            homeZip = homeZip,
            region = place?.region,
            weather = weather,
            specialDates = specialDates,
        )

        // The assignment. Without it every listener in this class computed the
        // right answer and dropped it on the floor: the screen kept the
        // placeholder the constructor built and nothing anyone did ever showed
        // up, with no error to explain it.
        _state.value = next

        // Earning is done here rather than in the write that caused it, because
        // a day or a deck opens when the *other* person submits -- which this
        // phone learns from a listener, not from anything it did.
        if (todayDoc?.getBoolean("revealed") == true) {
            if (platesLoaded) awardForToday()
            recordKnowing(todayEntry)
        }
        if (platesLoaded) {
            awardForQuizzes(next)
            awardForTalks(next)
            awardForBucket(next)
        }
    }

    /**
     * Questions not to ask today: the last six weeks of days, and any deck the
     * two of you both finished in that time.
     *
     * Both runs, not just yours. Your own run is readable the moment you finish,
     * your partner's only once the deck opens -- so counting a deck as soon as
     * one of you had played it would give the two phones different sets, and
     * they would each land on a different question.
     */
    private fun recentlyAsked(s: CoupleState): Set<String> {
        val since = today.minusDays(QuestionPool.RecentDays)
        val fromDays = history.filter { it.date >= since }.map { it.question.id }
        val cutoff = since.toEpochDay() * 86_400_000L
        val fromDecks = pool.playable.filter { quiz ->
            val mine = s.myRun(quiz.id)
            val theirs = s.theirRun(quiz.id)
            mine != null && theirs != null &&
                maxOf(mine.completedAtMillis ?: Long.MAX_VALUE, theirs.completedAtMillis ?: Long.MAX_VALUE) >= cutoff
        }.flatMap { quiz -> quiz.questions.map { it.id } }
        return (fromDays + fromDecks).toSet()
    }

    /**
     * Lay the next-morning piece for any day either of you said came up again.
     *
     * Checked against every plate ever built, not just this week's, because the
     * day being talked about usually belongs to a week that has already sealed
     * -- and a piece is never laid twice.
     */
    private fun awardForTalks(s: CoupleState) {
        val plate = s.plate
        if (plate.isSealed) return
        val laid = (plates.flatMap { it.blocks } + plate.blocks).mapTo(mutableSetOf()) { it.id }
        val fresh = s.history.filter { it.wasTalkedAbout }.flatMap { day ->
            Rewards.forTalk(day, s.you, s.them, today) + Rewards.forTalk(day, s.them, s.you, today)
        }.filter { it.id !in laid }
        if (fresh.isEmpty()) return

        plateRef(plate).set(
            plate.toMap() + ("blocks" to (plate.blocks + fresh).map { it.toMap() }),
            SetOptions.merge(),
        )
    }

    /**
     * A brick each for anything ticked off the list. By id, and checked against
     * every plate, so a tick is paid once however many phones see it.
     */
    private fun awardForBucket(s: CoupleState) {
        val plate = s.plate
        if (plate.isSealed) return
        val laid = (plates.flatMap { it.blocks } + plate.blocks).mapTo(mutableSetOf()) { it.id }
        val fresh = s.bucket.flatMap { item ->
            listOfNotNull(BucketList.reward(item, s.you), BucketList.reward(item, s.them))
        }.filter { it.id !in laid }
        if (fresh.isEmpty()) return
        plateRef(plate).set(
            plate.toMap() + ("blocks" to (plate.blocks + fresh).map { it.toMap() }),
            SetOptions.merge(),
        )
    }

    private val runRegs = mutableMapOf<String, ListenerRegistration>()
    private val partnerRunRegs = mutableMapOf<String, ListenerRegistration>()

    /** quiz id -> uid -> run, so one listener per person cannot clobber the other. */
    private var runs: Map<String, Map<String, QuizRun>> = emptyMap()

    /** The deck documents themselves, so a run landing can ask to open one. */
    private var quizDocs: Map<String, DocumentSnapshot> = emptyMap()

    private val instrumentRegs = mutableMapOf<String, ListenerRegistration>()

    /** instrument id -> uid -> responses, in published order, passes concatenated. */
    private var instrumentResponses: Map<String, Map<String, List<Int>>> = emptyMap()
    private var instrumentDocs: Map<String, DocumentSnapshot> = emptyMap()

    /** instrument id -> uid -> how many answered, sealed or not. */
    private var instrumentProgress: Map<String, Map<String, Int>> = emptyMap()

    /** Your own half-finished answers. There is nowhere for theirs to come from. */
    private var instrumentDrafts: Map<String, List<Int>> = emptyMap()

    /**
     * Your draft, and only ever yours.
     *
     * The rules refuse a read of anybody else's, which is the same protection
     * the sealed run has -- a draft is unsealed answers and is the one thing
     * that must not leak early. What crosses between the two of you is the
     * count on the parent document.
     */
    private fun watchInstrumentDraft(id: String) {
        val key = "draft/" + id
        if (key in instrumentRegs) return
        instrumentRegs[key] = couple.collection(INSTRUMENTS).document(id)
            .collection(DRAFTS).document(myUid)
            .addSnapshotListener { d, _ ->
                @Suppress("UNCHECKED_CAST")
                val raw = d?.get("responses") as? List<Long>
                instrumentDrafts = if (raw == null) {
                    instrumentDrafts - id
                } else {
                    instrumentDrafts + (id to raw.map { it.toInt() })
                }
                recompose()
            }
    }

    /**
     * One listener per person per instrument.
     *
     * Keyed by both, unlike the deck version which needed two maps, because an
     * instrument has no partner-specific bookkeeping -- there is nothing to
     * read but the numbers.
     */
    private fun watchInstrumentRun(id: String, uid: String) {
        val key = "$id/$uid"
        if (key in instrumentRegs) return
        instrumentRegs[key] = couple.collection(INSTRUMENTS).document(id)
            .collection(RUNS).document(uid)
            .addSnapshotListener { d, _ -> putInstrumentRun(id, uid, d) }
    }

    private fun putInstrumentRun(id: String, uid: String, doc: DocumentSnapshot?) {
        @Suppress("UNCHECKED_CAST")
        val raw = doc?.get("responses") as? List<Long>
        val forId = instrumentResponses[id].orEmpty().let { existing ->
            if (raw == null) existing - uid else existing + (uid to raw.map { it.toInt() })
        }
        instrumentResponses = instrumentResponses + (id to forId)
        liftInstrumentSealIfBothIn(id)
        recompose()
    }

    /** Ask the rules to open it; refusal means the other half is not in yet. */
    private fun liftInstrumentSealIfBothIn(id: String) {
        val doc = instrumentDocs[id] ?: return
        if (doc.getBoolean("revealed") == true) return
        if (instrumentResponses[id]?.get(myUid) == null) return

        couple.collection(INSTRUMENTS).document(id)
            .set(mapOf("revealed" to true), SetOptions.merge())
            .addOnFailureListener { Log.d(TAG, "instrument not ready to open yet", it) }
    }

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

        // Asked, so it leaves the queue. Stamped here rather than when it is
        // chosen, because being chosen is a guess about today until someone
        // actually answers it.
        val asked = _state.value.today.question.id
        if (Wildcard.isWildcard(asked)) {
            couple.collection(WILDCARDS).document(Wildcard.idOf(asked))
                .set(mapOf("usedOn" to today.toString()), SetOptions.merge())
        }
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

        // The run goes down with the seal, so a streak past the ten weeks of
        // history the phones load still has something to count from.
        // Counted with today as open, which is what this write makes it. The
        // state still holds the moment before, so it was always one short.
        val s = _state.value
        val run = Streaks.run(s.history, s.today.copy(opened = true))
        dayRef.set(mapOf("revealed" to true, "run" to run), SetOptions.merge())
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

        // Every plate ever built, not just this one. Checking only this week's
        // plate meant that when a week sealed, every deck the two of you had
        // ever finished was paid out again on the new one.
        val laid = (plates.flatMap { it.blocks } + plate.blocks).mapTo(mutableSetOf()) { it.id }
        val fresh = pool.playable.flatMap { quiz ->
            val mine = s.myRun(quiz.id) ?: return@flatMap emptyList()
            val theirs = s.theirRun(quiz.id) ?: return@flatMap emptyList()
            val result = QuizResult.of(quiz, mine, theirs)
            Rewards.forQuiz(
                result, s.you, s.them, result.youKnewThem, today,
                effort = Rewards.deckEffort(result.rows, yours = true),
            ) + Rewards.forQuiz(
                result, s.them, s.you, result.theyKnewYou, today,
                effort = Rewards.deckEffort(result.rows, yours = false),
            )
        }.filter { it.id !in laid }

        if (fresh.isEmpty()) return
        plateRef(plate).set(
            plate.toMap() + ("blocks" to (plate.blocks + fresh).map { it.toMap() }),
            SetOptions.merge(),
        )
    }

    override fun noteWeek(plateId: String, text: String) {
        // Merged into the map, so writing yours never touches theirs.
        couple.collection(PLATES).document(plateId)
            .set(mapOf("notes" to mapOf(myUid to text)), SetOptions.merge())
    }

    override fun addToBucket(text: String, group: BucketGroup) {
        couple.collection(BUCKET).add(
            mapOf(
                "text" to text,
                "group" to group.name,
                "byUid" to myUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "doneOn" to null,
            ),
        )
    }

    override fun tickBucket(id: String) {
        couple.collection(BUCKET).document(id)
            .update(mapOf("doneOn" to today.toString(), "doneBy" to myUid))
    }

    override fun removeFromBucket(id: String) {
        couple.collection(BUCKET).document(id).delete()
    }

    override fun addWildcard(text: String, type: QuestionType, options: List<String>) {
        couple.collection(WILDCARDS).add(
            mapOf(
                "text" to text,
                "type" to type.name,
                "options" to options,
                "byUid" to myUid,
                "byName" to _state.value.you.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "usedOn" to null,
            ),
        )
    }

    /**
     * Record who called whom, once a day has opened.
     *
     * Keyed by uid rather than by "you" and "them", because both phones write
     * this document and each one's idea of "you" is the other's idea of "them".
     * The first version of this flipped the scoreboard every time the second
     * person's app noticed the reveal.
     */
    private fun recordKnowing(day: DayEntry) {
        if (!day.isRevealed || !day.question.isScored) return
        val mine = day.answerFor(myUid) ?: return
        val theirs = day.answerFor(partnerUid) ?: return

        couple.collection(KNOWING).document(YearMonth.from(day.date).toString()).set(
            mapOf(
                day.date.toString() to mapOf(
                    myUid to day.question.guessHit(mine.guess, theirs.values),
                    partnerUid to day.question.guessHit(theirs.guess, mine.values),
                ),
            ),
            SetOptions.merge(),
        )
    }

    /**
     * Find a day worth asking again, and read it back in full.
     *
     * The ladder runs a year, six months, three, then one. A couple three weeks
     * in has no year to look back on, and a feature that only switches on after
     * six months is a feature nobody ever sees working.
     *
     * **How often is a function of how much there is to look back on.** The pool
     * is ninety-nine questions and it will run out; that is arithmetic rather
     * than a bug, and the second lap is not something to be ashamed of. A
     * question the two of you answered a year ago, shown beside what you said
     * then, is a better thing than a question neither of you has seen -- so as
     * the history fills, the capsule stops being a Sunday treat and becomes a
     * regular part of the week.
     *
     * It never takes more than half the days. Every day would make the app a
     * quiz about itself, and the rest of the pool would stop arriving.
     */
    private fun loadCapsule() {
        if (today.dayOfWeek !in capsuleDays()) return
        tryCapsule(listOf(12L, 6L, 3L, 1L))
    }

    /**
     * Which days of the week may look backwards, widening with the archive.
     *
     * Counted in sealed plates rather than in days, for two reasons. `history`
     * is capped at seventy days by its own query, so it can never answer "how
     * long have you been doing this" -- it would have sat at the first rung
     * forever. And a plate is a week by construction, so the number is already
     * the unit the question is about.
     *
     * Fixed sets rather than a draw: the day has to come out the same on both
     * phones, and both phones count the same plates.
     */
    private fun capsuleDays(): Set<DayOfWeek> {
        val weeks = plates.count { it.isSealed }
        return when {
            // Two years in. The past is now the better half of the pool.
            weeks >= 104 -> setOf(
                DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY,
            )
            // Around the point the pool has been round once.
            weeks >= 26 -> setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            weeks >= 12 -> setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY)
            else -> setOf(DayOfWeek.SUNDAY)
        }
    }

    private fun tryCapsule(months: List<Long>) {
        val ahead = months.firstOrNull() ?: return
        val date = today.minusMonths(ahead)
        val rest = months.drop(1)

        couple.collection(DAYS).document(date.toString()).get()
            .addOnSuccessListener { doc ->
                val question = resolveQuestion(doc.getString("questionId"))
                if (doc.exists() && doc.getBoolean("revealed") == true && question != null) {
                    readCapsuleAnswers(date, question, doc)
                } else {
                    tryCapsule(rest)
                }
            }
            .addOnFailureListener { tryCapsule(rest) }
    }

    private fun readCapsuleAnswers(date: LocalDate, question: Question, dayDoc: DocumentSnapshot) {
        val answers = couple.collection(DAYS).document(date.toString()).collection(ANSWERS)
        answers.document(myUid).get().addOnSuccessListener { mine ->
            answers.document(partnerUid).get().addOnSuccessListener { theirs ->
                val both = listOfNotNull(mine.toAnswer(), theirs.toAnswer())
                if (both.size < 2) return@addOnSuccessListener
                capsuleSource = DayEntry(
                    date = date,
                    question = question,
                    answers = both.associateBy { it.uid },
                    reactions = dayDoc.toReactions(),
                )
                recompose()
            }
        }
    }

    /** A pool question, or one of you wrote it. */
    private fun resolveQuestion(id: String?): Question? = when {
        id == null -> null
        Wildcard.isWildcard(id) ->
            allWildcards.firstOrNull { it.id == Wildcard.idOf(id) }?.asQuestion()
        else -> pool.question(id)
    }

    override fun setProfile(gender: String?, birthday: LocalDate?) {
        db.collection(USERS).document(myUid).set(
            mapOf("gender" to gender, "birthday" to birthday?.toString()),
            SetOptions.merge(),
        )
    }

    override fun setRelationship(status: RelationshipStatus, marriedOn: LocalDate?) {
        couple.set(
            mapOf(
                "relationship" to status.name,
                "marriedOn" to marriedOn?.takeIf { status == RelationshipStatus.MARRIED }?.toString(),
            ),
            SetOptions.merge(),
        )
    }

    override fun setTopicHidden(topic: Topic, hidden: Boolean) {
        val mine = _state.value.yourHiddenTopics.let { if (hidden) it + topic else it - topic }
        // Merged by uid, so writing yours leaves theirs alone.
        couple.set(mapOf("hiddenTopics" to mapOf(myUid to mine.map { it.name })), SetOptions.merge())
    }

    override fun setTopicInterest(topic: Topic, interested: Boolean) {
        val mine = _state.value.yourInterests.let { if (interested) it + topic else it - topic }
        couple.set(mapOf("interests" to mapOf(myUid to mine.map { it.name })), SetOptions.merge())
    }

    override fun setHomeZip(zip: String): Boolean {
        pool.places.lookup(zip) ?: return false
        couple.set(mapOf("homeZip" to zip.trim()), SetOptions.merge())
        // A new home is a new forecast, if today's has not been saved yet.
        weatherTried = false
        return true
    }

    /** Once per repository: one fetch, however many listeners fire. */
    private var weatherTried = false

    /**
     * Fetch today's forecast once and save it on the day, so the other phone
     * reads the same weather instead of fetching its own a minute later and
     * landing on a different question.
     *
     * Only when the day has no weather yet, and never blocking: a failed or
     * slow fetch is an ordinary day. Saved in a transaction so two phones
     * fetching at the same moment keep the first.
     */
    private fun maybeFetchWeather(place: Place?, saved: DayWeather?) {
        if (weatherTried || place == null || saved != null || !todayLoaded) return
        weatherTried = true
        Thread {
            val fetched = runCatching { NationalWeatherService.today(place) }
                .onFailure { Log.d(TAG, "no forecast today", it) }
                .getOrNull() ?: return@Thread
            db.runTransaction { txn ->
                if (txn.get(dayRef).get("weather") == null) {
                    txn.set(dayRef, mapOf("weather" to fetched.toMap()), SetOptions.merge())
                }
                null
            }.addOnFailureListener { Log.d(TAG, "forecast not saved", it) }
        }.start()
    }

    override fun addSpecialDate(label: String, date: LocalDate) {
        couple.collection(DATES).add(mapOf("label" to label, "date" to date.toString(), "byUid" to myUid))
    }

    override fun removeSpecialDate(id: String) {
        couple.collection(DATES).document(id).delete()
    }

    override fun setColor(colorArgb: Long) {
        // Theirs is theirs. The picker already refuses it; this is the backstop.
        if (BrickColor.of(colorArgb) == BrickColor.of(_state.value.them.colorArgb)) return
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

    /**
     * Additive, via arrayUnion, so two phones marking the same day cannot
     * clobber each other and nobody has to hold a read-modify-write.
     */
    override fun markTalkedAbout(date: LocalDate) {
        val ref = if (date == today) dayRef else couple.collection(DAYS).document(date.toString())
        ref.set(
            mapOf("talkedAbout" to FieldValue.arrayUnion(myUid)),
            SetOptions.merge(),
        )
    }

    /**
     * Tonight's pick for tomorrow. Written once: the phone checks it is your
     * turn and nothing is picked, and tomorrow ignores a pick by the wrong
     * person, so a race between two phones cannot change what the day is.
     */
    override fun pickTomorrow(theme: String) {
        val day = _state.value.today
        if (!Turns.canPick(day, myUid, partnerUid)) return
        dayRef.set(
            mapOf("nextTheme" to theme, "nextThemeBy" to myUid),
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
        // A fixed id, so two phones sealing the same week write one next plate
        // instead of two that each carry the same bricks.
        plateRef(next).set(
            next.toMap() + ("blocks" to next.blocks.map { it.toMap() }),
            SetOptions.merge(),
        )
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

    override fun submitInstrument(instrumentId: String, responses: List<Int>) {
        if (instrumentResponses[instrumentId]?.get(myUid) != null) return
        val doc = couple.collection(INSTRUMENTS).document(instrumentId)

        doc.set(
            mapOf(
                "revealed" to false,
                "submitted" to FieldValue.arrayUnion(myUid),
                "progress" to mapOf(myUid to responses.size),
            ),
            SetOptions.merge(),
        )
        doc.collection(RUNS).document(myUid).set(
            mapOf(
                "responses" to responses,
                "completedAt" to FieldValue.serverTimestamp(),
            ),
        )
        // The draft has done its job. Leaving it would keep a second, editable
        // copy of answers that are now supposed to be immutable.
        doc.collection(DRAFTS).document(myUid).delete()
    }

    override fun saveInstrumentDraft(instrumentId: String, responses: List<Int>) {
        if (instrumentResponses[instrumentId]?.get(myUid) != null) return
        val doc = couple.collection(INSTRUMENTS).document(instrumentId)

        doc.set(
            mapOf(
                "revealed" to false,
                // A count, not the answers. This is the half that is meant to
                // be visible to the other person.
                "progress" to mapOf(myUid to responses.count { it > 0 }),
            ),
            SetOptions.merge(),
        )
        doc.collection(DRAFTS).document(myUid).set(
            mapOf(
                "responses" to responses,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
    }

    fun close() {
        regs.forEach { it.remove() }
        regs.clear()
        runRegs.values.forEach { it.remove() }
        runRegs.clear()
        instrumentRegs.values.forEach { it.remove() }
        instrumentRegs.clear()
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
    gender = this?.getString("gender"),
    birthday = this?.getString("birthday")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
)

private fun DayWeather.toMap(): Map<String, Any?> =
    mapOf("tempF" to tempF, "summary" to summary, "city" to city, "precipChance" to precipChance)

private fun DocumentSnapshot?.toWeather(): DayWeather? {
    val w = this?.get("weather") as? Map<*, *> ?: return null
    val temp = (w["tempF"] as? Number)?.toInt() ?: return null
    return DayWeather(
        tempF = temp,
        summary = w["summary"] as? String ?: "",
        city = w["city"] as? String,
        precipChance = (w["precipChance"] as? Number)?.toInt(),
    )
}

private fun DocumentSnapshot?.topicsByUid(field: String): Map<String, Set<Topic>> =
    (this?.get(field) as? Map<*, *>).orEmpty().entries.mapNotNull { (uid, names) ->
        val key = uid as? String ?: return@mapNotNull null
        key to (names as? List<*>).orEmpty().mapNotNull { n -> Topic.entries.firstOrNull { it.name == n } }.toSet()
    }.toMap()

private fun DocumentSnapshot?.toSubmitted(): Set<String> =
    (this?.get("submitted") as? List<*>).orEmpty().filterIsInstance<String>().toSet()

private fun DocumentSnapshot?.toReactions(): Map<String, Reaction> {
    val raw = this?.get("reactions") as? Map<*, *> ?: return emptyMap()
    return raw.entries.mapNotNull { (k, v) ->
        val uid = k as? String ?: return@mapNotNull null
        val name = v as? String ?: return@mapNotNull null
        Reaction.entries.firstOrNull { it.name == name }?.let { uid to it }
    }.toMap()
}

private fun DocumentSnapshot?.toTalkedAbout(): Set<String> {
    @Suppress("UNCHECKED_CAST")
    val raw = this?.get("talkedAbout") as? List<String> ?: return emptySet()
    return raw.toSet()
}

private fun DocumentSnapshot.toWildcard(): Wildcard? {
    val text = getString("text")?.takeIf { it.isNotBlank() } ?: return null
    val type = QuestionType.entries.firstOrNull { it.name == getString("type") } ?: return null
    @Suppress("UNCHECKED_CAST")
    val options = (get("options") as? List<String>).orEmpty()
    return Wildcard(
        id = id,
        text = text,
        type = type,
        options = options,
        byUid = getString("byUid").orEmpty(),
        byName = getString("byName").orEmpty(),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        usedOn = getString("usedOn")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    )
}

private fun DocumentSnapshot.toBucketItem(): BucketItem? {
    val text = getString("text")?.takeIf { it.isNotBlank() } ?: return null
    return BucketItem(
        id = id,
        text = text,
        group = BucketGroup.entries.firstOrNull { it.name == getString("group") } ?: BucketGroup.GOALS,
        byUid = getString("byUid").orEmpty(),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        doneOn = getString("doneOn")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    )
}

private fun DocumentSnapshot.toKnowingMonth(myUid: String, partnerUid: String): KnowingMonth? {
    val month = runCatching { YearMonth.parse(id) }.getOrNull() ?: return null
    var mine = 0
    var theirs = 0
    var scored = 0
    data.orEmpty().forEach { (_, value) ->
        val day = value as? Map<*, *> ?: return@forEach
        scored++
        if (day[myUid] == true) mine++
        if (day[partnerUid] == true) theirs++
    }
    return KnowingMonth(month, youHits = mine, theyHits = theirs, scored = scored)
}

private fun DocumentSnapshot.toAnswer(): Answer? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val values = get("values") as? List<String> ?: return null
    @Suppress("UNCHECKED_CAST")
    val guess = get("guess") as? List<String>
    return Answer(uid = id, values = values, guess = guess)
}

private fun DocumentSnapshot.toDayEntry(pool: QuestionPool, resolve: (String?) -> Question?): DayEntry? {
    val date = runCatching { LocalDate.parse(id) }.getOrNull() ?: return null
    // A day document can exist with nobody having answered: the forecast is
    // saved on it first thing. That is not a day anyone played.
    if (getString("questionId") == null && getBoolean("revealed") != true && toSubmitted().isEmpty()) return null
    val question = resolve(getString("questionId")) ?: pool.daily(date)
    // History carries no answers: they live in a subcollection that would need
    // a read each. The Shelf shows questions, dates and reactions, so this is
    // what it needs; the Reveal screen loads a single day in full.
    return DayEntry(
        date = date,
        question = question,
        reactions = toReactions(),
        talkedAbout = toTalkedAbout(),
        opened = getBoolean("revealed") == true,
        submitted = toSubmitted(),
        nextTheme = getString("nextTheme"),
        nextThemeBy = getString("nextThemeBy"),
        recordedRun = getLong("run")?.toInt() ?: 0,
    )
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
        notes = (get("notes") as? Map<*, *>).orEmpty().entries
            .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { key to it } } }
            .toMap(),
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
) + (
    // Written only when set. A merge carrying completedOn: null from a stale
    // copy of the plate un-sealed a week the other phone had just sealed.
    completedOn?.let { mapOf("completedOn" to it.toString()) } ?: emptyMap()
)

