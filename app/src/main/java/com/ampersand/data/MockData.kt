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
import com.ampersand.model.QuestionType
import java.time.LocalDate

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
    )

    /** A both-answered day. Omitted guesses simply count as a miss. */
    private fun day(
        date: LocalDate,
        questionId: String,
        yourResponse: String,
        theirResponse: String,
        youGuess: String? = null,
        themGuess: String? = null,
    ): DayEntry = DayEntry(
        date = date,
        question = MockQuestions.byId(questionId),
        answers = mapOf(
            you.uid to Answer.of(you.uid, yourResponse, youGuess),
            them.uid to Answer.of(them.uid, theirResponse, themGuess),
        ),
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
        val start = today.minusDays(9)
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
            periodDays = 14,
            gridW = 12,
            gridH = 14,
            blocks = blocks,
            placed = placed,
        )
    }

    fun state(today: LocalDate = LocalDate.now()) = CoupleState(
        you = you,
        them = them,
        today = freshToday(today),
        plate = plate(today),
        history = history(today),
        streak = 7,
        freezesLeft = 0,
    )
}
