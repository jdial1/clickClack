package com.ampersand.model

import java.time.LocalDate

/**
 * How faithfully the app reproduces a source, which decides the credit line.
 *
 * The distinction is not pedantry. Reproducing a working instrument verbatim is
 * a different act from quoting a conversation prompt, and the two need different
 * words under them -- "from Jorgenson's Open Extended Jungian Type Scales" is a
 * true sentence about [VERBATIM] and a false one about [INSPIRED].
 */
enum class Fidelity {
    /** Their items, their order, their wording. */
    VERBATIM,

    /** Their items, shortened or reworded. The psychometrics no longer carry. */
    ADAPTED,

    /** Our words, their idea. Cite the paper for the construct, not the text. */
    INSPIRED,
}

/**
 * Where a question or an instrument came from.
 *
 * Attribution lives on the content rather than on a credits screen, because a
 * credits screen is the version of attribution nobody reads. The line shows up
 * after the seal lifts, which is also a nice beat: you have both answered, and
 * then the app tells you this one is from a study where strangers fell in love.
 */
data class Source(
    val id: String,
    val authors: String,
    val year: Int,
    val title: String,
    val venue: String,
    val url: String,
    /** Spelled out, because "free" is not a licence and neither is a web page. */
    val licence: String,
    val fidelity: Fidelity,
) {
    /** "Aron et al. (1997)" -- what fits under a question. */
    val credit: String get() = "$authors ($year)"
}

/**
 * One rated item.
 *
 * Two shapes, because the two instruments that need this ask differently. A
 * *pole pair* has no statement at all -- the two ends are the whole item, and
 * you place yourself between them ("makes lists" against "relies on memory").
 * A *statement* has [text] and the poles become agreement anchors.
 *
 * Both are still one 1..5 answer, so the scoring and the storage never have to
 * know which kind they are looking at.
 */
data class InstrumentItem(
    val low: String,
    val high: String,
    /** Null on a pole pair. */
    val text: String? = null,
) {
    val isPolePair: Boolean get() = text == null
}

/**
 * A group of items that combine into one number.
 *
 * [terms] carries the sign of each item in the equation rather than a separate
 * "reversed" set, because the published scoring for the Jungian scales is
 * genuinely a signed sum with a constant -- `IE = 30 - Q3 - Q7 - Q11 + Q15 ...`
 * -- and expressing that as "sum these, then reverse those" loses the constant
 * and invites an off-by-one nobody would ever notice.
 *
 * [cut] turns the number into a word. Above it you get [high], at or below it
 * you get [low]. Null when the subscale is just a score, as it is for every
 * love language: there is no threshold at which you become Acts of Service.
 */
data class Subscale(
    val id: String,
    val label: String,
    /** Item index (0-based, within one pass) to its sign, +1 or -1. */
    val terms: Map<Int, Int>,
    val constant: Int = 0,
    val cut: Int? = null,
    val low: String = "",
    val high: String = "",
) {
    /** The floor and ceiling the equation can actually reach. */
    val range: IntRange
        get() {
            val lo = constant + terms.values.sumOf { if (it > 0) it * 1 else it * 5 }
            val hi = constant + terms.values.sumOf { if (it > 0) it * 5 else it * 1 }
            return lo..hi
        }

    fun scoreOf(responses: List<Int>): Int =
        constant + terms.entries.sumOf { (i, sign) -> sign * (responses.getOrNull(i) ?: 3) }
}

/** How often an instrument may be run. Enforced, not documented. */
enum class Cadence(val label: String, val days: Int) {
    ONCE("once", Int.MAX_VALUE),
    YEARLY("once a year", 365),
    QUARTERLY("every few months", 90),
}

/**
 * A whole instrument: every item, in the published order, in one sitting.
 *
 * This is deliberately *not* a `Question`, and giving it the same type is the
 * mistake this class exists to prevent. A pool question can be shuffled into a
 * Tuesday; an instrument cannot, because its arithmetic only holds if the whole
 * thing was answered together. Seven items in March and seven in July is not a
 * profile, it is fourteen unrelated rating days wearing a citation.
 *
 * [passes] is how one item list gets asked twice. The love languages research
 * measures the same twenty behaviours as *what makes me feel loved* and as
 * *what I tend to do*, and the gap between one person's first pass and the
 * other's second is the entire finding. Each pass contributes its own block of
 * responses, so a run of a two-pass instrument stores forty numbers.
 */
