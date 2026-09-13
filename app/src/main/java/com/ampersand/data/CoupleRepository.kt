package com.ampersand.data

import com.ampersand.model.NameWords
import com.ampersand.model.DateKind
import com.ampersand.model.MoodEntry
import com.ampersand.model.Mood
import com.ampersand.model.League
import com.ampersand.model.SpecialDate
import com.ampersand.model.Topic
import com.ampersand.model.RelationshipStatus
import com.ampersand.model.BucketList
import com.ampersand.model.BucketItem
import com.ampersand.model.BucketGroup
import com.ampersand.model.BrickColor
import com.ampersand.model.Answer
import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.InstrumentRun
import com.ampersand.model.Placement
import com.ampersand.model.QuizRun
import com.ampersand.model.QuestionType
import com.ampersand.model.Reaction
import com.ampersand.model.Wildcard
import com.ampersand.model.Rewards
import com.ampersand.model.Turns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Data operations for a couple. Firestore on device; [FakeCoupleRepository] for previews.
 * The partner's answer stays hidden until reveal because Firestore rules block reading it.
 */
interface CoupleRepository {

    val state: StateFlow<CoupleState>

    /** Binds the local member to the signed-in account. Idempotent (called from composition). */
    fun bindLocalUser(uid: String, displayName: String)

    /** Submits your answer for today. Cannot be changed. */
    fun submitToday(values: List<String>, guess: List<String>?)

    /** Sets your own color. Rules block writes to the partner's document. */
    fun setColor(colorArgb: Long)

    /** Either partner may set it. */
    fun setAnniversary(date: LocalDate)

    /** Writes your own member document only. */
    fun setProfile(gender: String?, birthday: LocalDate?)

    /** Either partner may set it. */
    fun setRelationship(status: RelationshipStatus, marriedOn: LocalDate?)

    /** Hides or unhides a topic for you; the draw uses both partners' settings. */
    fun setTopicHidden(topic: Topic, hidden: Boolean)

    /** Sets today's mood (and optional color). Null clears it. */
    fun setMood(mood: Mood?, color: BrickColor? = null)

    /** Sets followed leagues and marks interests as asked. Empty means none. */
    fun setFollowing(leagues: Set<League>)

    /** Adds or removes a topic interest for you; the draw uses both partners' settings. */
    fun setTopicInterest(topic: Topic, interested: Boolean)

    /** Saves the home ZIP. Returns false (and saves nothing) if the ZIP is not in the bundled table. */
    fun setHomeZip(zip: String): Boolean

    fun addSpecialDate(label: String, date: LocalDate, kind: DateKind)

    fun removeSpecialDate(id: String)

    /** Places an earned brick on the plate. Ignored if the spot is illegal. */
    fun placeBlock(blockId: String, x: Int, y: Int)

    /** Sets or clears your reaction to a revealed day. Unlike answers, reactions can change. */
    fun react(date: LocalDate, reaction: Reaction?)

    /** Seals the current plate and starts the next. Firestore calls this itself when the period ends. */
    fun sealPlate()

    /**
     * Submits your deck run. Cannot be changed.
     * [answers] and [guesses] are keyed by question id, since a deck can change between partners' runs.
     */
    fun submitQuizRun(quizId: String, answers: Map<String, List<String>>, guesses: Map<String, List<String>>)

    /** Queues your own question; it is asked on a later day, not immediately. */
    fun addWildcard(text: String, type: QuestionType, options: List<String>)

    /**
     * Submits a full instrument sitting; submitting again replaces your earlier answers.
     * [responses] is every item in published order, all passes concatenated
     * (`Instrument.length` values, each 1..5).
     */
    fun submitInstrument(instrumentId: String, responses: List<Int>)

    /**
     * Saves a partial sitting (called per page, not per tap).
     * [responses] is the full list with 0 for unanswered. Rules hide the draft from the
     * partner; only the answered count is visible to them.
     */
    fun saveInstrumentDraft(instrumentId: String, responses: List<Int>)

    /** Marks that a revealed day was talked about. Cannot be undone. */
    fun markTalkedAbout(date: LocalDate)

    /** Sets your note on a sealed week. Can be rewritten. */
    fun noteWeek(plateId: String, text: String)

    /** Sets your two words toward a sealed week's name. */
    fun nameWeek(plateId: String, words: NameWords)

