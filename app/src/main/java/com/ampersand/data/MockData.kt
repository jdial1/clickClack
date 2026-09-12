package com.ampersand.data

import com.ampersand.model.Answer
import com.ampersand.model.Block
import com.ampersand.model.BlockFace
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.CharacterShape
import com.ampersand.model.Slope
import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Depth
import com.ampersand.model.Member
import com.ampersand.model.Placement
import com.ampersand.model.Question
import com.ampersand.model.QuizRun
import com.ampersand.model.QuestionType
import com.ampersand.model.Reaction
import java.time.LocalDate
import kotlin.random.Random

/**
 * Stand-in for the real question pool, which will be ingested into Firestore.
 * Shaped exactly like the eventual `questions/{id}` documents so swapping the
 * source is a repository change and not a UI change.
 */
object MockQuestions {

    val pool: List<Question> = listOf(
        // --- REFLECT ---------------------------------------------------------
        Question(
            id = "r1",
            text = "What is something small I did this week that you appreciated but never mentioned?",
            type = QuestionType.REFLECT,
            category = "Appreciation",
            depth = Depth(2),
        ),
        Question(
            id = "r2",
            text = "Describe a moment in the last month when you felt closest to me.",
            type = QuestionType.REFLECT,
            category = "Closeness",
            depth = Depth(3),
        ),
        Question(
            id = "r3",
            text = "What are you looking forward to that has nothing to do with me?",
            type = QuestionType.REFLECT,
            category = "Independence",
            depth = Depth(1),
        ),
        Question(
            id = "r4",
            text = "What did you need this week that you did not ask for?",
            type = QuestionType.REFLECT,
            category = "Needs",
            depth = Depth(3),
        ),
        Question(
            id = "r5",
            text = "When you picture us five years from now, what is the first image that shows up?",
            type = QuestionType.REFLECT,
            category = "Future",
            depth = Depth(3),
        ),
        Question(
            id = "r6",
            text = "Which small ritual of ours would you miss most if it disappeared?",
            type = QuestionType.REFLECT,
            category = "Us",
            depth = Depth(2),
        ),

        // --- GUESS ME --------------------------------------------------------
        Question(
            id = "g1",
            text = "If we had sixty seconds to leave the house, what would you grab first?",
            type = QuestionType.GUESS_ME,
            category = "Know Me",
            depth = Depth(2),
        ),
        Question(
            id = "g2",
            text = "What is my most annoying habit that you secretly find endearing?",
            type = QuestionType.GUESS_ME,
            category = "Know Me",
            depth = Depth(2),
        ),
        Question(
            id = "g3",
            text = "Which meal could you happily eat every day for a month?",
            type = QuestionType.GUESS_ME,
            category = "Light",
            depth = Depth(1),
        ),
        Question(
            id = "g4",
            text = "Where would you go if we could leave tomorrow, money no object?",
            type = QuestionType.GUESS_ME,
            category = "Adventure",
            depth = Depth(1),
        ),
        Question(
            id = "g5",
            text = "What am I most insecure about?",
            type = QuestionType.GUESS_ME,
            category = "Deep",
            depth = Depth(3),
        ),
        Question(
            id = "g6",
            text = "What is the compliment you most want to hear, and never get?",
            type = QuestionType.GUESS_ME,
            category = "Deep",
            depth = Depth(3),
        ),

        // --- THIS OR THAT ----------------------------------------------------
        Question(
            id = "t1",
            text = "The ideal Friday night is:",
            type = QuestionType.THIS_OR_THAT,
            category = "Light",
            depth = Depth(1),
            options = listOf("Out with everyone", "Just us on the couch"),
        ),
        Question(
            id = "t2",
            text = "Surprise trip, or surprise party?",
            type = QuestionType.THIS_OR_THAT,
            category = "Light",
            depth = Depth(1),
            options = listOf("Trip", "Party"),
        ),
        Question(
            id = "t3",
            text = "When it is bad news, you would rather hear it:",
            type = QuestionType.THIS_OR_THAT,
            category = "Conflict",
            depth = Depth(2),
            options = listOf("Straight away, bluntly", "Gently, when there is time"),
        ),
        Question(
            id = "t4",
            text = "Found money: save it for the house, or spend it on the trip?",
            type = QuestionType.THIS_OR_THAT,
            category = "Money",
            depth = Depth(2),
            options = listOf("Save it", "Spend it"),
        ),

        // --- RANK ------------------------------------------------------------
        Question(
            id = "k1",
            text = "Put these in the order that actually matters to you right now.",
            type = QuestionType.RANK,
            category = "Priorities",
            depth = Depth(3),
            options = listOf(
                "Time together",
                "Money in the bank",
                "Career moving forward",
                "Family close by",
            ),
        ),
        Question(
            id = "k2",
            text = "Rank what you would like more of from me.",
            type = QuestionType.RANK,
            category = "Needs",
            depth = Depth(3),
            options = listOf("Attention", "Space", "Practical help", "Surprises"),
        ),

        // --- MULTI SELECT ----------------------------------------------------
        Question(
            id = "m1",
            text = "Which of these would make next year feel like a good one?",
            type = QuestionType.MULTI_SELECT,
            category = "Future",
            depth = Depth(2),
            options = listOf(
                "A trip somewhere new",
                "Moving house",
                "More time with friends",
                "Getting properly fit",
                "Less work",
                "A pet",
            ),
            maxChoices = 3,
        ),

        // --- PICK ONE --------------------------------------------------------
        Question(
            id = "p1",
            text = "What is our biggest strength as a pair?",
            type = QuestionType.PICK_ONE,
            category = "Us",
            depth = Depth(2),
            options = listOf(
                "We make each other laugh",
                "We sort problems out fast",
                "We give each other room",
                "We are honest, even when it stings",
            ),
        ),

        // --- SCALE -----------------------------------------------------------
        Question(
            id = "s1",
            text = "How supported did you feel this week?",
            type = QuestionType.SCALE,
            category = "Check in",
            depth = Depth(3),
            lowLabel = "Not much",
            highLabel = "Completely",
        ),

        // --- DROPDOWN --------------------------------------------------------
        Question(
            id = "d1",
            text = "If we could only travel once next year, which month?",
            type = QuestionType.DROPDOWN,
            category = "Adventure",
            depth = Depth(1),
            options = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December",
            ),
        ),
    )

    fun byId(id: String): Question = pool.first { it.id == id }
}

