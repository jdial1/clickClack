package com.ampersand.data

import com.ampersand.model.Badge
import com.ampersand.model.BadgeBar
import com.ampersand.model.BadgeGroup
import com.ampersand.model.BadgeRow
import com.ampersand.model.Instrument
import com.ampersand.model.InstrumentRun
import com.ampersand.model.Member
import com.ampersand.model.Owner
import com.ampersand.model.Reading
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The Us page's results, one heading per test.
 *
 * Grouped by the sitting that produced them rather than laid out as peers.
 * Love languages yields three courses on its own -- what you want, what you
 * give, and the fit between them -- and as three sibling rows they read as
 * three unrelated tests sharing a word, when in fact the third only means
 * anything because the first two exist.
 *
 * Every badge needs both of you. A course fills when the second person
 * finishes, never before. A wall of half-filled rows would read as nagging; a
 * wall where every filled row means you both showed up reads as the thing the
 * app is for.
 */
object BadgeWall {

    fun groups(runs: List<InstrumentRun>, you: Member, them: Member): List<BadgeGroup> {
        val jungian = runs.of(Instruments.JUNGIAN)
        val language = runs.of(Instruments.LOVE_LANGUAGES)
        val style = runs.of(Instruments.LOVE_ATTITUDES)
        return listOf(
            BadgeGroup(
                instrumentId = Instruments.JUNGIAN.id,
                title = Instruments.JUNGIAN.title,
                about = Instruments.JUNGIAN.blurb,
                minutes = Instruments.JUNGIAN.minutes,
                // One course, so no sub-label: a heading above a single row
                // that repeats the group's own name is noise.
                rows = listOf(typeRow(jungian, you, them)),
            ),
            BadgeGroup(
                instrumentId = Instruments.LOVE_ATTITUDES.id,
                title = Instruments.LOVE_ATTITUDES.title,
                about = Instruments.LOVE_ATTITUDES.blurb,
                minutes = Instruments.LOVE_ATTITUDES.minutes,
                rows = listOf(styleRow(style, you, them)),
            ),
            BadgeGroup(
                instrumentId = Instruments.LOVE_LANGUAGES.id,
                title = Instruments.LOVE_LANGUAGES.title,
                about = Instruments.LOVE_LANGUAGES.blurb,
                minutes = Instruments.LOVE_LANGUAGES.minutes,
                rows = listOf(
                    languageRow(language, you, them, Instruments.FELT,
                        "What makes you feel loved", "wanted"),
                    languageRow(language, you, them, Instruments.GIVEN,
                        "What you each tend to do", "given"),
                    fitRow(language, you, them),
                ),
            ),
        )
    }

    private fun List<InstrumentRun>.of(instrument: Instrument): InstrumentRun? =
        lastOrNull { it.instrumentId == instrument.id }

    // ---- type ------------------------------------------------------------

    private fun typeRow(run: InstrumentRun?, you: Member, them: Member) = BadgeRow(
        label = "",
        badges = listOf(
            typeBadge(run, you, Owner.YOU),
            typeBadge(run, them, Owner.THEM),
        ),
    )

    private fun typeBadge(run: InstrumentRun?, of: Member, owner: Owner): Badge {
        val kit = Instruments.JUNGIAN
        val open = run?.isOpen == true
        val responses = if (open) run.responsesFor(of.uid) else null
        val readings = responses?.let { kit.readings(it) }.orEmpty()

        return Badge(
            slotId = "type.${owner.name.lowercase()}",
            owner = owner,
            who = of.name,
            result = readings.takeIf { it.isNotEmpty() }
                ?.joinToString("") { it.letter.orEmpty() },
            detail = readings.map { r ->
                BadgeBar(
                    label = r.subscale.label,
                    fraction = r.fraction,
                    // The margin, said plainly. A letter won 25 to 24 must not
                    // look like one won 39 to 8.
                    value = "${r.letter} · ${r.value} of ${r.subscale.range.last}",
                )
            },
            note = readings.minByOrNull { it.margin }?.let {
                if (it.margin > 0.12f) null
                else "${it.subscale.label} was close to a coin toss. " +
                    "That letter would likely come out the other way on another day."
            },
            instrumentId = kit.id,
            minutes = kit.minutes,
            waitingOnThem = run != null && !open && run.has(of.uid),
        )
    }

