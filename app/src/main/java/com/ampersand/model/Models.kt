package com.ampersand.model

import java.time.LocalDate

/**
 * How a question is answered, which also decides whether it can be scored.
 *
 * REFLECT is deliberately unscored: the whole point of those days is the
 * conversation, and attaching a number to "how close did you feel to me" would
 * be the wrong incentive.
 */
enum class QuestionType {
    /** One open answer each. No right answer, nothing scored. */
    REFLECT,

    /** Open answer for yourself, plus a prediction of your partner. */
    GUESS_ME,

    /** Two options, radio-style. Fast, and what keeps a streak alive. */
    THIS_OR_THAT,

    /** Three or more options, pick one. */
    PICK_ONE,

    /** Pick several, optionally capped. */
    MULTI_SELECT,

    /** Drag into order. Scored on whether the top choice was predicted. */
    RANK,

    /** A 1-5 scale. */
    SCALE,

    /** One choice from a long list, in a dropdown. */
    DROPDOWN,
}

/** 1 = light, safe for a tired Tuesday. 3 = the ones worth a real evening. */
@JvmInline
value class Depth(val level: Int) {
    init { require(level in 1..3) { "depth must be 1..3, was $level" } }
}

data class Question(
    val id: String,
    val text: String,
    val type: QuestionType,
    val category: String,
    val depth: Depth,
    /** The choices, for every type except REFLECT and GUESS_ME. */
    val options: List<String> = emptyList(),
    /** MULTI_SELECT only: how many may be picked. */
    val maxChoices: Int? = null,
    /** SCALE only. */
    val lowLabel: String = "Not at all",
    val highLabel: String = "Completely",
) {
    val isScored: Boolean get() = type != QuestionType.REFLECT

    /**
     * Whether a prediction counted as a hit. Per type, because "right" means
     * something different for each:
     *
     * - RANK scores on the top choice alone. Exact-order matching on four items
     *   is almost never achieved, and a scoreboard nobody can move is a
     *   scoreboard nobody looks at. "Did you know what matters most to them" is
     *   the interesting question anyway.
     * - MULTI_SELECT compares sets, so order does not matter.
     */
    fun guessHit(guess: List<String>?, actual: List<String>): Boolean {
        if (guess.isNullOrEmpty() || actual.isEmpty()) return false
        return when (type) {
            QuestionType.REFLECT -> false
            QuestionType.MULTI_SELECT -> guess.normalised() == actual.normalised()
            else -> guess.first().trim().equals(actual.first().trim(), ignoreCase = true)
        }
    }

    private fun List<String>.normalised(): Set<String> =
        map { it.trim().lowercase() }.toSet()
}

/**
 * What you can say back once the seal lifts, without it becoming a chat.
 *
 * Maps **meaning** to art, the same way `Sfx` maps meaning to file: the label is
 * the sentence the picture stands in for, so swapping the artwork later does not
 * change what a stored reaction meant.
 *
 * [icon] is a name, not a resource id, because this file deliberately has no
 * Android imports -- the UI resolves it. These were emoji until it became clear
 * that an emoji is somebody else's art direction rendered by somebody else's
 * font, and looks different on every phone the couple owns.
 *
 * Six, deliberately. The reveal is meant to send you into the other room to
 * talk, so a reaction is a nudge and a receipt that they read it -- not a reply.
 */
enum class Reaction(val icon: String, val label: String) {
    LOVE("heart", "That is lovely"),
    LAUGH("laugh", "Of course you did"),
    MELT("hearts", "Oh"),
    SURPRISE("exclamation", "I did not know that"),
    CURIOUS("question", "Say more tonight"),
    KNEW_IT("star", "Knew it"),
}

data class Member(
    val uid: String,
    val name: String,
    /** 0xAARRGGBB. Drives this member's half of the blended theme. */
    val colorArgb: Long,
)

/**
 * One person's submission.
 *
 * [values] is a list for every type, even the single-answer ones, so RANK and
 * MULTI_SELECT do not need a parallel field or a delimiter hack.
 */
data class Answer(
    val uid: String,
    val values: List<String>,
    /** The prediction of the partner. Null on REFLECT days. */
    val guess: List<String>? = null,
) {
    val response: String get() = values.joinToString(", ")
    val guessText: String? get() = guess?.joinToString(", ")

    companion object {
        /** For the single-value types, which is most of them. */
        fun of(uid: String, response: String, guess: String? = null) =
            Answer(uid, listOf(response), guess?.let(::listOf))
    }
}

/**
 * One day for one couple.
 *
 * [answers] is keyed by uid and is the sealed part: in the real Firestore model
 * each entry is its own document, because security rules are per-document and
 * cannot hide a field inside a shared one.
 */