/** A believable couple with a fortnight of history behind them. */
object MockCouple {

    // Pack red and pack blue. They meet at magenta, which is the hero gradient.
    val you = Member(uid = "u_justin", name = "Justin", colorArgb = 0xFFF54E4F)
    val them = Member(uid = "u_maya", name = "Maya", colorArgb = 0xFF108BE2)

    /** A sculpture is a week. Short enough that you finish one and start again. */
    const val PeriodDays = 7
    private const val GridW = 12
    private const val GridH = 14


    /** Today starts unanswered, so the app opens on the state worth designing. */
    fun freshToday(today: LocalDate = LocalDate.now()): DayEntry =
        DayEntry(date = today, question = MockQuestions.byId("g1"))

    fun history(today: LocalDate = LocalDate.now()): List<DayEntry> = listOf(
        day(
            today.minusDays(1), "t3",
            yourResponse = "Straight away, bluntly",
            theirResponse = "Gently, when there is time",
            youGuess = "Gently, when there is time",
            themGuess = "Straight away, bluntly",
        ),
        day(
            today.minusDays(2), "r2",
            yourResponse = "The Tuesday the power went out and we ate crackers on the floor.",
            theirResponse = "Driving back from your mums, you asleep with a hand on the gearstick.",
        ),
        day(
            today.minusDays(3), "g3",
            yourResponse = "Ramen",
            theirResponse = "Your roast potatoes",
            youGuess = "Tacos",
            themGuess = "Ramen",
        ),
        day(
            today.minusDays(4), "t1",
            yourResponse = "Just us on the couch",
            theirResponse = "Just us on the couch",
            youGuess = "Out with everyone",
            themGuess = "Just us on the couch",
        ),
        day(
            today.minusDays(5), "r6",
            yourResponse = "Coffee before either of us says anything.",
            theirResponse = "You narrating what the cat is thinking.",
        ),
        day(
            today.minusDays(6), "g4",
            yourResponse = "Lisbon",
            theirResponse = "Japan, in autumn",
            youGuess = "Japan, in autumn",
            themGuess = "Lisbon",
        ),
        day(
            today.minusDays(7), "t4",
            yourResponse = "Spend it",
            theirResponse = "Save it",
            youGuess = "Save it",
            themGuess = "Spend it",
        ),
        // Day 8 is missing on purpose: that is the miss the weekly freeze absorbed.
        day(
            today.minusDays(9), "r3",
            yourResponse = "Finishing the bike. Genuinely just the bike.",
            theirResponse = "That pottery class I keep not booking.",
        ),
        day(
            today.minusDays(10), "g2",
            yourResponse = "You hum while you read",
            theirResponse = "You reorganise my desk",
            youGuess = "You reorganise my desk",
            themGuess = "You talk to the cat",
        ),

        // --- Older, and deliberately on today's date --------------------------
        // These exist so "on this day" has something to find. Six months and a
        // year back, because the whole point of that block is to show the app
        // holding on to something long after you would have forgotten it.
        day(
            today.minusMonths(6), "r4",
            yourResponse = "For you to tell me it was fine. I did not want it fixed.",
            theirResponse = "An hour where nobody needed anything from me.",
            reactions = mapOf(you.uid to Reaction.MELT),
        ),
        day(
            today.minusYears(1), "r5",
            yourResponse = "The kitchen with the bad light, and us not minding.",
            theirResponse = "A dog. Somehow there is always a dog.",
            reactions = mapOf(you.uid to Reaction.LAUGH, them.uid to Reaction.LOVE),
        ),
    )

