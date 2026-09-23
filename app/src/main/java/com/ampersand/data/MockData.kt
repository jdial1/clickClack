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
import com.ampersand.model.InstrumentRun
import com.ampersand.model.DayEntry
import com.ampersand.model.Knowing
import com.ampersand.model.KnowingMonth
import com.ampersand.model.Depth
import com.ampersand.model.Member
import com.ampersand.model.Placement
import com.ampersand.model.Question
import com.ampersand.model.QuizRun
import com.ampersand.model.QuestionType
import com.ampersand.model.Reaction
import com.ampersand.model.Wildcard
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

/** Sample question pool, shaped like the `questions/{id}` documents. */
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

/** Sample couple with two weeks of history, for previews and the fake repository. */
object MockCouple {

    // Red and blue; the hero gradient runs between them.
    val you = Member(uid = "u_justin", name = "Justin", colorArgb = 0xFFF54E4F)
    val them = Member(uid = "u_maya", name = "Maya", colorArgb = 0xFF108BE2)

    /** Plate length in days. */
    const val PeriodDays = 7
    private const val GridW = BuildPlate.GRID_W
    private const val GridH = BuildPlate.GRID_H


    /** Today, unanswered. */
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
            theirResponse = "Japan, in the fall",
            youGuess = "Japan, in the fall",
            themGuess = "Lisbon",
        ),
        day(
            today.minusDays(7), "t4",
            yourResponse = "Spend it",
            theirResponse = "Save it",
            youGuess = "Save it",
            themGuess = "Spend it",
        ),
        // Day 8 missing on purpose: the miss covered by the weekly freeze.
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

        // Six months and a year back on today's date, for "on this day".
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

    /** A day both answered. Omitted guesses count as misses. */
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

    /** A part-built, hand-placed plate. Every placement satisfies [BuildPlate.canPlace]. */
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
        // Staggered course.
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
        // Character on top.
        put(CharacterShape, you, 4, 7, BlockFace.A)

        // Earned but unplaced (the tray).
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
     * Sealed plates for the shelf, generated via [BuildPlate.dropRow] so they obey placement rules.
     * Fixed seeds keep them stable between runs.
     */
    fun shelf(today: LocalDate = LocalDate.now()): List<BuildPlate> =
        listOf(
            // An even week, a lopsided week, and a week of quick picks.
            sculpture("plate_w1", today.minusDays(11), seed = 4021, yourShare = 0.5f, writtenRate = 0.7f),
            sculpture("plate_w2", today.minusDays(18), seed = 1177, yourShare = 0.78f, writtenRate = 0.6f),
            sculpture("plate_w3", today.minusDays(25), seed = 8830, yourShare = 0.45f, writtenRate = 0.15f),
        )

    /** [yourShare] is the fraction of bricks that are yours; [writtenRate] how many days were not quick picks (see [Rewards.widthFor]). */
    private fun sculpture(
        id: String,
        startedOn: LocalDate,
        seed: Int,
        yourShare: Float,
        writtenRate: Float,
    ): BuildPlate {
        val random = Random(seed)
        val blocks = mutableListOf<Block>()
        val placed = mutableListOf<Placement>()

        fun current() = BuildPlate(id, startedOn, PeriodDays, GridW, GridH, blocks, placed)

        repeat(PeriodDays * 2) { i ->
            val owner = if (random.nextFloat() < yourShare) you else them
            // A list or a written answer, or a 2-stud pick.
            val shape = if (random.nextFloat() < writtenRate) {
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
     * Picks the column with the most support and lowest row, plus seeded jitter.
     * [BuildPlate.canPlace] needs only one stud of support, so random drops would stack top-heavy.
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
            // Support first, then lower rows, plus jitter to avoid symmetry.
            val score = under * 10 - y * 3 + random.nextInt(4)
            if (score > bestScore) {
                bestScore = score
                best = x to y
            }
        }
        return best
    }

    /**
     * Deck runs in three states: partner done, you done, both done.
     * Question ids must match the real deck files or results score zero.
     */
    fun runs(): List<QuizRun> = listOf(
        // Maya played; waiting on you.
        QuizRun(
            quizId = "food-dinner",
            uid = them.uid,
            answers = mapOf("fd2" to listOf("Takeout on the couch"), "fd3" to listOf("Ramen")),
            guesses = mapOf("fd2" to listOf("Cook it together"), "fd3" to listOf("Ramen")),
        ),
        // You played; waiting on Maya.
        QuizRun(
            quizId = "travel-where",
            uid = you.uid,
            answers = mapOf("tr2" to listOf("Decide at breakfast")),
            guesses = mapOf("tr2" to listOf("Plan every day")),
        ),
        // Both played: result available.
        QuizRun(
            quizId = "playful-fast",
            uid = you.uid,
            // pl5 is a RANK, to cover ordered-answer results.
            answers = mapOf(
                "pl3" to listOf("Fly"),
                "pl2" to listOf("Coffee"),
                "pl5" to listOf(
                    "Dancing", "Small talk with strangers",
                    "Parallel parking", "Keeping a secret",
                ),
            ),
            guesses = mapOf(
                "pl3" to listOf("Be invisible"),
                "pl2" to listOf("Coffee"),
                "pl5" to listOf(
                    "Parallel parking", "Dancing",
                    "Keeping a secret", "Small talk with strangers",
                ),
            ),
        ),
        QuizRun(
            quizId = "playful-fast",
            uid = them.uid,
            answers = mapOf(
                "pl3" to listOf("Be invisible"),
                "pl2" to listOf("My phone"),
                "pl5" to listOf(
                    "Parallel parking", "Keeping a secret",
                    "Dancing", "Small talk with strangers",
                ),
            ),
            guesses = mapOf(
                "pl3" to listOf("Fly"),
                "pl2" to listOf("Sitting down"),
                "pl5" to listOf(
                    "Dancing", "Keeping a secret",
                    "Parallel parking", "Small talk with strangers",
                ),
            ),
        ),
    )

    /** Six months ago, same question id as [freshToday] (the capsule needs that match). */
    fun capsule(today: LocalDate = LocalDate.now()): DayEntry = day(
        today.minusMonths(6),
        "g1",
        yourResponse = "The laptop, probably. I would regret it.",
        theirResponse = "Whichever of us was nearest the door.",
        youGuess = "The photos",
        themGuess = "The laptop",
    )

    /** One queued wildcard. */
    fun wildcards(): List<Wildcard> = listOf(
        Wildcard(
            id = "w1",
            text = "What did you nearly say this week and did not?",
            type = QuestionType.REFLECT,
            byUid = them.uid,
            byName = them.name,
            createdAtMillis = System.currentTimeMillis() - 86_400_000L,
        ),
    )

    /** Four months of guess scores, deliberately uneven between partners. */
    fun knowing(today: LocalDate = LocalDate.now()): Knowing = Knowing(
        (0L..3L).map { back ->
            val month = YearMonth.from(today.minusMonths(back))
            when (back) {
                0L -> KnowingMonth(month, youHits = 7, theyHits = 9, scored = 12)
                1L -> KnowingMonth(month, youHits = 14, theyHits = 21, scored = 27)
                2L -> KnowingMonth(month, youHits = 11, theyHits = 16, scored = 24)
                else -> KnowingMonth(month, youHits = 6, theyHits = 8, scored = 15)
            }
        },
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
        capsule = capsule(today),
        wildcards = wildcards(),
        knowing = knowing(today),
        instrumentRuns = instrumentRuns(today),
    )

    /**
     * Love languages taken, Jungian scales not, so the badge wall shows both states.
     * You want acts and give words; they want time and give touch (a deliberate mismatch).
     */
    private fun instrumentRuns(today: LocalDate) = listOf(
        InstrumentRun(
            instrumentId = Instruments.LOVE_LANGUAGES.id,
            answers = mapOf(
                //                        words time gifts acts touch
                you.uid to language(felt = listOf(3, 3, 3, 5, 4),
                                    given = listOf(5, 3, 3, 3, 4)),
                them.uid to language(felt = listOf(3, 5, 3, 3, 4),
                                     given = listOf(3, 3, 3, 3, 5)),
            ),
        ),
    )

    /** Five per-language levels, expanded to the four items each one covers. */
    private fun language(felt: List<Int>, given: List<Int>): List<Int> =
        (felt + given).flatMap { level -> List(4) { level } }
}
