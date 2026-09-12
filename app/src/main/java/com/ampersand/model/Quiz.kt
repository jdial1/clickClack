package com.ampersand.model

/**
 * A themed set of questions, answered in one sitting.
 *
 * The unit of play is the **quiz**, not the question. Every couples app worth
 * looking at does this -- Paired, Couple Game, QuizUs all ship decks of ten
 * rather than a lone daily prompt -- because a single question is a nice moment
 * and a deck is an activity you sit down for. Click Clack keeps the daily
 * question as the habit and adds decks as the thing you do on a Sunday.
 *
 * The pool arrives as JSON, so everything here is data. Nothing about a quiz is
 * hard-coded except how each [QuestionType] is drawn.
 */
data class Quiz(
    val id: String,
    val title: String,
    /** One line under the title. What this deck is actually about. */
    val blurb: String,
    val theme: QuizTheme,
    val questions: List<Question>,
) {
    val size: Int get() = questions.size

    /** Rough, and deliberately rough: an estimate people can plan around. */
    val minutes: Int get() = (questions.sumOf { it.type.weight } / 60).coerceAtLeast(1)

    /** Whether this deck scores at all. A deck of REFLECTs does not. */
    val isScored: Boolean get() = questions.any { it.isScored }
}

/**
 * The visual identity of a deck.
 *
 * A colour from the pack rather than a free hex value, because a deck header is
 * built out of bricks like everything else and there is no brick in a colour
 * the pack does not ship.
 *
 * Colour and label, and no icon. There was an emoji here; emoji are somebody
 * else's art direction rendered by somebody else's font, and they are banned
 * everywhere except the reactions, where the glyph *is* the message. Kenney's
 * icon set is game-controller furniture -- gamepads, arrows, a trophy -- with
 * nothing for "food" or "spicy", and a nearly-right icon is worse than none.
 * The brick colour already identifies a deck at a glance.
 */
enum class QuizTheme(
    val label: String,
    val brick: BrickColor,
) {
    KNOW_ME("Know me", BrickColor.RED),
    EVERYDAY("Everyday", BrickColor.YELLOW),
    FUTURE("Future", BrickColor.BLUE),
    PLAYFUL("Playful", BrickColor.GREEN),
    DEEP("Deep", BrickColor.BLUE),
    FOOD("Food", BrickColor.YELLOW),
    TRAVEL("Travel", BrickColor.GREEN),

    /**
     * Warm, not explicit. The brief is playful: this is a couple on a sofa, and
     * a deck that turns into an interrogation gets closed and not reopened.
     */
    SPICY("Spicy", BrickColor.RED),
}

/**
 * What to call a question type on screen.
 *
 * The enum name is for the code. THIS_OR_THAT shouted at someone mid-deck is a
 * database field, not a label.
 */
val QuestionType.label: String
    get() = when (this) {
        QuestionType.REFLECT -> "Reflect"
        QuestionType.GUESS_ME -> "Guess me"
        QuestionType.THIS_OR_THAT -> "This or that"
        QuestionType.PICK_ONE -> "Pick one"
        QuestionType.MULTI_SELECT -> "Pick a few"
        QuestionType.RANK -> "Put in order"
        QuestionType.SCALE -> "How much"
        QuestionType.DROPDOWN -> "Choose"
    }

/**
 * How much of a question each type is, in seconds, for the estimate above.
 *
 * Typing costs far more than tapping, and ordering costs more than either
 * because you have to weigh every item against every other one.
 */
val QuestionType.weight: Int
    get() = when (this) {
        QuestionType.REFLECT -> 90
        QuestionType.GUESS_ME -> 75
        QuestionType.RANK -> 60
        QuestionType.MULTI_SELECT -> 40
        QuestionType.DROPDOWN -> 25
        QuestionType.PICK_ONE -> 20
        QuestionType.SCALE -> 15
        QuestionType.THIS_OR_THAT -> 12
    }

/**
 * Where a deck has got to, for these two people.
 *
 * Declaration order is the order it belongs in a list, and that order is the
 * whole feature. A pool of decks sorted by filename makes the one your partner
 * is waiting on you for look exactly like the six you have never opened, so the
 * only thing either of you can do is remember. Sorted by state, the list tells
 * you what to do next without being asked.
 */
enum class DeckState(val heading: String) {
    /** They played it and you have not. The only one with a deadline attached. */
    WAITING_ON_YOU("Waiting on you"),

    /** You played it; nothing opens until they do. */
    WAITING_ON_THEM("Waiting on them"),

    /** Both in. There is a result sitting in it. */
    FINISHED("Finished"),

    NOT_STARTED("Not started yet"),
}

