package com.ampersand.data

import com.ampersand.model.DayPhoto
import com.ampersand.model.Together
import com.ampersand.model.decadeOf
import com.ampersand.model.ChineseZodiac
import com.ampersand.model.forSigns
import com.google.firebase.firestore.FieldPath
import com.ampersand.model.QuestionSource
import com.ampersand.model.NameWords
import com.ampersand.model.Dates
import com.ampersand.model.DateKind
import com.google.firebase.firestore.DocumentReference
import android.os.Looper
import android.os.Handler
import com.ampersand.model.Mood
import com.ampersand.model.Following
import com.ampersand.model.League
import com.ampersand.model.Teams
import com.ampersand.model.forPlace
import com.ampersand.model.Place
import com.ampersand.model.DayWeather
import com.ampersand.model.SpecialDate
import com.ampersand.model.Topic
import com.ampersand.model.RelationshipStatus
import com.ampersand.model.BucketItem
import com.ampersand.model.BucketGroup
import com.ampersand.model.BrickColor
import android.util.Log
import com.ampersand.auth.COUPLES
import com.ampersand.auth.USERS
import com.ampersand.model.Answer
import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.InstrumentRun
import com.ampersand.model.Knowing
import com.ampersand.model.Memories
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Member
import com.ampersand.model.QuizRun
import com.ampersand.model.Wildcard
import com.ampersand.model.Reaction
import com.ampersand.model.Rewards
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
private const val PHOTOS = "photos"
private const val KNOWING = "knowing"
private const val INSTRUMENTS = "instruments"
private const val DRAFTS = "drafts"

/** Days of history to load (ten weeks, what the Shelf shows). */
private const val HistoryDays = 70L

/** Each person's words toward a sealed week's name, under the plate. */
private const val WORDS = "words"

/**
 * Firestore-backed repository. Document layout: README.md, "Firestore model".
 * Today's question is computed from the bundled [QuestionPool] so both phones agree before any write.
 * Each listener stores its raw piece and calls [recompose], which rebuilds the whole [CoupleState].
 */
