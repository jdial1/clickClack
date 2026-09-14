package com.ampersand.model

/** A themed deck of questions, answered in one sitting. Loaded from JSON. */
data class Quiz(
    val id: String,
    val title: String,
    /** One line under the title. */
    val blurb: String,
    val theme: QuizTheme,
    val questions: List<Question>,
) {
    val size: Int get() = questions.size

    /** Playful decks are open: each of you can see the other's answers as you play. Others stay sealed. */
    val sealed: Boolean get() = theme != QuizTheme.PLAYFUL

    /** Rough time estimate from [QuestionType.weight]. */
    val minutes: Int get() = (questions.sumOf { it.type.weight } / 60).coerceAtLeast(1)
}

/** A deck's label and brick color. No icons; the pack has none that fit. */
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
    ROMANTIC("Romantic", BrickColor.RED),

    /** Warm and playful, not explicit. */
    SPICY("Spicy", BrickColor.RED),
}

/** On-screen name for a question type. */
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

/** Estimated seconds to answer each type, for [Quiz.minutes]. */
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

/** Where a deck has got to for the couple. Declaration order is the list sort order. */
enum class DeckState(val heading: String) {
    /** They played it and you have not. */
    WAITING_ON_YOU("Waiting for you"),

    /** You played it; results open when they do. */
    WAITING_ON_THEM("Waiting for them"),

    /** Both played; results are ready. */
    FINISHED("Finished"),

    NOT_STARTED("Not started"),
}

/** One person's run through a quiz. Keyed by question id so deck edits cannot misalign answers. */
data class QuizRun(
    val quizId: String,
    val uid: String,
    /** questionId -> what they said about themselves. */
    val answers: Map<String, List<String>>,
    /** questionId -> what they predicted their partner would say. */
    val guesses: Map<String, List<String>> = emptyMap(),
    /** Submit time in epoch millis, or null until the server stamps it. Used for ordering. */
    val completedAtMillis: Long? = null,
) {
    fun isComplete(quiz: Quiz): Boolean =
        quiz.questions.all { answers[it.id]?.isNotEmpty() == true }
}

/** One question's answers and guesses from both people. */
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

    /** You both gave the same answer (separate from either guess landing). */
    val agreed: Boolean get() = question.guessHit(yours, theirs)
}

/** A finished quiz from your side, with a separate score for each direction. */
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

    /** One-line summary under the chart, by score band. */
    val verdict: String
        get() = when {
            possible == 0 -> "Nothing to score in this one."
            syncPercent >= 90 -> "You know each other really well."
            syncPercent >= 70 -> "You know each other well."
            syncPercent >= 45 -> "Mostly in sync, with a few surprises."
            syncPercent >= 20 -> "Lots left to learn."
            else -> "Time for a good talk."
        }

    /** A line when one of you guessed far better, or null when it is close. */
    fun lopsided(yourName: String, theirName: String): String? {
        val gap = youKnewThem - theyKnewYou
        if (possible < 4 || kotlin.math.abs(gap) < possible / 3 + 1) return null
        return if (gap > 0) {
            "$yourName guessed right more often."
        } else {
            "$theirName guessed right more often."
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