data class Instrument(
    val id: String,
    val sourceId: String,
    val title: String,
    /** One line, shown on the locked badge and at the top of the sitting. */
    val blurb: String,
    val items: List<InstrumentItem>,
    val subscales: List<Subscale>,
    val cadence: Cadence,
    val minutes: Int,
    /**
     * Subscales scored but never shown.
     *
     * The Love Attitudes Scale is six subscales and two of them -- concealing
     * other lovers, and jealousy -- are a bad thing to hand a married couple on
     * a Tuesday. Dropping the items would make it a different instrument, so
     * they are answered and simply not rendered. The profile stays valid and
     * nobody reads the ugly part.
     */
    val hidden: Set<String> = emptySet(),
    /** The stem each pass puts in front of every item. One entry = one pass. */
    val passes: List<String> = listOf(""),
) {
    val passCount: Int get() = passes.size

    /** Total responses one person gives: every item, once per pass. */
    val length: Int get() = items.size * passCount

    val shown: List<Subscale> get() = subscales.filterNot { it.id in hidden }

    /**
     * How far [answered] responses gets someone through this instrument.
     *
     * Only love languages has rounds, so for everything else this collapses to
     * a count and the UI never says the word. That asymmetry is the reason it
     * is computed here rather than in the wall: two of the three tests have no
     * part one, and a screen that reasons about rounds would have to keep
     * remembering which.
     */
    fun standingOf(answered: Int): Standing {
        val safe = answered.coerceIn(0, length)
        return Standing(
            answered = safe,
            total = length,
            // The round you are *in*, so a finished round one reads as round
            // two rather than as round one over and over until you start it.
            pass = if (items.isEmpty()) 0 else (safe / items.size).coerceAtMost(passCount - 1),
            passes = passCount,
        )
    }

    /**
     * Whether this is long enough to be worth showing progress against.
     *
     * A form you can see all of at once does not need a header telling you how
     * far down it you are; that is chrome on a thing with no distance in it.
     */
    val needsProgress: Boolean get() = length > ProgressFloor

    /**
     * The sitting, cut into pages.
     *
     * Thirty-two rated items on one scroll is a wall of identical rows with no
     * landmark in it -- you cannot tell whether you are a third of the way down
     * or two thirds, and there is nowhere to stop. Ten is long enough to be
     * worth a page and short enough to see the end of from the top.
     *
     * A page never spans two passes. The stem changes between them ("what makes
     * me feel loved" becomes "what I tend to do") and a page carrying half of
     * each would be asking two different questions under one heading.
     */
    fun pages(perPage: Int = PageSize): List<InstrumentPage> {
        require(perPage > 0) { "a page needs at least one item" }
        val out = mutableListOf<InstrumentPage>()
        passes.indices.forEach { pass ->
            var from = 0
            while (from < items.size) {
                val end = minOf(from + perPage, items.size)
                val base = pass * items.size
                out += InstrumentPage(
                    index = out.size,
                    pass = pass,
                    items = from..(end - 1),
                    slots = (base + from)..(base + end - 1),
                )
                from = end
            }
        }
        return out
    }

    /** [responses] is the whole run; this slices out one pass. */
    fun readings(responses: List<Int>, pass: Int = 0): List<Reading> {
        val from = pass * items.size
        val slice = responses.drop(from).take(items.size)
        return subscales.map { Reading(it, it.scoreOf(slice), pass) }
    }
}

/**
 * How far one person has got with one instrument.
 *
 * Deliberately a count and a round rather than a percentage. "Round 1 of 2" is
 * a thing the other person can act on -- it tells them what you are in the
 * middle of -- and 47% is not.
 *
 * This is the only thing about an unfinished sitting that crosses between the
 * two of you. The answers themselves stay private until both are sealed, which
 * is the whole mechanic; a count of how many you have given away nothing about
 * what they were.
 */
