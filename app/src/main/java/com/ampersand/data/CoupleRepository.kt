package com.ampersand.data

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
 * Everything the screens can ask of a couple.
 *
 * Firestore backs it on a real phone; [FakeCoupleRepository] backs previews.
 * The sealed behaviour is modelled here rather than in the UI on purpose: in
 * Firestore the partner's answer is simply unreadable until the rules allow
 * it, and the UI already handles that state.
 */
interface CoupleRepository {

    val state: StateFlow<CoupleState>

    /**
     * Point the local half of the couple at the signed-in account.
     *
     * Idempotent, because it is called from a composition that can re-run for
     * reasons that have nothing to do with the account changing.
     */
    fun bindLocalUser(uid: String, displayName: String)

    /** Submit your own answer for today. Irreversible, by design. */
    fun submitToday(values: List<String>, guess: List<String>?)

    /**
     * Only your own. A colour is part of a profile, and the rules refuse a
     * write to your partner's document anyway -- so the UI offering to change
     * it was offering something that could not happen.
     */
    fun setColor(colorArgb: Long)

    /** The day the two of you started. Either of you may set it. */
    fun setAnniversary(date: LocalDate)

    /** About you. Your own document, so only you. */
    fun setProfile(gender: String?, birthday: LocalDate?)

    /** About the two of you. Either of you. */
    fun setRelationship(status: RelationshipStatus, marriedOn: LocalDate?)

    /** Take a topic out of the daily question, or put it back. Yours; the draw uses both. */
    fun setTopicHidden(topic: Topic, hidden: Boolean)

    /** The sports you follow, and that you have been asked. Empty means none. */
    fun setFollowing(leagues: Set<League>)

    /** Ask for more of a topic, or stop asking. Yours; the draw uses both. */
    fun setTopicInterest(topic: Topic, interested: Boolean)

    /**
     * The couple's home ZIP, for weather and regional questions. False when the
     * ZIP is not one the bundled table knows, and nothing is saved.
     */
    fun setHomeZip(zip: String): Boolean

    fun addSpecialDate(label: String, date: LocalDate)

    fun removeSpecialDate(id: String)

    /** Put an earned brick on the shared plate. Ignored if the spot is illegal. */
    fun placeBlock(blockId: String, x: Int, y: Int)

    /**
     * Set or clear your reaction to a revealed day.
     *
     * Changeable, unlike an answer: an answer is a statement made under seal
     * and editing it would undo the whole mechanic, whereas a reaction is a
     * gesture and people change their minds about those.
     */
    fun react(date: LocalDate, reaction: Reaction?)

    /**
     * Seal the current plate and start the next one.
     *
     * Driven by the calendar: the Firestore implementation calls this itself
     * once the period has run out, so a sculpture is dated whether or not
     * anyone opened the app that day.
     */
    fun sealPlate()

    /**
     * Submit your run through a deck. Irreversible, like a day.
     *
     * [answers] and [guesses] are keyed by question id rather than positional,
     * because a deck can gain or lose a question between you starting it and
     * your partner finishing it, and an index would then pair up the wrong two
     * answers.
     */
    fun submitQuizRun(quizId: String, answers: Map<String, List<String>>, guesses: Map<String, List<String>>)

    /**
     * Write a question of your own for the two of you.
     *
     * Queued rather than asked immediately: a question that arrives the instant
     * it is written is a message, and this app deliberately has no messages.
     * Waiting until tomorrow is what makes it a question.
     */
    fun addWildcard(text: String, type: QuestionType, options: List<String>)

    /**
     * Submit a whole sitting with one instrument. Irreversible, like a day.
     *
     * [responses] is every item, in the published order, every pass
     * concatenated -- `Instrument.length` of them, each 1..5. Positional rather
     * than keyed, because unlike a deck an instrument's item list cannot change
     * between the two of you taking it: changing it would make the scoring key
     * wrong and the citation on it false.
     */
    fun submitInstrument(instrumentId: String, responses: List<Int>)

