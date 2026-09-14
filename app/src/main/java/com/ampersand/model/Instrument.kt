package com.ampersand.model


/** How closely the app reproduces a source, which decides the credit line. */
enum class Fidelity {
    /** Their items, their order, their wording. */
    VERBATIM,

    /** Their items, shortened or reworded. The published psychometrics no longer apply. */
    ADAPTED,

    /** Our words, their idea. Cite the paper for the construct, not the text. */
    INSPIRED,
}

/** Where a question or an instrument came from. Credit is shown after the reveal. */
data class Source(
    val id: String,
    val authors: String,
    val year: Int,
    val title: String,
    val venue: String,
    val url: String,
    /** The license, spelled out. */
    val licence: String,
    val fidelity: Fidelity,
) {
    /** "Aron et al. (1997)", short enough to fit under a question. No year when the source gives none. */
    val credit: String get() = if (year > 0) "$authors ($year)" else authors
}

/**
 * One rated item, answered 1..5.
 *
 * A pole pair has no [text]; you place yourself between [low] and [high].
 * A statement has [text] and the poles are agreement anchors.
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
 * Items that combine into one number: `constant + sum(sign * response)`.
 *
 * Signed [terms] plus [constant] mirror the published Jungian scoring
 * (`IE = 30 - Q3 - Q7 ...`). Above [cut] reads as [high], at or below as [low];
 * null [cut] means a plain score.
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

/**
 * A whole instrument, answered in one sitting in published order.
 *
 * Not a `Question`: its scoring only holds if every item is answered together.
 * Each entry in [passes] asks the full item list again with a different stem,
 * so a run stores `items.size * passes.size` responses.
 */
data class Instrument(
    val id: String,
    val sourceId: String,
    val title: String,
    /** One line, shown on the locked badge and at the top of the sitting. */
    val blurb: String,
    val items: List<InstrumentItem>,
    val subscales: List<Subscale>,
    val minutes: Int,
    /** The stem each pass puts in front of every item. One entry = one pass. */
    val passes: List<String> = listOf(""),
) {
    val passCount: Int get() = passes.size

    /** Total responses one person gives: every item, once per pass. */
    val length: Int get() = items.size * passCount

    /** Progress after [answered] responses. Single-pass instruments just count. */
    fun standingOf(answered: Int): Standing {
        val safe = answered.coerceIn(0, length)
        return Standing(
            answered = safe,
            total = length,
            // The pass you are in, so a finished pass 1 reads as pass 2.
            pass = if (items.isEmpty()) 0 else (safe / items.size).coerceAtMost(passCount - 1),
            passes = passCount,
        )
    }

    /** Whether the sitting is long enough to show progress. */
    val needsProgress: Boolean get() = length > ProgressFloor

    /** The sitting split into pages of [perPage] items. A page never spans two passes. */
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
        return subscales.map { Reading(it, it.scoreOf(slice)) }
    }
}

/**
 * How far one person has got with one instrument. The only part of an
 * unfinished sitting visible to the partner.
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

    /** True when the instrument has more than one round. */
    val hasRounds: Boolean get() = passes > 1

    companion object {
        fun none(total: Int, passes: Int) = Standing(0, total, 0, passes)
    }
}

/** Items per page in a sitting. */
const val PageSize = 10

/** At or below this many responses, no progress is shown. */
const val ProgressFloor = 5

/**
 * One page of a sitting.
 *
 * [items] indexes `Instrument.items` (relative to the pass); [slots] indexes the
 * flat response list across all passes.
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
) {
    /** The word this number stands for, on a subscale that has a cut. */
    val letter: String?
        get() = subscale.cut?.let { if (value > it) subscale.high else subscale.low }

    /** Distance from the cut: 0f at the cut, 1f at either extreme. 1f when there is no cut. */
    val margin: Float
        get() {
            val cut = subscale.cut ?: return 1f
            val r = subscale.range
            val reach = if (value > cut) (r.last - cut) else (cut - r.first)
            if (reach <= 0) return 0f
            return (kotlin.math.abs(value - cut).toFloat() / reach).coerceIn(0f, 1f)
        }

    /** Position in the subscale's own range, 0f..1f. */
    val fraction: Float
        get() {
            val r = subscale.range
            val span = (r.last - r.first).toFloat()
            if (span <= 0f) return 0f
            return ((value - r.first) / span).coerceIn(0f, 1f)
        }
}

/** One couple's sitting with one instrument. Sealed until both have finished. */
data class InstrumentRun(
    val instrumentId: String,
    /** uid to responses, in item order, every pass concatenated. */
    val answers: Map<String, List<Int>> = emptyMap(),
    /** uid to how many responses they have given so far, sealed or not. Visible to both. */
    val progress: Map<String, Int> = emptyMap(),
    /** Your own saved but unsealed answers. Rules make drafts owner-only. Empty once sealed. */
    val draft: List<Int> = emptyList(),
) {
    val isOpen: Boolean get() = answers.size >= 2

    fun has(uid: String): Boolean = answers.containsKey(uid)

    fun responsesFor(uid: String): List<Int>? = answers[uid]

    /**
     * How far [uid] has got. A sealed run wins over [progress], since the two are
     * written separately and a stale count could otherwise stick forever.
     */
    fun answeredBy(uid: String, of: Int): Int =
        if (answers.containsKey(uid)) of else (progress[uid] ?: 0).coerceIn(0, of)
}