class FirestoreCoupleRepository(
    private val coupleId: String,
    private val myUid: String,
    private val partnerUid: String,
    private val pool: QuestionPool,
    /** The couple's home zone. [today] is the date there, never the phone's own. */
    private val zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    private val today: LocalDate = LocalDate.now(zone),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CoupleRepository {

    private val _state = MutableStateFlow(
        // Placeholder state until the first snapshots arrive.
        CoupleState(
            you = Member(myUid, "You", 0xFFF54E4F),
            them = Member(partnerUid, "Them", 0xFF108BE2),
            today = DayEntry(today, pool.daily(today)),
            plate = BuildPlate.startingOn(today),
        ),
    )
    override val state: StateFlow<CoupleState> = _state.asStateFlow()

    private val regs = mutableListOf<ListenerRegistration>()

    // Raw pieces for recompose(). Null means not arrived yet.
    private var me: DocumentSnapshot? = null
    private var them: DocumentSnapshot? = null
    private var todayDoc: DocumentSnapshot? = null
    private var todayAnswers: Map<String, Answer> = emptyMap()
    private var history: List<DayEntry> = emptyList()
    private var historyDocs: List<DocumentSnapshot> = emptyList()
    private var plates: List<BuildPlate> = emptyList()

    /** Whether plates have loaded. Awarding or sealing before this writes to a wrong, empty plate. */
    private var platesLoaded = false
    private var historyLoaded = false
    private var anniversary: LocalDate? = null
    private var coupleDoc: DocumentSnapshot? = null

    /** Whether the day doc has loaded, so missing weather means none saved. */
    private var todayLoaded = false
    private var specialDates: List<SpecialDate> = emptyList()

    /** All wildcards, used or not. */
    private var allWildcards: List<Wildcard> = emptyList()

    private var bucket: List<BucketItem> = emptyList()

    /** The past day today's question repeats, on a capsule day. */
    private var capsuleSource: DayEntry? = null

    /** Today's date in earlier years and seasons, with both answers (see [loadMemories]). */
    private var memoryDays: Map<LocalDate, DayEntry> = emptyMap()

    /** Your own words toward sealed weeks' names, read by name (see [loadMyWords]). */
    private var myWords: Map<String, NameWords> = emptyMap()
    private var myWordsAsked: Set<String> = emptySet()

    private var photos: List<DayPhoto> = emptyList()

    /** Day documents older than the loaded history, read on demand (see [loadAnswers]). */
    private var recalledDocs: Map<LocalDate, DocumentSnapshot> = emptyMap()

    /** Answers read for past days on demand (see [loadAnswers]). */
    private var pastAnswers: Map<LocalDate, Map<String, Answer>> = emptyMap()

    /** Monthly guess-hit rollups. */
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
                .toDateOrNull()
            recompose()
        }

        regs += couple.collection(PHOTOS).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "photos listener failed", error)
                return@addSnapshotListener
            }
            photos = snap?.documents.orEmpty().mapNotNull { doc ->
                val date = doc.getString("date").toDateOrNull() ?: return@mapNotNull null
                DayPhoto(doc.id, date, doc.getString("label").orEmpty(), doc.getString("byUid").orEmpty(), doc.getString("data") ?: return@mapNotNull null)
            }.sortedByDescending { it.date }
            recompose()
        }

        regs += couple.collection(DATES).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "dates listener failed", error)
                return@addSnapshotListener
            }
            specialDates = snap?.documents.orEmpty().mapNotNull { doc ->
                val label = doc.getString("label")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val date = doc.getString("date").toDateOrNull()
                    ?: return@mapNotNull null
                SpecialDate(doc.id, label, date, DateKind.entries.firstOrNull { it.name == doc.getString("kind") } ?: DateKind.OTHER)
            }
            recompose()
        }

        regs += dayRef.addSnapshotListener { d, _ ->
            todayDoc = d
            todayLoaded = true
            // The partner's answer is only readable after reveal.
            if (d?.getBoolean("revealed") == true) watchPartnerAnswer()
            liftSealIfBothIn()
            recompose()
        }

        // Listen to your own answer doc by id, not the collection: rules deny a
        // collection query while sealed, and the listener dies silently.
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
                historyLoaded = true
                recompose()
            }

        // Deck docs. Your run is watched per deck; the partner's only after reveal (rules).
        regs += couple.collection(QUIZZES)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "quiz listener failed", error)
                    return@addSnapshotListener
                }
                snap?.documents.orEmpty().forEach { quiz ->
                    quizDocs = quizDocs + (quiz.id to quiz)
                    watchRuns(quiz.id)
                    // Open decks (sealed = false) let you read theirs straight away.
                    if (quiz.getBoolean("revealed") == true || quiz.getBoolean("sealed") == false) watchPartnerRun(quiz.id)
                    liftQuizSealIfBothIn(quiz.id)
                }
            }

        // Instruments, sealed like decks: one run doc per person, partner's readable after reveal.
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

        // One doc per month, keyed by day, so repeated writes cannot double-count.
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
        loadMemories()

        regs += couple.collection(PLATES)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "plates listener failed", error)
                    return@addSnapshotListener
                }
                plates = snap?.documents.orEmpty().mapNotNull { it.toPlate() }
                platesLoaded = true
                loadMyWords()
                recompose()
            }
    }

    /** Rebuilds state from whatever has arrived. Safe to call from any listener; missing pieces use defaults. */
    private fun recompose() {
        val current = _state.value
        val waiting = allWildcards.filter { it.usedOn == null }.sortedBy { it.createdAtMillis }
        // Only wildcards created before today are eligible. A pending server timestamp reads as 0 and waits.
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val ready = waiting.filter { it.createdAtMillis in 1 until startOfToday }

        // History is mapped here (not in its listener) so past wildcard days resolve once wildcards load.
        val homeZip = coupleDoc?.getString("homeZip")
        val place = homeZip?.let { pool.places.lookup(it) }
        // Backfill the time zone for ZIPs saved before it was stored.
        place?.timeZone?.takeIf { coupleDoc?.getString("timeZone") != it }?.let {
            couple.set(mapOf("timeZone" to it), SetOptions.merge())
        }
        // No ZIP yet: the first phone to look lends its own zone, so both phones
        // still share one calendar. Setting a ZIP replaces it.
        if (coupleDoc != null && place == null && coupleDoc?.getString("timeZone") == null) {
            couple.set(mapOf("timeZone" to java.time.ZoneId.systemDefault().id), SetOptions.merge())
        }
        history = historyDocs
            .mapNotNull { it.toDayEntry(pool) { id, date -> resolveQuestion(id, date) } }
            .map { day -> pastAnswers[day.date]?.let { day.copy(answers = it) } ?: day }
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
        // Draw from freshly assembled data, not the previous state, or late history is ignored.
        val basis = current.copy(
            you = you,
            them = partner,
            history = history,
            quizRuns = quizRuns,
            instrumentRuns = instrumentRuns,
            knowing = knowing,
        )

        // Inputs come from shared state so both phones pick the same question. A pinned
        // questionId (written on first answer) always wins, which fixes any brief mismatch.
        val taste = Profiles.of(basis)
        val recent = recentlyAsked(basis)
        val picked = Turns.themeFor(today, history, myUid, partnerUid)
        val hiddenByUid = coupleDoc.topicsByUid("hiddenTopics")
        val interestsByUid = coupleDoc.topicsByUid("interests")
        val followingByUid = coupleDoc.leaguesByUid()
        val following = followingByUid.values.flatten().toSet()
        val interests = interestsByUid.values.flatten().toSet()
        val hidden = hiddenByUid.values.flatten().toSet() +
            (if (Following.hideSports(following, interests)) setOf(Topic.SPORTS) else emptySet())
        val weather = todayDoc.toWeather()
        val marriedOn = coupleDoc?.getString("marriedOn").toDateOrNull()
        val dateKinds = Dates.kindsOn(today, specialDates, listOf(you.birthday, partner.birthday), marriedOn)
        val leagues = Following.leagues(place?.let { Teams.activeLeagues(it, today) }.orEmpty(), following)
        maybeFetchWeather(place, weather)

        val off = QuestionSource.entries.filter { coupleDoc?.getBoolean(it.field) == false }.toSet()
        val signs = Topic.HOROSCOPES in interests && you.birthday != null && partner.birthday != null
        val birthdays = listOfNotNull(you.birthday, partner.birthday)
        val together = Together.on(today, anniversary, marriedOn)
        val decades = birthdays.map(::decadeOf).toSet()
        val animals = if (Topic.HOROSCOPES in interests) birthdays.map { ChineseZodiac.of(it) }.toSet() else emptySet()
        // Only once both are in: an older couple's empty first read must not look like day one.
        val firstWeek = if (historyLoaded && platesLoaded) {
            firstWeekDay(history, sealedSculpture = plates.any { it.isSealed && it.placed.isNotEmpty() })
        } else {
            null
        }
        fun draw() = pool.daily(today, anniversary, taste, recent, picked, hidden = hidden, weatherKind = weather?.kind.takeIf { QuestionSource.WEATHER !in off }, region = place?.region.takeIf { QuestionSource.LOCAL !in off }, state = place?.state.takeIf { QuestionSource.HOLIDAYS !in off }, leagues = leagues.takeIf { QuestionSource.LOCAL !in off }.orEmpty(), signs = signs, decades = decades, animals = animals, together = together, dateKinds = dateKinds, gentle = gentleReturn(today, history), firstWeekDay = firstWeek)
        val pinned = todayDoc?.getString("questionId")
        val question = when {
            pinned != null -> resolveQuestion(pinned) ?: draw()
            // A ready wildcard beats the capsule and the pool.
            ready.isNotEmpty() -> ready.first().asQuestion()
            capsuleSource != null -> capsuleSource!!.question
            else -> draw()
        }.forPlace(place, today).forSigns(you.birthday, partner.birthday)

        val todayEntry = DayEntry(
            date = today,
            question = question,
            answers = todayAnswers,
            reactions = todayDoc.toReactions(),
            notes = todayDoc.stringMap("notes"),
            moodReplies = todayDoc.stringMap("moodReplies"),
            talkedAbout = todayDoc.toTalkedAbout(),
            askAgain = todayDoc.toUidSet("askAgain"),
            opened = todayDoc?.getBoolean("revealed") == true,
            submitted = todayDoc.toSubmitted(),
            nextTheme = todayDoc?.getString("nextTheme"),
            nextThemeBy = todayDoc?.getString("nextThemeBy"),
        )

        val live = plates.filter { !it.isSealed }.maxByOrNull { it.startedOn }
            ?: BuildPlate.startingOn(today)

        // A week that has ended rolls over, placed-on or not: an unsealed past week used to
        // keep collecting bricks, and the first brick placed on it sealed it from a stale copy
        // of the plate, so that very brick was carried back to the tray. Seal from `live`.
        if (platesLoaded && live.isSealed.not() && live.endsOn() < today) {
            sealPlate(live)
        }

        val next = basis.copy(
            homeZone = coupleDoc?.getString("timeZone"),
            today = todayEntry,
            plate = live,
            // A week nobody built on rolls over without leaving an empty sculpture on the shelf.
            shelf = plates.filter { it.isSealed && it.placed.isNotEmpty() }.sortedByDescending { it.completedOn }
                .map { p -> myWords[p.id]?.takeIf { myUid !in p.words }?.let { p.copy(words = p.words + (myUid to it)) } ?: p },
            memories = memoryDays.values.sortedByDescending { it.date },
            anniversary = anniversary,
            streak = Streaks.run(history, todayEntry),
            // Only a capsule if today's question matches (a pin can override it).
            capsule = capsuleSource?.takeIf { it.question.id == question.id },
            wildcards = waiting,
            todayAuthorUid = question.id.takeIf { Wildcard.isWildcard(it) }
                ?.let { id -> allWildcards.firstOrNull { it.id == Wildcard.idOf(id) }?.byUid },
            written = allWildcards.groupingBy { it.byUid }.eachCount(),
            bucket = bucket,
            relationship = coupleDoc?.getString("relationship")
                ?.let { s -> RelationshipStatus.entries.firstOrNull { it.name == s } },
            marriedOn = marriedOn,
            yourHiddenTopics = hiddenByUid[myUid].orEmpty(),
            hiddenTopics = hidden,
            yourInterests = interestsByUid[myUid].orEmpty(),
            interests = interests,
            following = following,
            yourFollowing = followingByUid[myUid].orEmpty(),
            interestsAsked = me?.let { it.getBoolean("interestsAsked") == true },
            homeZip = homeZip,
            region = place?.region,
            homeState = place?.state,
            kept = (coupleDoc?.get("kept") as? Map<*, *>).orEmpty().entries.mapNotNull { (k, v) ->
                val label = (v as? Map<*, *>)?.get("label") as? String ?: return@mapNotNull null
                (k as? String)?.let { it to label }
            }.toMap(),
            photos = photos,
            recalled = recalledDocs.values.mapNotNull { it.toDayEntry(pool) { id, date -> resolveQuestion(id, date) } }
                .map { day -> pastAnswers[day.date]?.let { day.copy(answers = it) } ?: day },
            why = pool.why(question, today, anniversary, place?.state.takeIf { QuestionSource.HOLIDAYS !in off }, dateKinds, interests, together),
            sourcesOff = off,
            quietFrom = coupleDoc?.getLong("quietFrom")?.toInt() ?: 20,
            quietTo = coupleDoc?.getLong("quietTo")?.toInt() ?: 8,
            localTeams = place?.let { "${Teams.nearest(it, League.MLB).nickname} and ${Teams.nearest(it, League.NFL).nickname}" },
            weather = weather,
            specialDates = specialDates,
        )

        _state.value = next

        if (todayDoc?.getBoolean("revealed") == true) recordKnowing(todayEntry)
        // Awards run here because a reveal is often triggered by the partner, seen via a listener.
        if (platesLoaded) award(next)
    }

    /**
     * Question ids to skip: recent days, plus decks both partners finished in that window.
     * Requires both runs so each phone computes the same set.
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
     * Lays every brick [Rewards.due] says is owed. Appended with arrayUnion: several
     * awards can land from one snapshot, and writing `plate.blocks + fresh` made the
     * last write erase the others, so a brick dropped mid-fight was refused because it
     * had briefly vanished from the plate. Both phones call this; block ids dedupe.
     */
    private fun award(s: CoupleState) {
        val fresh = Rewards.due(s, today, pool.playable, plates)
        if (fresh.isEmpty()) return
        plateRef(s.plate).set(
            s.plate.toMap() + ("blocks" to FieldValue.arrayUnion(*fresh.map { it.toMap() }.toTypedArray())),
            SetOptions.merge(),
        )
    }

    private val runRegs = mutableMapOf<String, ListenerRegistration>()
    private val partnerRunRegs = mutableMapOf<String, ListenerRegistration>()

    /** quiz id -> uid -> run, so one listener per person cannot clobber the other. */
    private var runs: Map<String, Map<String, QuizRun>> = emptyMap()

    /** quiz id -> deck document. */
    private var quizDocs: Map<String, DocumentSnapshot> = emptyMap()

    private val instrumentRegs = mutableMapOf<String, ListenerRegistration>()

    /** instrument id -> uid -> responses, in published order, passes concatenated. */
    private var instrumentResponses: Map<String, Map<String, List<Int>>> = emptyMap()
    private var instrumentDocs: Map<String, DocumentSnapshot> = emptyMap()

    /** instrument id -> uid -> answered count, sealed or not. */
    private var instrumentProgress: Map<String, Map<String, Int>> = emptyMap()

    /** instrument id -> your draft responses. */
    private var instrumentDrafts: Map<String, List<Int>> = emptyMap()

    /** Watches your own draft. Rules deny reading the partner's; only the count on the parent is shared. */
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

    /** One listener per instrument and uid. */
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

    /** Requests reveal; a rules refusal means the partner has not submitted. */
    private fun liftInstrumentSealIfBothIn(id: String) {
        val doc = instrumentDocs[id] ?: return
        if (doc.getBoolean("revealed") == true) return
        if (instrumentResponses[id]?.get(myUid) == null) return

        openSeal(couple.collection(INSTRUMENTS).document(id))
    }

    /** Watches your own run by id; a query over runs is denied while sealed. */
    private fun watchRuns(quizId: String) {
        if (quizId in runRegs) return
        runRegs[quizId] = runRef(quizId, myUid)
            .addSnapshotListener { d, _ -> putRun(quizId, myUid, d) }
    }

    /** Watches the partner's run, after reveal. */
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

    /** Deck version of [liftSealIfBothIn]. */
    private fun liftQuizSealIfBothIn(quizId: String) {
        val quiz = quizDocs[quizId] ?: return
        if (quiz.getBoolean("revealed") == true) return
        if (runs[quizId]?.get(myUid) == null) return

        openSeal(couple.collection(QUIZZES).document(quizId))
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
        // Also ask here: the second answerer's first ask can precede its server write and be refused.
        liftSealIfBothIn()
        recompose()
    }

    // --- Writes --------------------------------------------------------------

    override fun bindLocalUser(uid: String, displayName: String) {
        // Keeps displayName current for the partner.
        db.collection(USERS).document(myUid).set(
            mapOf("displayName" to displayName),
            SetOptions.merge(),
        )
    }


    override fun submitToday(values: List<String>, guess: List<String>?) {
        if (_state.value.today.answerFor(myUid) != null) return

        // Pin the question on the day doc before writing the answer.
        dayRef.set(
            mapOf(
                "questionId" to _state.value.today.question.id,
                "revealed" to (todayDoc?.getBoolean("revealed") ?: false),
                // Informational only; reveal is decided by rules checking the answer docs.
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

        // Mark a wildcard used once it is answered, not when drawn.
        val asked = _state.value.today.question.id
        if (Wildcard.isWildcard(asked)) {
            couple.collection(WILDCARDS).document(Wildcard.idOf(asked))
                .set(mapOf("usedOn" to today.toString()), SetOptions.merge())
        }
    }

    /**
     * Requests today's reveal once you have answered. Rules refuse `revealed = true` unless
     * both answer docs exist, so a refusal just means "not yet". Do not trust the `submitted`
     * list for this; old day docs may never contain both uids.
     * Uses [openSeal], not a plain set. Ideally a Cloud Function.
     */
    private fun liftSealIfBothIn() {
        val doc = todayDoc ?: return
        if (doc.getBoolean("revealed") == true) return
        if (todayAnswers[myUid] == null) return

        // Store the streak on reveal so it survives past the loaded history. Count today as
        // opened, since state has not updated yet (otherwise it is one short).
        val s = _state.value
        val run = Streaks.run(s.history, s.today.copy(opened = true))
        openSeal(dayRef, mapOf("run" to run))
    }

    /** Paths with a reveal request in flight, to dedupe bursts of snapshots. */
    private val opening = mutableSetOf<String>()

    /**
     * Sets `revealed = true` on a day, deck or instrument doc in a transaction.
     * A plain set is applied to the local cache first, so a refusal rolls back and
     * re-triggers the listener in a loop (UI flicker). Retries twice with backoff,
     * since your own answer may not have reached the server yet.
     */
    private fun openSeal(ref: DocumentReference, extra: Map<String, Any> = emptyMap(), attempt: Int = 0) {
        if (attempt == 0 && !opening.add(ref.path)) return
        db.runTransaction { txn ->
            if (txn.get(ref).getBoolean("revealed") != true) {
                txn.set(ref, mapOf("revealed" to true) + extra, SetOptions.merge())
            }
            null
        }.addOnSuccessListener {
            opening.remove(ref.path)
        }.addOnFailureListener { e ->
            if (attempt < 2) {
                Handler(Looper.getMainLooper()).postDelayed({ openSeal(ref, extra, attempt + 1) }, 2000L shl attempt)
            } else {
                opening.remove(ref.path)
                Log.d(TAG, "not ready to open yet: ${ref.path}", e)
            }
        }
    }

    override fun noteWeek(plateId: String, text: String) {
        // Merge keeps the partner's note.
        couple.collection(PLATES).document(plateId)
            .set(mapOf("notes" to mapOf(myUid to text)), SetOptions.merge())
    }

    override fun setQuietHours(from: Int, to: Int) {
        couple.set(mapOf("quietFrom" to from, "quietTo" to to), SetOptions.merge())
    }

    override fun setSource(source: QuestionSource, on: Boolean) {
        couple.set(mapOf(source.field to on), SetOptions.merge())
    }

    /**
     * Your two words, sealed like an answer: one document each, readable only by
     * its author. The `onWords` function copies both onto the plate once both exist.
     */
    override fun nameWeek(plateId: String, words: NameWords) {
        if (myWords.containsKey(plateId)) return
        couple.collection(PLATES).document(plateId).collection(WORDS).document(myUid)
            .set(mapOf("describing" to words.describing, "thing" to words.thing))
        myWords = myWords + (plateId to words)
        recompose()
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

    /** Records each partner's guess hit for a revealed day. Keyed by uid, since both phones write it. */
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
     * On capsule days, looks for a revealed day 12, 6, 3, then 1 months ago and loads its answers.
     * See [capsuleDays] for how often.
     */
    /**
     * Today's capsule, in order: a day either of you asked for again a year ago,
     * then the very first question you answered on its anniversary, then the
     * weekday rota. The first two come on their date whatever the weekday, and
     * nothing announces them in advance.
     */
    private fun loadCapsule() {
        val yearAgo = today.minusYears(1)
        couple.collection(DAYS).document(yearAgo.toString()).get()
            .addOnSuccessListener { doc ->
                val question = resolveQuestion(doc.getString("questionId"), yearAgo)
                if (doc.toUidSet("askAgain").isNotEmpty() && doc.getBoolean("revealed") == true && question != null) {
                    readCapsuleAnswers(yearAgo, question, doc)
                } else {
                    firstQuestionAgain()
                }
            }
            .addOnFailureListener { firstQuestionAgain() }
    }

    /** The first day you both answered, if today is its anniversary (whole years only). */
    private fun firstQuestionAgain() {
        couple.collection(DAYS).whereEqualTo("revealed", true)
            .orderBy(FieldPath.documentId()).limit(1).get()
            .addOnSuccessListener { snap ->
                val first = snap.documents.firstOrNull()
                val date = first?.id.toDateOrNull()
                val question = date?.let { resolveQuestion(first?.getString("questionId"), it) }
                if (first != null && date != null && question != null &&
                    date.year < today.year && date.monthValue == today.monthValue && date.dayOfMonth == today.dayOfMonth
                ) {
                    readCapsuleAnswers(date, question, first)
                } else {
                    weekdayCapsule()
                }
            }
            .addOnFailureListener { weekdayCapsule() }
    }

    private fun weekdayCapsule() {
        if (today.dayOfWeek !in capsuleDays()) return
        tryCapsule(listOf(12L, 6L, 3L, 1L))
    }

    /**
     * Weekdays that may show a capsule, more as sealed plates (weeks) accumulate.
     * Uses plates because loaded history is capped at 70 days. Fixed sets so both phones agree.
     */
    private fun capsuleDays(): Set<DayOfWeek> {
        val weeks = plates.count { it.isSealed }
        return when {
            // Two years.
            weeks >= 104 -> setOf(
                DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY,
            )
            // About when the pool has cycled once.
            weeks >= 26 -> setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            weeks >= 12 -> setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY)
            else -> setOf(DayOfWeek.SUNDAY)
        }
    }

    /**
     * Today's date [Memories.MonthsBack] months ago, read by name with both answers,
     * so On this day can reach past the ten weeks of loaded history.
     */
    private fun loadMemories() {
        Memories.MonthsBack.map { today.minusMonths(it) }
            .filter { it.dayOfMonth == today.dayOfMonth }
            .forEach { date ->
                couple.collection(DAYS).document(date.toString()).get().addOnSuccessListener { doc ->
                    val question = resolveQuestion(doc.getString("questionId"), date)
                    if (!doc.exists() || doc.getBoolean("revealed") != true || question == null) return@addOnSuccessListener
                    val answers = couple.collection(DAYS).document(date.toString()).collection(ANSWERS)
                    answers.document(myUid).get().addOnSuccessListener { mine ->
                        answers.document(partnerUid).get().addOnSuccessListener { theirs ->
                            val both = listOfNotNull(mine.toAnswer(), theirs.toAnswer())
                            if (both.size < 2) return@addOnSuccessListener
                            memoryDays = memoryDays + (
                                date to DayEntry(
                                    date = date,
                                    question = question,
                                    answers = both.associateBy { it.uid },
                                    reactions = doc.toReactions(),
                                    opened = true,
                                )
                                )
                            recompose()
                        }
                    }
                }
            }
    }

    /**
     * Your own words for sealed weeks that have no name yet. Read by name: the
     * partner's are unreadable until the function copies both onto the plate.
     */
    private fun loadMyWords() {
        plates.filter { it.isSealed && myUid !in it.words && it.id !in myWordsAsked }.forEach { plate ->
            myWordsAsked = myWordsAsked + plate.id
            couple.collection(PLATES).document(plate.id).collection(WORDS).document(myUid).get()
                .addOnSuccessListener { doc ->
                    val describing = doc.getString("describing") ?: return@addOnSuccessListener
                    val thing = doc.getString("thing") ?: return@addOnSuccessListener
                    myWords = myWords + (plate.id to NameWords(describing, thing))
                    recompose()
                }
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

    /** Looks up a pool question or wildcard by id. */
    private fun resolveQuestion(id: String?, date: LocalDate = today): Question? = when {
        id == null -> null
        Wildcard.isWildcard(id) ->
            allWildcards.firstOrNull { it.id == Wildcard.idOf(id) }?.asQuestion()
        // Fill in team names here so past days show them too.
        else -> pool.question(id)?.forPlace(coupleDoc?.getString("homeZip")?.let { pool.places.lookup(it) }, date)
            ?.forSigns(_state.value.you.birthday, _state.value.them.birthday)
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
        // Merged by uid, leaving the partner's alone.
        couple.set(mapOf("hiddenTopics" to mapOf(myUid to mine.map { it.name })), SetOptions.merge())
    }

    override fun setMood(mood: Mood?, color: BrickColor?) {
        val now = System.currentTimeMillis()
        // Carry today's time in the previous mood, so the day's brick is the mood held longest.
        val prev = _state.value.you.mood?.takeIf { it.on == today }
        val spans = prev?.let { p ->
            p.spans + (p.mood to ((p.spans[p.mood] ?: 0) + (now - p.atMillis).coerceAtLeast(0)))
        }.orEmpty()
        db.collection(USERS).document(myUid).set(
            mapOf(
                "mood" to mood?.let {
                    mapOf(
                        "id" to it.name, "on" to today.toString(), "color" to color?.slug, "at" to now,
                        "spans" to spans.mapKeys { (k, _) -> k.name },
                    )
                },
            ),
            SetOptions.merge(),
        )
    }

    override fun setFollowing(leagues: Set<League>) {
        couple.set(mapOf("following" to mapOf(myUid to leagues.map { it.name })), SetOptions.merge())
        db.collection(USERS).document(myUid).set(mapOf("interestsAsked" to true), SetOptions.merge())
    }

    override fun setTopicInterest(topic: Topic, interested: Boolean) {
        val mine = _state.value.yourInterests.let { if (interested) it + topic else it - topic }
        couple.set(mapOf("interests" to mapOf(myUid to mine.map { it.name })), SetOptions.merge())
    }

    override fun setHomeZip(zip: String): Boolean {
        val place = pool.places.lookup(zip) ?: return false
        // Saved beside the ZIP so the morning date push runs at 8am local time.
        couple.set(mapOf("homeZip" to zip.trim(), "timeZone" to place.timeZone), SetOptions.merge())
        // Allow a new fetch if today's weather is not saved yet.
        weatherTried = false
        return true
    }

    /** Fetch weather at most once per repository instance. */
    private var weatherTried = false

    /**
     * Fetches today's forecast on a background thread and saves it on the day doc, so both
     * phones use the same weather. Transaction keeps the first write. Failures are ignored.
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

    override fun addSpecialDate(label: String, date: LocalDate, kind: DateKind) {
        couple.collection(DATES).add(mapOf("label" to label, "date" to date.toString(), "kind" to kind.name, "byUid" to myUid))
    }

    override fun removeSpecialDate(id: String) {
        couple.collection(DATES).document(id).delete()
    }

    override fun setColor(colorArgb: Long) {
        // Backstop: cannot take the partner's color.
        if (BrickColor.of(colorArgb) == BrickColor.of(_state.value.them.colorArgb)) return
        db.collection(USERS).document(myUid).set(
            mapOf("colorArgb" to colorArgb),
            SetOptions.merge(),
        )
    }

    /** Stored on the couple doc so both partners share it. */
    override fun setAnniversary(date: LocalDate) {
        couple.set(mapOf("anniversary" to date.toString()), SetOptions.merge())
    }

    override fun placeBlock(blockId: String, x: Int, y: Int) {
        val plate = _state.value.plate
        // Never onto a week that has ended; it is rolling over.
        val placement = plate.placement(blockId, x, y, today) ?: return

        // Appended, never rewritten: a whole-list write from a slightly stale plate
        // would erase a brick the other phone placed a moment ago.
        plateRef(plate).set(
            plate.toMap() + ("placed" to FieldValue.arrayUnion(placement.toMap())),
            SetOptions.merge(),
        )
    }

    override fun addPhoto(date: LocalDate, label: String, jpegBase64: String) {
        couple.collection(PHOTOS).add(
            mapOf("date" to date.toString(), "label" to label.take(80), "byUid" to myUid, "data" to jpegBase64),
        )
    }

    override fun noteDay(date: LocalDate, text: String) {
        couple.collection(DAYS).document(date.toString())
            .set(mapOf("notes" to mapOf(myUid to text.take(140))), SetOptions.merge())
    }

    override fun replyToMood(text: String) {
        dayRef.set(mapOf("moodReplies" to mapOf(myUid to text.take(80))), SetOptions.merge())
    }

    override fun keep(key: String, label: String?) {
        couple.update(
            FieldPath.of("kept", key),
            label?.let { mapOf("by" to myUid, "label" to it.take(200)) } ?: FieldValue.delete(),
        )
    }

    override fun loadAnswers(date: LocalDate) {
        if (date == today) return
        if (historyDocs.none { it.id == date.toString() } && date !in recalledDocs) {
            couple.collection(DAYS).document(date.toString()).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    recalledDocs = recalledDocs + (date to doc)
                    recompose()
                }
            }
        }
        val answers = couple.collection(DAYS).document(date.toString()).collection(ANSWERS)
        answers.document(myUid).get().addOnSuccessListener { mine ->
            // Theirs is only readable once the day is open; a refusal just leaves yours.
            answers.document(partnerUid).get()
                .addOnCompleteListener { theirs ->
                    val found = listOfNotNull(mine.toAnswer(), theirs.result?.takeIf { theirs.isSuccessful }?.toAnswer())
                    pastAnswers = pastAnswers + (date to found.associateBy { it.uid })
                    recompose()
                }
        }
    }

    override fun answerPast(date: LocalDate, questionId: String, values: List<String>, guess: List<String>?) {
        if (date >= today) return
        val ref = couple.collection(DAYS).document(date.toString())
        ref.set(mapOf("questionId" to questionId, "submitted" to FieldValue.arrayUnion(myUid)), SetOptions.merge())
        ref.collection(ANSWERS).document(myUid)
            .set(mapOf("values" to values, "guess" to guess, "submittedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener {
                openSeal(ref)
                loadAnswers(date)
            }
    }

    override fun react(date: LocalDate, reaction: Reaction?) {
        val ref = if (date == today) dayRef else couple.collection(DAYS).document(date.toString())
        ref.set(
            mapOf("reactions" to mapOf(myUid to reaction?.name)),
            SetOptions.merge(),
        )
    }

    /** Uses arrayUnion so concurrent marks do not clobber each other. */
    override fun askAgain(date: LocalDate, ask: Boolean) {
        val ref = if (date == today) dayRef else couple.collection(DAYS).document(date.toString())
        ref.set(
            mapOf("askAgain" to if (ask) FieldValue.arrayUnion(myUid) else FieldValue.arrayRemove(myUid)),
            SetOptions.merge(),
        )
    }

    override fun markTalkedAbout(date: LocalDate) {
        val ref = if (date == today) dayRef else couple.collection(DAYS).document(date.toString())
        ref.set(
            mapOf("talkedAbout" to FieldValue.arrayUnion(myUid)),
            SetOptions.merge(),
        )
    }

    /** Writes tomorrow's theme if it is your turn. Tomorrow ignores a pick by the wrong person. */
    override fun pickTomorrow(theme: String) {
        val day = _state.value.today
        if (!Turns.canPick(day, myUid, partnerUid)) return
        dayRef.set(
            mapOf("nextTheme" to theme, "nextThemeBy" to myUid),
            SetOptions.merge(),
        )
    }

    /** Plates already sealed by this phone, so a recompose before the snapshot doesn't seal twice. */
    private val sealing = mutableSetOf<String>()

    /**
     * Seals [plate] as it is now, not as the last published state saw it, and
     * starts the next week with whatever wasn't placed.
     */
    private fun sealPlate(plate: BuildPlate) {
        if (plate.isSealed || !sealing.add(plate.id)) return

        plateRef(plate).set(
            plate.toMap() + ("completedOn" to today.toString()),
            SetOptions.merge(),
        )
        // Unplaced bricks carry over.
        val carried = plate.unplaced()
        val next = BuildPlate.startingOn(today, carried)
        // Date-based id, so two phones sealing at once write the same next plate,
        // and appended, so neither phone's carried bricks erase the other's.
        plateRef(next).set(
            if (carried.isEmpty()) next.toMap()
            else next.toMap() + ("blocks" to FieldValue.arrayUnion(*carried.map { it.toMap() }.toTypedArray())),
            SetOptions.merge(),
        )
    }

    /** Writes your run as its own doc. Rules hide the partner's until the deck is revealed. */
    override fun submitQuizRun(
        quizId: String,
        answers: Map<String, List<String>>,
        guesses: Map<String, List<String>>,
    ) {
        if (_state.value.myRun(quizId) != null) return
        val quiz = couple.collection(QUIZZES).document(quizId)
        val doc = quizDocs[quizId]

        quiz.set(
            mapOf(
                "revealed" to (doc?.getBoolean("revealed") ?: false),
                "submitted" to FieldValue.arrayUnion(myUid),
                // Fixed by whoever plays first; the rules refuse changing it later.
                "sealed" to (doc?.getBoolean("sealed") ?: (pool.deck(quizId)?.sealed ?: true)),
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
        val doc = couple.collection(INSTRUMENTS).document(instrumentId)
        // Keep the current value; writing false on a retake would re-seal it.
        val revealed = instrumentDocs[instrumentId]?.getBoolean("revealed") ?: false

        doc.set(
            mapOf(
                "revealed" to revealed,
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
        // Delete the draft now that answers are submitted.
        doc.collection(DRAFTS).document(myUid).delete()
    }

    override fun saveInstrumentDraft(instrumentId: String, responses: List<Int>) {
        val doc = couple.collection(INSTRUMENTS).document(instrumentId)
        val revealed = instrumentDocs[instrumentId]?.getBoolean("revealed") ?: false

        doc.set(
            mapOf(
                "revealed" to revealed,
                // Count only; this is visible to the partner.
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
}