    /** A both-answered day. Omitted guesses simply count as a miss. */
    private fun day(
        date: LocalDate,
        questionId: String,
        yourResponse: String,
        theirResponse: String,
        youGuess: String? = null,
        themGuess: String? = null,
        reactions: Map<String, Reaction> = emptyMap(),
    ): DayEntry = DayEntry(
        date = date,
        question = MockQuestions.byId(questionId),
        answers = mapOf(
            you.uid to Answer.of(you.uid, yourResponse, youGuess),
            them.uid to Answer.of(them.uid, theirResponse, themGuess),
        ),
        reactions = reactions,
    )

    /**
     * A fortnight's plate, part built.
     *
     * Hand-placed rather than generated so the sample reads as something two
     * people actually made -- a random scatter would sell the feature short.
     * Every placement satisfies the support rule, so this doubles as a fixture
     * for [BuildPlate.canPlace].
     */
    fun plate(today: LocalDate = LocalDate.now()): BuildPlate {
        val start = today.minusDays(4)
        var n = 0
        val blocks = mutableListOf<Block>()
        val placed = mutableListOf<Placement>()

        fun brick(shape: BrickShape, owner: Member, face: BlockFace? = null): Block =
            Block(
                id = "b${n++}",
                shape = shape,
                colorArgb = owner.colorArgb,
                ownerUid = owner.uid,
                earnedOn = start,
                face = face,
            ).also { blocks += it }

        fun put(shape: BrickShape, owner: Member, x: Int, y: Int, face: BlockFace? = null) {
            placed += Placement(brick(shape, owner, face).id, x, y)
        }

        val low = BrickHeight.LOW
        val med = BrickHeight.MEDIUM

        // Foundation, two six-stud bricks.
        put(BrickShape(6, low), you, 0, 0)
        put(BrickShape(6, low), them, 6, 0)
        // Staggered course, the way you actually lay bricks.
        put(BrickShape(4, low), them, 0, 1)
        put(BrickShape(4, low), you, 4, 1)
        put(BrickShape(4, low), them, 8, 1)
        // Taller mid section.
        put(BrickShape(4, med), you, 1, 2)
        put(BrickShape(4, med), them, 5, 2)
        put(BrickShape(2, med), you, 9, 2)
        // Shoulders.
        put(BrickShape(4, low), them, 2, 4)
        put(BrickShape(4, low), you, 6, 4)
        // The roof line, in slopes.
        put(BrickShape(4, med, Slope.LEFT), you, 2, 5)
        put(BrickShape(4, med, Slope.RIGHT), them, 6, 5)
        // And somebody on top.
        put(CharacterShape, you, 4, 7, BlockFace.A)

        // Earned but not yet placed -- what the tray shows.
        brick(BrickShape(4, low), you)
        brick(BrickShape(2, med, Slope.RIGHT), you)
        brick(BrickShape(6, low), you)
        brick(CharacterShape, you, BlockFace.B)
        brick(BrickShape(1, low), you)

        return BuildPlate(
            id = "plate_1",
            startedOn = start,
            periodDays = PeriodDays,
            gridW = GridW,
            gridH = GridH,
            blocks = blocks,
            placed = placed,
        )
    }

    /**
     * Sealed sculptures for the shelf.
     *
     * Generated rather than hand-placed, and generated *through*
     * [BuildPlate.dropRow] rather than by writing coordinates -- so every one of
     * these is a plate that the real placement rules would have accepted, and
     * the shelf doubles as a fixture for them. A fixed seed per week keeps each
     * sculpture the same between runs, which matters: a shelf that reshuffles
     * itself is not a memory of anything.
     */
    fun shelf(today: LocalDate = LocalDate.now()): List<BuildPlate> =
        listOf(
            // Three different weeks on purpose, because the shelf is where the
            // shape of a week is supposed to be legible at a glance:
            // an even one, a lopsided one, and a rough one built out of stubs.
            sculpture("plate_w1", today.minusDays(11), seed = 4021, yourShare = 0.5f, hitRate = 0.7f),
            sculpture("plate_w2", today.minusDays(18), seed = 1177, yourShare = 0.78f, hitRate = 0.6f),
            sculpture("plate_w3", today.minusDays(25), seed = 8830, yourShare = 0.45f, hitRate = 0.15f),
        )