    // ---- love style --------------------------------------------------------

    private fun styleRow(run: InstrumentRun?, you: Member, them: Member) = BadgeRow(
        label = "",
        badges = listOf(
            styleBadge(run, you, Owner.YOU),
            styleBadge(run, them, Owner.THEM),
        ),
    )

    /**
     * How somebody loves, rather than how much.
     *
     * The headline is the leading style and the note names the runner-up,
     * because the whole claim the scale supports is that these are separate
     * dimensions you can be high on two of at once -- reporting one word and
     * stopping would be asserting the opposite of what the paper found.
     */
    private fun styleBadge(run: InstrumentRun?, of: Member, owner: Owner): Badge {
        val kit = Instruments.LOVE_ATTITUDES
        val open = run?.isOpen == true
        val readings = if (open) run.responsesFor(of.uid)?.let { kit.readings(it) } else null
        val ranked = readings?.sortedByDescending { it.value }

        return Badge(
            slotId = "style.${owner.name.lowercase()}",
            owner = owner,
            who = of.name,
            result = ranked?.firstOrNull()?.subscale?.label,
            detail = ranked.orEmpty().map { r ->
                BadgeBar(r.subscale.label, r.fraction, "${r.value} of ${r.subscale.range.last}")
            },
            note = ranked?.let {
                val top = it.getOrNull(0) ?: return@let null
                val second = it.getOrNull(1) ?: return@let null
                if (top.value == second.value) {
                    "${top.subscale.label.lowercase()} and ${second.subscale.label.lowercase()}, " +
                        "evenly. Nobody is only one of these."
                } else {
                    "Mostly ${top.subscale.label.lowercase()}, then " +
                        "${second.subscale.label.lowercase()}. Nobody is only one of these."
                }
            },
            instrumentId = kit.id,
            minutes = kit.minutes,
            waitingOnThem = run != null && !open && run.has(of.uid),
        )
    }

    // ---- love language, one pass -----------------------------------------

    private fun languageRow(
        run: InstrumentRun?,
        you: Member,
        them: Member,
        pass: Int,
        label: String,
        slot: String,
    ) = BadgeRow(
        label = label,
        badges = listOf(
            languageBadge(run, you, Owner.YOU, pass, slot),
            languageBadge(run, them, Owner.THEM, pass, slot),
        ),
    )

    private fun languageBadge(
        run: InstrumentRun?,
        of: Member,
        owner: Owner,
        pass: Int,
        slot: String,
    ): Badge {
        val kit = Instruments.LOVE_LANGUAGES
        val open = run?.isOpen == true
        val readings = if (open) run.responsesFor(of.uid)?.let { kit.readings(it, pass) } else null

        return Badge(
            slotId = "language.$slot.${owner.name.lowercase()}",
            owner = owner,
            who = of.name,
            // The highest, named as a headline -- not as a category anybody is
            // being told they belong to. The bars under it are the real answer.
            result = readings?.maxByOrNull { it.value }?.subscale?.label,
            detail = readings.orEmpty().map { r ->
                BadgeBar(r.subscale.label, r.fraction, "${r.value} of ${r.subscale.range.last}")
            },
            note = readings?.let { "Shown highest-first. All five matter; this is the loudest." },
            instrumentId = kit.id,
            minutes = kit.minutes,
            waitingOnThem = run != null && !open && run.has(of.uid),
        )
    }

    // ---- the gap ---------------------------------------------------------

