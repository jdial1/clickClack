package com.ampersand.model

import java.time.LocalDate

/** How a question is answered, which also decides whether it can be scored. */
enum class QuestionType {
    /** One open answer each. Not scored. */
    REFLECT,

    /** Open answer for yourself, plus a prediction of your partner. */
    GUESS_ME,

    /** Two options, radio-style. */
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

/** 1 = light, 3 = deep. */
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
    /** Subject tags like "planning", "touch", "money". Finer than [category], which is the deck theme. */
    val tags: Set<String> = emptySet(),
) {
    val isScored: Boolean get() = type != QuestionType.REFLECT

    /**
     * Whether a prediction counted as a hit. MULTI_SELECT compares sets; every
     * other type (including RANK) compares the first value only.
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
 * Reactions to a revealed answer.
 *
 * [icon] is an art name the UI resolves (no Android imports here). Stored by
 * enum name, so the art and label can change freely.
 */
enum class Reaction(val icon: String, val label: String) {
    LOVE("heart", "Love it"),
    LAUGH("laugh", "Of course"),
    MELT("hearts", "Aww"),
    SURPRISE("exclamation", "I didn't know that"),
    CURIOUS("question", "Tell me more tonight"),
    KNEW_IT("star", "Knew it"),
}

data class Member(
    val uid: String,
    val name: String,
    /** 0xAARRGGBB. Drives this member's half of the blended theme. */
    val colorArgb: Long,
    /** One of [GenderChoices], if they said. Shown, never used to choose questions. */
    val gender: String? = null,
    val birthday: LocalDate? = null,
    /** How they said they are, and when. See [MoodEntry.current]. */
    val mood: MoodEntry? = null,
)

/** One person's submission. [values] is a list for every type, so RANK and MULTI_SELECT fit. */
data class Answer(
    val uid: String,
    val values: List<String>,
    /** The prediction of the partner. Null on REFLECT days. */
    val guess: List<String>? = null,
) {
    val response: String get() = values.joinToString(", ")
    val guessText: String? get() = guess?.joinToString(", ")

    companion object {
        /** For the single-value types. */
        fun of(uid: String, response: String, guess: String? = null) =
            Answer(uid, listOf(response), guess?.let(::listOf))
    }
}

/**
 * One day for one couple.
 *
 * [answers] is keyed by uid. In Firestore each answer is its own document,
 * because rules are per-document and cannot hide a field.
 */
data class DayEntry(
    val date: LocalDate,
    val question: Question,
    val answers: Map<String, Answer> = emptyMap(),
    /** One reaction each, keyed by uid, set after the reveal. Changeable; stored on the day doc. */
    val reactions: Map<String, Reaction> = emptyMap(),
    /** Uids who marked that this day came up again in conversation. Not sealed, not scored. */
    val talkedAbout: Set<String> = emptySet(),
    /**
     * The day doc's `revealed` flag: both answers are in, even when not loaded.
     *
     * Past days are read without their answer subcollection, so counting [answers]
     * gives zero. Rules only allow `revealed` once both answer docs exist.
     */
    val opened: Boolean = false,
    /** Who has answered, from the day document. Read on past days, which carry no answers. */
    val submitted: Set<String> = emptySet(),
    /** Tomorrow's theme, picked tonight. Hidden from the partner by the UI, not rules. See [Turns]. */
    val nextTheme: String? = null,
    val nextThemeBy: String? = null,
    /**
     * The run this day made, written by whichever phone opened it. Zero when
     * nothing was written (older days). See [Streaks].
     */
    val recordedRun: Int = 0,
    /** A line each after the reveal, keyed by uid. */
    val notes: Map<String, String> = emptyMap(),
    /** A reply to the other person's mood that day, keyed by the uid who sent it. */
    val moodReplies: Map<String, String> = emptyMap(),
) {
    /** Either of you marked it. */
    val wasTalkedAbout: Boolean get() = talkedAbout.isNotEmpty()

    /** The seal only lifts once both have submitted. */
    val isRevealed: Boolean get() = opened || answers.size >= 2

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
    /** Sealed sculptures, newest first. */
    val shelf: List<BuildPlate> = emptyList(),
    val history: List<DayEntry> = emptyList(),
    /** Both people's runs through any deck either of them has started. */
    val quizRuns: List<QuizRun> = emptyList(),
    /** The day the two of you started, if set. */
    val anniversary: LocalDate? = null,
    val streak: Int = 0,
    /** The earlier day today's question repeats, when today is a capsule. Null otherwise. */
    val capsule: DayEntry? = null,
    /** Questions the two of you wrote, oldest first, none of them asked yet. */
    val wildcards: List<Wildcard> = emptyList(),
    /** The shared bucket list, done and not. */
    val bucket: List<BucketItem> = emptyList(),
    val relationship: RelationshipStatus? = null,
    /** Asked for only when [relationship] is married. */
    val marriedOn: LocalDate? = null,
    /** Topics you have taken out of the daily question. */
    val yourHiddenTopics: Set<Topic> = emptySet(),
    /** Hidden by either of you; this is what the draw uses. */
    val hiddenTopics: Set<Topic> = emptySet(),
    val specialDates: List<SpecialDate> = emptyList(),
    /** Topics you asked for more of. */
    val yourInterests: Set<Topic> = emptySet(),
    /** Asked for by either of you; the draw leans toward these. */
    val interests: Set<Topic> = emptySet(),
    /** The couple's home ZIP, and where that is. */
    val homeZip: String? = null,
    val region: UsRegion? = null,
    /** The nearest baseball and football teams, e.g. "Pirates and Steelers". */
    val localTeams: String? = null,
    /** Leagues either of you follows. Sports questions stay out until there is one. */
    val following: Set<League> = emptySet(),
    val yourFollowing: Set<League> = emptySet(),
    /** Whether you have seen the interests questions. Null until your profile loads. */
    val interestsAsked: Boolean? = null,
    /** The home ZIP's state, for holidays that belong to a place. */
    val homeState: String? = null,
    /** Sunrise and sunset at home today. */
    /** Question sources either of you can turn off in Advanced settings. Shared, so both phones agree. */
    val sourcesOff: Set<QuestionSource> = emptySet(),
    /** Why today's question was chosen, for the tag on its card: "Weather", "First date", "Family". */
    val why: String? = null,
    /** Days and decks either of you kept, keyed by [Kept.day] or [Kept.deck], with a label to show. */
    val kept: Map<String, String> = emptyMap(),
    /** Opened days older than the loaded history, fetched on demand to show a kept or recalled day. */
    val recalled: List<DayEntry> = emptyList(),
    /** Today's forecast, as saved on the day. Null until somebody fetched it. */
    val weather: DayWeather? = null,
    /** Instrument sittings, oldest first. Kept apart from [history] so tests never mix with daily questions. */
    val instrumentRuns: List<InstrumentRun> = emptyList(),
    /** How often each of you has called the other, all time. */
    val knowing: Knowing = Knowing(),
) {
    /** Answered days on today's day-of-month in earlier months, excluding the last 27 days. */
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

    /** How many decks are waiting on you. */
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

/** Optional places the daily question can come from. Stored on the couple document by [field]. */
enum class QuestionSource(val field: String, val label: String, val detail: String) {
    WEATHER("weatherQuestions", "Weather questions", "On notable weather days."),
    LOCAL("localQuestions", "Local questions", "Your region and local teams."),
    HOLIDAYS("stateHolidays", "State holidays", "Days that belong to your state."),
}

/** Keys for kept things, stored on the couple document. */
object Kept {
    fun day(date: java.time.LocalDate) = "day_$date"
    fun deck(id: String) = "deck_$id"
    fun dateOf(key: String): java.time.LocalDate? =
        key.removePrefix("day_").takeIf { key.startsWith("day_") }?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
    fun deckOf(key: String): String? = key.removePrefix("deck_").takeIf { key.startsWith("deck_") }
}