data class DayEntry(
    val date: LocalDate,
    val question: Question,
    val answers: Map<String, Answer> = emptyMap(),
    /**
     * One reaction each, keyed by uid, and only ever set after the reveal.
     *
     * Unlike [answers] this is not sealed -- there is nothing left to spoil --
     * and unlike an answer it *can* be changed, because it is a gesture rather
     * than a statement. That is also why it lives on the day document instead
     * of in the per-person subcollection: no rule has to hide it.
     */
    val reactions: Map<String, Reaction> = emptyMap(),
) {
    /** The seal only lifts once both have submitted. */
    val isRevealed: Boolean get() = answers.size >= 2

    fun answerFor(uid: String): Answer? = answers[uid]

    fun reactionFor(uid: String): Reaction? = reactions[uid]

    /**
     * Predictions that landed, 0..2. Null when the day is not scoreable or is
     * not revealed yet.
     */
    fun score(a: Member, b: Member): Int? {
        if (!question.isScored || !isRevealed) return null
        val ansA = answers[a.uid] ?: return null
        val ansB = answers[b.uid] ?: return null
        var hits = 0
        if (question.guessHit(ansA.guess, ansB.values)) hits++
        if (question.guessHit(ansB.guess, ansA.values)) hits++
        return hits
    }
}

/** Everything the UI needs to render. One object so previews stay cheap to set up. */
data class CoupleState(
    val you: Member,
    val them: Member,
    val today: DayEntry,
    val plate: BuildPlate,
    /** Sealed sculptures, newest first. Each one is a finished period. */
    val shelf: List<BuildPlate> = emptyList(),
    val history: List<DayEntry> = emptyList(),
    /** Both people's runs through any deck either of them has started. */
    val quizRuns: List<QuizRun> = emptyList(),
    /** The day the two of you started, once someone has told the app. */
    val anniversary: LocalDate? = null,
    val streak: Int = 0,
    val freezesLeft: Int = 1,
    /**
     * The day today's question came back from, when today is a capsule.
     *
     * Null on an ordinary day. When it is set, the two of you answered this
     * exact question once before and the reveal has four answers in it rather
     * than two.
     */
    val capsule: DayEntry? = null,
    /** Questions the two of you wrote, oldest first, none of them asked yet. */
    val wildcards: List<Wildcard> = emptyList(),
    /**
     * Sittings with the whole-instrument tests, oldest first.
     *
     * Kept apart from [history] on purpose. A day is one question and a run is
     * thirty-two, and the moment an instrument can be reached through the same
     * list as a daily question is the moment one of them gets shuffled into a
     * Tuesday and the citation on it becomes false.
     */
    val instrumentRuns: List<InstrumentRun> = emptyList(),
    /** How often each of you has called the other, for as long as you have played. */
    val knowing: Knowing = Knowing(),
) {
    /**
     * Answered days falling on today's date in an earlier month or year.
     *
     * Matching on day-of-month rather than exact anniversaries means there is
     * something to show more than once a year, and the screen can say how long
     * ago it was. Anything inside the last month is excluded: "three weeks ago
     * today" is not a memory, it is just recent.
     */
    fun onThisDay(on: LocalDate = today.date): List<DayEntry> =
        history.filter { it.date.dayOfMonth == on.dayOfMonth && it.date < on.minusDays(27) }
            .sortedByDescending { it.date }

    /** Your run through [quizId], if you have played it. */
    fun myRun(quizId: String): QuizRun? = quizRuns.firstOrNull {
        it.quizId == quizId && it.uid == you.uid
    }

    fun theirRun(quizId: String): QuizRun? = quizRuns.firstOrNull {
        it.quizId == quizId && it.uid == them.uid
    }

    /** Where [quizId] has got to between the two of you. */
    fun deckState(quizId: String): DeckState {
        val mine = myRun(quizId)
        val theirs = theirRun(quizId)
        return when {
            mine != null && theirs != null -> DeckState.FINISHED
            mine != null -> DeckState.WAITING_ON_THEM
            theirs != null -> DeckState.WAITING_ON_YOU
            else -> DeckState.NOT_STARTED
        }
    }

    /** How many decks are sitting waiting for you specifically. */
    fun decksAwaitingYou(quizIds: List<String>): Int =
        quizIds.count { deckState(it) == DeckState.WAITING_ON_YOU }

    /** Predictions landed vs. possible, across scoreable revealed days. */
    val syncPercent: Int
        get() {
            val scored = history.mapNotNull { it.score(you, them) }
            if (scored.isEmpty()) return 0
            return (scored.sum() * 100) / (scored.size * 2)
        }
}