    /**
     * What one of you wants against what the other actually does.
     *
     * This is the finding, not a derived nicety: Mostova and colleagues computed
     * exactly this discrepancy and it was what correlated with satisfaction.
     * Both directions are measured, because they are different numbers -- you
     * can be well served and still be badly serving.
     *
     * "Lined up" is a gap of four points or less on a scale of four to twenty,
     * which is a quarter of the range. The threshold is ours; the research
     * reports a continuous correlation and offers no cut-off.
     */
    private fun fitRow(run: InstrumentRun?, you: Member, them: Member): BadgeRow {
        val kit = Instruments.LOVE_LANGUAGES
        val open = run?.isOpen == true

        val yourFelt = readings(run, you.uid, Instruments.FELT)
        val yourGiven = readings(run, you.uid, Instruments.GIVEN)
        val theirFelt = readings(run, them.uid, Instruments.FELT)
        val theirGiven = readings(run, them.uid, Instruments.GIVEN)

        val bars = mutableListOf<BadgeBar>()
        var alignedCount = 0
        // label, shortfall, whose. Tracked separately from the bars because the
        // bars are the research's measure and this is the actionable sentence,
        // and they are not the same arithmetic. Mostova's discrepancy is
        // absolute -- any gap in either direction counts against the fit -- but
        // "you want touch more than they give it" and "you give twice the
        // words they need" are opposite problems, and only one of them is a
        // shortfall. An earlier version reported over-giving as neglect.
        var worst: Triple<String, Int, Boolean>? = null

        if (open && yourFelt.isNotEmpty()) {
            kit.subscales.forEachIndexed { i, scale ->
                val toYou = abs(yourFelt[i].value - theirGiven[i].value)
                val toThem = abs(theirFelt[i].value - yourGiven[i].value)
                val gap = maxOf(toYou, toThem)
                if (gap <= ALIGNED) alignedCount++
                bars += BadgeBar(
                    label = scale.label,
                    // Full bar is a perfect match, empty is the widest possible miss.
                    fraction = 1f - (gap / SPAN.toFloat()).coerceIn(0f, 1f),
                    value = if (gap <= ALIGNED) "lines up" else "off by $gap",
                )

                // Signed, and floored at zero: wanting less than you are given
                // is not a shortfall and there is nothing to act on.
                val youShort = (yourFelt[i].value - theirGiven[i].value).coerceAtLeast(0)
                val theyShort = (theirFelt[i].value - yourGiven[i].value).coerceAtLeast(0)
                val short = maxOf(youShort, theyShort)
                if (short > ALIGNED && (worst == null || short > worst!!.second)) {
                    worst = Triple(scale.label, short, theyShort >= youShort)
                }
            }
        }

        val badge = Badge(
            slotId = "language.fit",
            owner = Owner.BOTH,
            who = "Together",
            result = if (open && bars.isNotEmpty()) "Lines up on $alignedCount of ${kit.subscales.size}" else null,
            detail = bars,
            note = when {
                !open || bars.isEmpty() -> null
                worst == null ->
                    "Nobody is going short. Whatever you are both doing, keep doing it."
                worst!!.third ->
                    "${them.name} wants ${worst!!.first.lowercase()} more than you tend to give it."
                else ->
                    "You want ${worst!!.first.lowercase()} more than ${them.name} tends to give it."
            },
            instrumentId = kit.id,
            minutes = kit.minutes,
            waitingOnThem = run != null && !open,
        )
        return BadgeRow("How well those two line up", listOf(badge))
    }

    private fun readings(run: InstrumentRun?, uid: String, pass: Int): List<Reading> =
        if (run?.isOpen != true) emptyList()
        else run.responsesFor(uid)?.let { Instruments.LOVE_LANGUAGES.readings(it, pass) }.orEmpty()

    /** A gap this size or smaller counts as lined up. A quarter of 4..20. */
    private const val ALIGNED = 4

    /** The widest a gap can be on a 4..20 subscale. */
    private const val SPAN = 16

    /** Percent, for anywhere that wants one number instead of five bars. */
    fun fitPercent(run: InstrumentRun?, you: Member, them: Member): Int? {
        val kit = Instruments.LOVE_LANGUAGES
        val yourFelt = readings(run, you.uid, Instruments.FELT)
        if (yourFelt.isEmpty()) return null
        val yourGiven = readings(run, you.uid, Instruments.GIVEN)
        val theirFelt = readings(run, them.uid, Instruments.FELT)
        val theirGiven = readings(run, them.uid, Instruments.GIVEN)

        val gaps = kit.subscales.indices.flatMap { i ->
            listOf(
                abs(yourFelt[i].value - theirGiven[i].value),
                abs(theirFelt[i].value - yourGiven[i].value),
            )
        }
        val mean = gaps.average()
        return ((1.0 - (mean / SPAN)) * 100).roundToInt().coerceIn(0, 100)
    }
}