    /**
     * Keep a half-finished sitting, so leaving does not throw it away.
     *
     * Written as each page is turned rather than on every tap: a page is the
     * unit somebody actually commits to, and forty writes for forty ratings is
     * a lot of network for a form nobody is racing.
     *
     * [responses] is the whole list with zeros where nothing has been answered
     * yet. The draft is yours alone -- the rules make it unreadable to your
     * partner, because unsealed answers are exactly what the seal is for. What
     * they *can* see is the count, which is written alongside it and gives away
     * nothing about the answers.
     */
    fun saveInstrumentDraft(instrumentId: String, responses: List<Int>)

    /**
     * Say that a revealed day came up again, away from the phone.
     *
     * The one thing the app most wants to know and has never been able to see.
     * Additive and un-take-backable by design: this is a note that something
     * happened, and there is no version of "actually we did not talk about it
     * after all" worth building a control for.
     */
    fun markTalkedAbout(date: LocalDate)

    /** Your one line about a sealed week. Rewritable until you are happy with it. */
    fun noteWeek(plateId: String, text: String)

    /** Put something on the list the two of you share. */
    fun addToBucket(text: String, group: BucketGroup)

    /** Tick it off. Earns a brick each; un-ticking is not a thing. */
    fun tickBucket(id: String)

    /** Take it off again. Only what you added, and only while it is not done. */
    fun removeFromBucket(id: String)

    /**
     * Pick what tomorrow's question is about. Only works on your turn, once
     * today is open, and only once. See [com.ampersand.model.Turns].
     */
    fun pickTomorrow(theme: String)
}

/**
 * Firestore when there is a project and a real couple behind it, the mock
 * otherwise.
 *
 * The same test as the auth and pairing factories, so the three never disagree
 * about which world the app is running in.
 */
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

/** In-memory, deterministic, and seeded from [MockCouple]. */
class FakeCoupleRepository(
    initial: CoupleState = MockCouple.state(),
) : CoupleRepository {

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<CoupleState> = _state.asStateFlow()

    /**
     * Rewrite the local member, and every reference to them, to the real uid.
     *
     * The rewrite is the whole job. `you.uid` appears as a map key on every
     * answer, as `ownerUid` on every brick, and inside block ids of the form
     * `date_uid` -- so changing the member alone would leave ten weeks of
     * history belonging to a user who no longer exists, and the plate would
     * show your bricks as your partner's. Renaming without remapping is the
     * obvious version of this and it silently guts the mock data.
     *
     * Firestore will not need any of this: there the uid is the uid from the
     * first write. It exists because the sample couple was invented before
     * anyone had signed in.
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
                    // Block ids are `date_uid` and are the idempotency key for
                    // awarding, so they have to move with the owner.
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
            // Responses are keyed by uid too, so a seeded sitting would belong
            // to a user who no longer exists and the badge wall would read as
            // half-finished the moment somebody signed in.
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

    /**
     * Both answered, so both earn a brick.
     *
     * Idempotent: block ids are `date_uid`, so running this again on an
     * already-rewarded day is a no-op. That matters because it is called from
     * both submit paths, and the Firestore version runs from listeners where
     * the same day can arrive more than once.
     */
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
            // Nothing to react to until the seal has lifted.
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

    /**
     * A day lives in exactly one of two places, and callers should not have to
     * know which. Today graduates into history at the day boundary, so the same
     * reaction call has to work either side of midnight.
     */
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
                // Unplaced bricks carry over. They were earned by answering,
                // and confiscating them would punish you for not having tidied
                // the tray before a deadline you did not choose.
                blocks = plate.tray(current.you.uid) + plate.tray(current.them.uid),
            ),
        )
    }

    override fun placeBlock(blockId: String, x: Int, y: Int) = mutate { current ->
        val plate = current.plate
        if (plate.isSealed) return@mutate current
        val block = plate.block(blockId) ?: return@mutate current
        // Already down, or not a legal spot: leave the plate alone. Validating
        // here rather than in the UI means the Firestore version can run the
        // same check inside a transaction and reject a race the same way.
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

    override fun addSpecialDate(label: String, date: LocalDate) = mutate {
        it.copy(specialDates = it.specialDates + SpecialDate("sd${it.specialDates.size + 1}", label, date))
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