data class Standing(
    val answered: Int,
    val total: Int,
    /** 0-based round they are currently in. */
    val pass: Int,
    val passes: Int,
) {
    val started: Boolean get() = answered > 0
    val finished: Boolean get() = answered >= total
    val inProgress: Boolean get() = started && !finished

    /** True where the instrument has rounds at all, so wording can say so. */
    val hasRounds: Boolean get() = passes > 1

    companion object {
        fun none(total: Int, passes: Int) = Standing(0, total, 0, passes)
    }
}

/** Ten. Long enough to be worth a page, short enough to see the end of. */
const val PageSize = 10

/** At or below this, a sitting is one screen and progress is noise on it. */
const val ProgressFloor = 5

/**
 * One screenful of a sitting.
 *
 * [items] indexes into `Instrument.items` and so is relative to the pass;
 * [slots] indexes into the flat response list, which runs every pass end to
 * end. Both are carried because the UI needs the first to draw and the second
 * to read and write, and deriving one from the other at each call site is how
 * an off-by-one gets in.
 */
data class InstrumentPage(
    val index: Int,
    val pass: Int,
    val items: IntRange,
    val slots: IntRange,
) {
    val size: Int get() = items.count()
}

/** One subscale's number, once someone has answered. */
data class Reading(
    val subscale: Subscale,
    val value: Int,
    val pass: Int = 0,
) {
    /** The word this number stands for, on a subscale that has a cut. */
    val letter: String?
        get() = subscale.cut?.let { if (value > it) subscale.high else subscale.low }

    /**
     * How far from the threshold, 0f at the cut and 1f at either extreme.
     *
     * The reason to keep this is that a letter won 25 to 24 should not look
     * like a letter won 39 to 8, and a four-letter type that hides its margins
     * is claiming a precision the instrument does not have.
     */
    val margin: Float
        get() {
            val cut = subscale.cut ?: return 1f
            val r = subscale.range
            val reach = if (value > cut) (r.last - cut) else (cut - r.first)
            if (reach <= 0) return 0f
            return (kotlin.math.abs(value - cut).toFloat() / reach).coerceIn(0f, 1f)
        }

    /** Position in the subscale's own range, for drawing the shape behind a badge. */
    val fraction: Float
        get() {
            val r = subscale.range
            val span = (r.last - r.first).toFloat()
            if (span <= 0f) return 0f
            return ((value - r.first) / span).coerceIn(0f, 1f)
        }
}

/**
 * One couple's sitting with one instrument.
 *
 * Sealed exactly the way a day is sealed: you answer, it sits, and there is no
 * result for anybody until the second person finishes. A wall of half-filled
 * rows would read as nagging. A wall where every filled row means you both
 * showed up reads as the thing the app is for.
 */
data class InstrumentRun(
    val instrumentId: String,
    val takenOn: LocalDate,
    /** uid to responses, in item order, every pass concatenated. */
    val answers: Map<String, List<Int>> = emptyMap(),
    /**
     * uid to how many responses they have given so far, sealed or not.
     *
     * The only thing about a half-finished sitting that both of you can see.
     * A count is safe to share where the answers are not: knowing Maya is
     * twenty questions in tells you she is on round two, and nothing about
     * what she said.
     */
    val progress: Map<String, Int> = emptyMap(),
    /**
     * Your own saved-but-unsealed answers, if you left part way.
     *
     * Only ever yours -- the rules make a draft readable to its owner alone, so
     * there is nowhere for a partner's to come from even if this tried to hold
     * one. Empty once sealed.
     */
    val draft: List<Int> = emptyList(),
) {
    val isOpen: Boolean get() = answers.size >= 2

    fun has(uid: String): Boolean = answers.containsKey(uid)

    fun responsesFor(uid: String): List<Int>? = answers[uid]

    /**
     * How far [uid] has got, preferring a finished run over a running count.
     *
     * A sealed run is the truth even if a stale progress number disagrees --
     * the two are written separately and a phone that died between them would
     * otherwise report somebody as mid-round forever.
     */
    fun answeredBy(uid: String, of: Int): Int =
        if (answers.containsKey(uid)) of else (progress[uid] ?: 0).coerceIn(0, of)
}