/**
 * One person's run through a quiz.
 *
 * Keyed by question id rather than positional, because a deck can gain or lose
 * a question between someone starting it and their partner finishing it, and
 * an index would then line up the wrong pair of answers.
 */
data class QuizRun(
    val quizId: String,
    val uid: String,
    /** questionId -> what they said about themselves. */
    val answers: Map<String, List<String>>,
    /** questionId -> what they predicted their partner would say. */
    val guesses: Map<String, List<String>> = emptyMap(),
    /**
     * When it was submitted, epoch millis, or null when the server has not
     * stamped it yet.
     *
     * Only used for ordering. A list of decks in filename order is fine at nine
     * and useless at fifty: what you want at the top of "waiting on you" is the
     * one they finished this morning, not the one whose file sorts first.
     */
    val completedAtMillis: Long? = null,
) {
    fun isComplete(quiz: Quiz): Boolean =
        quiz.questions.all { answers[it.id]?.isNotEmpty() == true }
}

/**
 * One question, both people, both directions.
 *
 * This is the cross-answer: four values per question, not two. Yours, theirs,
 * your guess at theirs, and their guess at yours -- which is what makes the
 * scoring asymmetric and therefore interesting. "We got 4 out of 6" says
 * nothing; "you knew me four times and I knew you twice" is a conversation.
 */
data class CrossAnswer(
    val question: Question,
    val yours: List<String>,
    val theirs: List<String>,
    val yourGuess: List<String>?,
    val theirGuess: List<String>?,
) {
    /** You predicted them correctly. */
    val youKnewThem: Boolean get() = question.guessHit(yourGuess, theirs)

    /** They predicted you correctly. */
    val theyKnewYou: Boolean get() = question.guessHit(theirGuess, yours)

    /**
     * You two independently gave the same answer.
     *
     * Different from either prediction landing, and worth showing separately:
     * agreeing about pineapple is not the same as knowing what they think
     * about pineapple.
     */
    val agreed: Boolean get() = question.guessHit(yours, theirs)
}

/**
 * A finished quiz, from your side.
 *
 * Two scores, never one. A combined percentage hides the only interesting
 * thing in the data, which is when it is lopsided.
 */
data class QuizResult(
    val quiz: Quiz,
    val rows: List<CrossAnswer>,
) {
    val scored: List<CrossAnswer> get() = rows.filter { it.question.isScored }

    /** How many of your predictions about them landed. */
    val youKnewThem: Int get() = scored.count { it.youKnewThem }

    /** How many of theirs about you landed. */
    val theyKnewYou: Int get() = scored.count { it.theyKnewYou }

    /** How often you simply said the same thing. */
    val agreements: Int get() = scored.count { it.agreed }

    val possible: Int get() = scored.size

    /** Both scores together, over everything that could have been scored. */
    val syncPercent: Int
        get() = if (possible == 0) 0 else ((youKnewThem + theyKnewYou) * 100) / (possible * 2)

    /**
     * The one line that goes under the chart.
     *
     * Bands rather than a number, and phrased about the pair rather than about
     * a winner. The apps that do this badly declare one partner the victor,
     * which is a strange thing to do to two people on a sofa.
     */
    val verdict: String
        get() = when {
            possible == 0 -> "Nothing to score. That was the point."
            syncPercent >= 90 -> "You two are finishing each other's sentences."
            syncPercent >= 70 -> "You know each other well."
            syncPercent >= 45 -> "Solidly in step, with some surprises."
            syncPercent >= 20 -> "Plenty left to find out."
            else -> "You have some talking to do."
        }

    /**
     * Whether one of you knew the other far better, and who.
     *
     * Null when it is close. A two-point gap is noise and calling it out would
     * manufacture a grievance out of a rounding error.
     */
    fun lopsided(yourName: String, theirName: String): String? {
        val gap = youKnewThem - theyKnewYou
        if (possible < 4 || kotlin.math.abs(gap) < possible / 3 + 1) return null
        return if (gap > 0) {
            "$yourName called it more often this time."
        } else {
            "$theirName called it more often this time."
        }
    }

    companion object {
        /** Pair up two runs through the same deck. */
        fun of(quiz: Quiz, yours: QuizRun, theirs: QuizRun): QuizResult = QuizResult(
            quiz = quiz,
            rows = quiz.questions.map { q ->
                CrossAnswer(
                    question = q,
                    yours = yours.answers[q.id].orEmpty(),
                    theirs = theirs.answers[q.id].orEmpty(),
                    yourGuess = yours.guesses[q.id],
                    theirGuess = theirs.guesses[q.id],
                )
            },
        )
    }
}
