package com.ampersand.data

import com.ampersand.model.Answer
import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Placement
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Reaction
import com.ampersand.model.Rewards
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * The seam Firebase will slot into.
 *
 * Everything above this interface is pure UI, so the Firestore implementation
 * can replace [FakeCoupleRepository] without touching a screen. The sealed
 * behaviour is modelled here rather than in the UI on purpose: when this is
 * backed by Firestore, the partner's answer simply will not be readable until
 * security rules allow it, and the UI already handles that state.
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

    /** Same, for the person you are paired with. */
    fun bindPartner(uid: String, displayName: String)

    /** Submit your own answer for today. Irreversible, by design. */
    fun submitToday(values: List<String>, guess: List<String>?)

    fun setColor(uid: String, colorArgb: Long)

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
     * Driven by the calendar in the real implementation -- the period ends and
     * the sculpture is dated whether or not anyone opened the app. Exposed here
     * because a thing that happens once a week is otherwise impossible to see
     * while designing it.
     */
    fun sealPlate()

    // --- Scaffolding-only controls -------------------------------------------
    // These exist so the UI can be designed on one device with no backend.
    // They go away when the Firestore implementation lands.

    /** Fakes the partner submitting, so the reveal transition can be seen. */
    fun simulatePartnerAnswer()

    /** Fakes the partner reacting, so both halves of a reveal can be seen. */
    fun simulatePartnerReaction(date: LocalDate)

    /** Swaps today's question, to design each input style quickly. */
    fun previewQuestion(question: Question)

    /** Back to an unanswered day. */
    fun resetToday()
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
): CoupleRepository =
    if (hasFirebase && !coupleId.startsWith("c_fake")) {
        FirestoreCoupleRepository(coupleId, myUid, partnerUid)
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

    override fun bindPartner(uid: String, displayName: String) = mutate { current ->
        if (current.them.uid == uid && current.them.name == displayName) return@mutate current
        current.rebind(current.them.uid, uid) {
            copy(them = them.copy(uid = uid, name = displayName))
        }
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

    override fun simulatePartnerAnswer() = mutate { current ->
        val them = current.them
        val today = current.today
        if (today.answerFor(them.uid) != null) return@mutate current

        val q = today.question
        awardIfComplete(
            current.copy(
                today = today.copy(
                    answers = today.answers +
                        (them.uid to Answer(them.uid, cannedResponse(q), cannedGuess(q))),
                ),
            ),
        )
    }

    /**
     * Both answered, so both earn a brick.
     *
     * Idempotent: block ids are `date_uid`, so running this again on an
     * already-rewarded day is a no-op. That matters because it is called from
     * both submit paths and will be called from a Firestore listener later,
     * where the same day can arrive more than once.
     */
    private fun awardIfComplete(s: CoupleState): CoupleState {
        if (!s.today.isRevealed) return s
        val existing = s.plate.blocks.mapTo(mutableSetOf()) { it.id }
        val fresh = (
            Rewards.forDay(s.today, s.you, s.streak) +
                Rewards.forDay(s.today, s.them, s.streak)
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

    override fun simulatePartnerReaction(date: LocalDate) = mutate { current ->
        val uid = current.them.uid
        current.withDay(date) { day ->
            if (!day.isRevealed) {
                day
            } else {
                day.copy(
                    reactions = if (uid in day.reactions) {
                        day.reactions - uid
                    } else {
                        day.reactions + (uid to Reaction.entries.random())
                    },
                )
            }
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

    override fun setColor(uid: String, colorArgb: Long) = mutate { current ->
        when (uid) {
            current.you.uid -> current.copy(you = current.you.copy(colorArgb = colorArgb))
            current.them.uid -> current.copy(them = current.them.copy(colorArgb = colorArgb))
            else -> current
        }
    }

    override fun previewQuestion(question: Question) = mutate { current ->
        current.copy(today = current.today.copy(question = question, answers = emptyMap()))
    }

    override fun resetToday() = mutate { current ->
        current.copy(today = current.today.copy(answers = emptyMap()))
    }

    /** Plausible partner answers, shaped correctly for each question type. */
    private fun cannedResponse(q: Question): List<String> = when (q.type) {
        QuestionType.REFLECT, QuestionType.GUESS_ME ->
            listOf("The shoebox of photos under the bed.")

        QuestionType.RANK -> q.options.reversed()
        QuestionType.MULTI_SELECT -> q.options.take(q.maxChoices ?: 2)
        QuestionType.SCALE -> listOf("4")
        else -> listOf(q.options.lastOrNull().orEmpty())
    }

    private fun cannedGuess(q: Question): List<String>? = when (q.type) {
        QuestionType.REFLECT -> null
        QuestionType.GUESS_ME -> listOf("Probably your laptop, knowing you.")
        QuestionType.RANK -> q.options
        QuestionType.MULTI_SELECT -> q.options.takeLast(q.maxChoices ?: 2)
        QuestionType.SCALE -> listOf("3")
        else -> listOf(q.options.firstOrNull().orEmpty())
    }

    private inline fun mutate(block: (CoupleState) -> CoupleState) {
        _state.value = block(_state.value)
    }
}