    /**
     * [yourShare] is how much of the week was yours, and [hitRate] how often the
     * guesses landed -- the two things the real [Rewards] now encode. A sample
     * shelf where every week is an even fifty-fifty of identical bricks would
     * make the feature look like it does nothing.
     */
    private fun sculpture(
        id: String,
        startedOn: LocalDate,
        seed: Int,
        yourShare: Float,
        hitRate: Float,
    ): BuildPlate {
        val random = Random(seed)
        val blocks = mutableListOf<Block>()
        val placed = mutableListOf<Placement>()

        fun current() = BuildPlate(id, startedOn, PeriodDays, GridW, GridH, blocks, placed)

        repeat(PeriodDays * 2) { i ->
            val owner = if (random.nextFloat() < yourShare) you else them
            // Wide when the guess landed, a stub when it did not.
            val shape = if (random.nextFloat() < hitRate) {
                if (random.nextFloat() < 0.55f) {
                    BrickShape(6, BrickHeight.LOW)
                } else {
                    BrickShape(4, BrickHeight.MEDIUM)
                }
            } else {
                BrickShape(2, BrickHeight.LOW)
            }
            val plate = current()
            val occupied = plate.occupied()
            val (x, y) = bestSpot(plate, shape, occupied, random) ?: return@repeat

            val block = Block(
                id = "${id}_$i",
                shape = shape,
                colorArgb = owner.colorArgb,
                ownerUid = owner.uid,
                earnedOn = startedOn.plusDays((i / 2).toLong()),
            )
            blocks += block
            placed += Placement(block.id, x, y)
        }

        return current().copy(completedOn = startedOn.plusDays(PeriodDays - 1L))
    }

    /**
     * Where a person would actually put this brick.
     *
     * [BuildPlate.canPlace] only asks for **one** stud of support, which is the
     * right rule for a human placing a brick on purpose -- overhangs are half of
     * what makes a sculpture look built. Dropped at random, though, that rule
     * produces an inverted pyramid: a 6-stud brick balanced on a single stud,
     * over and over, until the whole week looks like it is about to fall over.
     *
     * So this scores every legal column by how much of the brick is actually
     * supported and how low it sits, and takes the best. Ties are broken by the
     * seeded [random] so the three shelf pieces do not come out identical.
     */
    private fun bestSpot(
        plate: BuildPlate,
        shape: BrickShape,
        occupied: Set<Pair<Int, Int>>,
        random: Random,
    ): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestScore = Int.MIN_VALUE

        for (x in 0..(GridW - shape.w)) {
            val y = plate.dropRow(shape, x, occupied) ?: continue
            val under = if (y == 0) {
                shape.w
            } else {
                (0 until shape.w).count { dx -> (x + dx to y - 1) in occupied }
            }
            // Support dominates; height breaks near-ties towards the baseplate;
            // the jitter keeps the silhouette from being perfectly symmetrical.
            val score = under * 10 - y * 3 + random.nextInt(4)
            if (score > bestScore) {
                bestScore = score
                best = x to y
            }
        }
        return best
    }

    /**
     * Deck runs in three different states, so the grouped list has something to
     * group. All-unplayed is the one arrangement that makes the sections look
     * like they do nothing.
     *
     * Question ids match the real deck files, because the result is built by
     * pairing runs against the loaded quiz -- ids that do not exist would score
     * a confident zero.
     */
    fun runs(): List<QuizRun> = listOf(
        // Maya has played this one and is waiting on you.
        QuizRun(
            quizId = "food",
            uid = them.uid,
            answers = mapOf("fd2" to listOf("Takeaway on the sofa"), "fd3" to listOf("Ramen")),
            guesses = mapOf("fd2" to listOf("Cook it together"), "fd3" to listOf("Ramen")),
        ),
        // You have played this one; nothing opens until she does.
        QuizRun(
            quizId = "travel",
            uid = you.uid,
            answers = mapOf("tr2" to listOf("Decide at breakfast")),
            guesses = mapOf("tr2" to listOf("Plan every day")),
        ),
        // Both in: there is a result sitting in this one.
        QuizRun(
            quizId = "playful",
            uid = you.uid,
            answers = mapOf("pl3" to listOf("Fly"), "pl2" to listOf("Coffee")),
            guesses = mapOf("pl3" to listOf("Be invisible"), "pl2" to listOf("Coffee")),
        ),
        QuizRun(
            quizId = "playful",
            uid = them.uid,
            answers = mapOf("pl3" to listOf("Be invisible"), "pl2" to listOf("My phone")),
            guesses = mapOf("pl3" to listOf("Fly"), "pl2" to listOf("Sitting down")),
        ),
    )

    fun state(today: LocalDate = LocalDate.now()) = CoupleState(
        you = you,
        them = them,
        today = freshToday(today),
        plate = plate(today),
        shelf = shelf(today),
        history = history(today),
        quizRuns = runs(),
        streak = 7,
        freezesLeft = 0,
    )
}