    fun addToBucket(text: String, group: BucketGroup)

    /** Marks an item done and awards each partner a brick. Cannot be undone. */
    fun tickBucket(id: String)

    /** Removes an item. Only your own, and only if not done. */
    fun removeFromBucket(id: String)

    /** Picks tomorrow's theme. Only on your turn, after today is revealed, once. See [com.ampersand.model.Turns]. */
    fun pickTomorrow(theme: String)
}

/** Firestore when configured and the couple is real, the fake otherwise. Same test as the auth and pairing factories. */
fun createCoupleRepository(
    hasFirebase: Boolean,
    coupleId: String,
    myUid: String,
    partnerUid: String,
    pool: QuestionPool,
): CoupleRepository =
    if (hasFirebase && !coupleId.startsWith("c_fake")) {
        FirestoreCoupleRepository(coupleId, myUid, partnerUid, pool)
    } else {
        FakeCoupleRepository()
    }

/** In-memory repository seeded from [MockCouple]. */
class FakeCoupleRepository(
    initial: CoupleState = MockCouple.state(),
) : CoupleRepository {

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<CoupleState> = _state.asStateFlow()

    /**
     * Rewrites the local member's uid everywhere it appears in the mock data: answer keys,
     * brick owners, and `date_uid` block ids. Changing only the member would orphan history.
     */
    override fun bindLocalUser(uid: String, displayName: String) = mutate { current ->
        if (current.you.uid == uid && current.you.name == displayName) return@mutate current
        current.rebind(current.you.uid, uid) { copy(you = you.copy(uid = uid, name = displayName)) }
    }

    private fun CoupleState.rebind(
        old: String,
        uid: String,
        setMember: CoupleState.() -> CoupleState,
    ): CoupleState {
        fun remap(id: String) = if (id == old) uid else id

        fun DayEntry.rebind() = copy(
            answers = answers.mapKeys { remap(it.key) }
                .mapValues { (_, a) -> a.copy(uid = remap(a.uid)) },
            reactions = reactions.mapKeys { remap(it.key) },
        )

        fun BuildPlate.rebind() = copy(
            blocks = blocks.map { b ->
                b.copy(
                    // Block ids are `date_uid` (the award idempotency key), so remap them too.
                    id = b.id.removeSuffix("_$old").let { if (it == b.id) b.id else "${it}_$uid" },
                    ownerUid = remap(b.ownerUid),
                )
            },
            placed = placed.map { p ->
                p.copy(blockId = p.blockId.removeSuffix("_$old").let {
                    if (it == p.blockId) p.blockId else "${it}_$uid"
                })
            },
        )

        return setMember().copy(
            today = today.rebind(),
            history = history.map { it.rebind() },
            plate = plate.rebind(),
            shelf = shelf.map { it.rebind() },
            // Instrument answers are keyed by uid too.
            instrumentRuns = instrumentRuns.map { run ->
                run.copy(answers = run.answers.mapKeys { remap(it.key) })
            },
        )
    }

    override fun submitToday(values: List<String>, guess: List<String>?) = mutate { current ->
        val you = current.you
        val today = current.today
        if (today.answerFor(you.uid) != null) return@mutate current
        awardIfComplete(
            current.copy(
                today = today.copy(
                    answers = today.answers + (you.uid to Answer(you.uid, values, guess)),
                ),
            ),
        )
    }

    /** Awards both partners' bricks once today is revealed. Idempotent via `date_uid` block ids. */
    private fun awardIfComplete(s: CoupleState): CoupleState {
        if (!s.today.isRevealed) return s
        val existing = s.plate.blocks.mapTo(mutableSetOf()) { it.id }
        val fresh = (
            Rewards.forDay(s.today, s.you, s.them, s.streak) +
                Rewards.forDay(s.today, s.them, s.you, s.streak)
            ).filter { it.id !in existing }
        if (fresh.isEmpty()) return s
        return s.copy(plate = s.plate.copy(blocks = s.plate.blocks + fresh))
    }

    override fun react(date: LocalDate, reaction: Reaction?) = mutate { current ->
        val uid = current.you.uid
        current.withDay(date) { day ->
            // Only revealed days take reactions.
            if (!day.isRevealed) day
            else day.copy(
                reactions = if (reaction == null) {
                    day.reactions - uid
                } else {
                    day.reactions + (uid to reaction)
                },
            )
        }
    }

    /** Updates the day for [date] whether it is today or already in history. */
    private fun CoupleState.withDay(date: LocalDate, f: (DayEntry) -> DayEntry): CoupleState =
        if (today.date == date) {
            copy(today = f(today))
        } else {
            copy(history = history.map { if (it.date == date) f(it) else it })
        }

    override fun sealPlate() = mutate { current ->
        val plate = current.plate
        if (plate.isSealed || plate.placed.isEmpty()) return@mutate current

        current.copy(
            shelf = listOf(plate.copy(completedOn = current.today.date)) + current.shelf,
            plate = BuildPlate(
                id = "plate_${current.shelf.size + 2}",
                startedOn = current.today.date,
                periodDays = plate.periodDays,
                gridW = plate.gridW,
                gridH = plate.gridH,
                // Unplaced bricks carry over to the new plate.
                blocks = plate.tray(current.you.uid) + plate.tray(current.them.uid),
            ),
        )
    }

    override fun placeBlock(blockId: String, x: Int, y: Int) = mutate { current ->
        val plate = current.plate
        if (plate.isSealed) return@mutate current
        val block = plate.block(blockId) ?: return@mutate current
        // Validated here (not in UI) so Firestore can run the same check in a transaction.
        if (plate.placed.any { it.blockId == blockId }) return@mutate current
        if (!plate.canPlace(block.shape, x, y)) return@mutate current

        current.copy(
            plate = plate.copy(placed = plate.placed + Placement(blockId, x, y)),
        )
    }

    override fun submitQuizRun(
        quizId: String,
        answers: Map<String, List<String>>,
        guesses: Map<String, List<String>>,
    ) = mutate { current ->
        if (current.myRun(quizId) != null) return@mutate current
        current.copy(
            quizRuns = current.quizRuns +
                QuizRun(quizId, current.you.uid, answers, guesses, System.currentTimeMillis()),
        )
    }

    override fun setColor(colorArgb: Long) = mutate { current ->
        if (BrickColor.of(colorArgb) == BrickColor.of(current.them.colorArgb)) return@mutate current
        current.copy(you = current.you.copy(colorArgb = colorArgb))
    }

    override fun setAnniversary(date: LocalDate) = mutate { it.copy(anniversary = date) }

    override fun setProfile(gender: String?, birthday: LocalDate?) = mutate {
        it.copy(you = it.you.copy(gender = gender, birthday = birthday))
    }

    override fun setRelationship(status: RelationshipStatus, marriedOn: LocalDate?) = mutate {
        it.copy(relationship = status, marriedOn = marriedOn.takeIf { status == RelationshipStatus.MARRIED })
    }

    override fun setTopicHidden(topic: Topic, hidden: Boolean) = mutate {
        val mine = if (hidden) it.yourHiddenTopics + topic else it.yourHiddenTopics - topic
        it.copy(yourHiddenTopics = mine, hiddenTopics = mine)
    }

    override fun setMood(mood: Mood?, color: BrickColor?) = mutate {
        it.copy(you = it.you.copy(mood = mood?.let { m -> MoodEntry(m, it.today.date, color) }))
    }

    override fun setFollowing(leagues: Set<League>) = mutate {
        it.copy(yourFollowing = leagues, following = leagues, interestsAsked = true)
    }

    override fun setTopicInterest(topic: Topic, interested: Boolean) = mutate {
        val mine = if (interested) it.yourInterests + topic else it.yourInterests - topic
        it.copy(yourInterests = mine, interests = mine)
    }

    override fun setHomeZip(zip: String): Boolean {
        val clean = zip.trim()
        if (clean.length != 5 || !clean.all(Char::isDigit)) return false
        mutate { it.copy(homeZip = clean) }
        return true
    }

    override fun addSpecialDate(label: String, date: LocalDate, kind: DateKind) = mutate {
        it.copy(specialDates = it.specialDates + SpecialDate("sd${it.specialDates.size + 1}", label, date, kind))
    }

    override fun removeSpecialDate(id: String) = mutate {
        it.copy(specialDates = it.specialDates.filterNot { d -> d.id == id })
    }

    override fun addWildcard(text: String, type: QuestionType, options: List<String>) =
        mutate { current ->
            current.copy(
                wildcards = current.wildcards + Wildcard(
                    id = "wc${current.wildcards.size + 1}",
                    text = text,
                    type = type,
                    options = options,
                    byUid = current.you.uid,
                    byName = current.you.name,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        }

    override fun noteWeek(plateId: String, text: String) = mutate { current ->
        current.copy(
            shelf = current.shelf.map {
                if (it.id == plateId && it.isSealed) it.copy(notes = it.notes + (current.you.uid to text)) else it
            },
        )
    }

    override fun nameWeek(plateId: String, words: NameWords) = mutate { current ->
        current.copy(
            shelf = current.shelf.map {
                if (it.id == plateId && it.isSealed) it.copy(words = it.words + (current.you.uid to words)) else it
            },
        )
    }

    override fun addToBucket(text: String, group: BucketGroup) = mutate { current ->
        current.copy(
            bucket = current.bucket + BucketItem(
                id = "b${current.bucket.size + 1}",
                text = text,
                group = group,
                byUid = current.you.uid,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    override fun tickBucket(id: String) = mutate { current ->
        val item = current.bucket.firstOrNull { it.id == id && !it.isDone } ?: return@mutate current
        val ticked = item.copy(doneOn = current.today.date)
        val laid = (current.shelf.flatMap { it.blocks } + current.plate.blocks).mapTo(mutableSetOf()) { it.id }
        val fresh = listOfNotNull(BucketList.reward(ticked, current.you), BucketList.reward(ticked, current.them))
            .filter { it.id !in laid }
        current.copy(
            bucket = current.bucket.map { if (it.id == id) ticked else it },
            plate = if (current.plate.isSealed) current.plate else current.plate.copy(blocks = current.plate.blocks + fresh),
        )
    }

    override fun removeFromBucket(id: String) = mutate { current ->
        current.copy(bucket = current.bucket.filterNot { it.id == id && it.byUid == current.you.uid && !it.isDone })
    }

    override fun markTalkedAbout(date: LocalDate) = mutate { current ->
        fun DayEntry.mark() =
            if (this.date != date || !isRevealed) this
            else copy(talkedAbout = talkedAbout + current.you.uid)

        val marked = current.copy(
            today = current.today.mark(),
            history = current.history.map { it.mark() },
        )
        val day = (listOf(marked.today) + marked.history).firstOrNull { it.date == date }
            ?: return@mutate marked
        if (marked.plate.isSealed) return@mutate marked

        val laid = (marked.shelf.flatMap { it.blocks } + marked.plate.blocks).mapTo(mutableSetOf()) { it.id }
        val fresh = (
            Rewards.forTalk(day, marked.you, marked.them, marked.today.date) +
                Rewards.forTalk(day, marked.them, marked.you, marked.today.date)
            ).filter { it.id !in laid }
        marked.copy(plate = marked.plate.copy(blocks = marked.plate.blocks + fresh))
    }

    override fun pickTomorrow(theme: String) = mutate { current ->
        if (!Turns.canPick(current.today, current.you.uid, current.them.uid)) return@mutate current
        current.copy(today = current.today.copy(nextTheme = theme, nextThemeBy = current.you.uid))
    }

    override fun saveInstrumentDraft(instrumentId: String, responses: List<Int>) =
        mutate { current ->
            val existing = current.instrumentRuns.lastOrNull { it.instrumentId == instrumentId }
            val updated = (existing ?: InstrumentRun(instrumentId)).let {
                it.copy(
                    draft = responses,
                    progress = it.progress + (current.you.uid to responses.count { v -> v > 0 }),
                )
            }
            current.copy(
                instrumentRuns = current.instrumentRuns.filterNot { it === existing } + updated,
            )
        }

    override fun submitInstrument(instrumentId: String, responses: List<Int>) =
        mutate { current ->
            val existing = current.instrumentRuns.lastOrNull { it.instrumentId == instrumentId }
            val updated = (existing ?: InstrumentRun(instrumentId)).let {
                it.copy(
                    answers = it.answers + (current.you.uid to responses),
                    progress = it.progress + (current.you.uid to responses.size),
                    draft = emptyList(),
                )
            }
            current.copy(
                instrumentRuns = current.instrumentRuns.filterNot { it === existing } + updated,
            )
        }

    private inline fun mutate(block: (CoupleState) -> CoupleState) {
        _state.value = block(_state.value)
    }
}
