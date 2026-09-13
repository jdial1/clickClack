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
import com.ampersand.model.Standing
import kotlin.math.abs
import kotlin.math.roundToInt

/** Builds the Us page's test results, one group per test. Results appear only once both have finished. */
object BadgeWall {

    fun groups(runs: List<InstrumentRun>, you: Member, them: Member): List<BadgeGroup> = listOf(
        group(runs, Instruments.JUNGIAN, you, them) {
            // Single row, so no sub-label.
            listOf(typeRow(it, you, them))
        },
        group(runs, Instruments.LOVE_ATTITUDES, you, them) {
            listOf(styleRow(it, you, them))
        },
        group(runs, Instruments.LOVE_LANGUAGES, you, them) {
            listOf(
                languageRow(it, you, them, Instruments.FELT,
                    "What makes you feel loved", "wanted"),
                languageRow(it, you, them, Instruments.GIVEN,
                    "What you each tend to do", "given"),
            )
        },
    )

    private fun group(
        runs: List<InstrumentRun>,
        instrument: Instrument,
        you: Member,
        them: Member,
        rows: (InstrumentRun?) -> List<BadgeRow>,
    ): BadgeGroup {
        val yours = standing(runs, instrument, you.uid)
        val theirs = standing(runs, instrument, them.uid)
        val (note, action) = caption(yours, theirs, them.name)
        return BadgeGroup(
            instrumentId = instrument.id,
            title = instrument.title,
            about = instrument.blurb,
            minutes = instrument.minutes,
            partnerName = them.name,
            standing = note,
            action = action,
            yourTurn = !yours.finished,
            rows = rows(runs.of(instrument)),
        )
    }

    private fun List<InstrumentRun>.of(instrument: Instrument): InstrumentRun? =
        lastOrNull { it.instrumentId == instrument.id }

    /** How far [uid] has got with one test. Never null, even before anyone starts. */
    fun standing(
        runs: List<InstrumentRun>,
        instrument: Instrument,
        uid: String,
    ): Standing {
        val run = runs.of(instrument) ?: return Standing.none(instrument.length, instrument.passCount)
        return instrument.standingOf(run.answeredBy(uid, instrument.length))
    }

    /** (progress line, action label) for a test, from both people's progress. */
    fun caption(yours: Standing, theirs: Standing, them: String): Pair<String, String> = when {
        yours.finished && theirs.inProgress ->
            (them + " is " + at(theirs)) to ("Waiting on " + them)

        yours.finished ->
            "You are done" to ("Waiting on " + them)

        // Same place: say it once instead of repeating it for each person.
        yours.inProgress && theirs.inProgress && at(yours) == at(theirs) ->
            ("Both of you are " + at(yours)) to "Carry on"

        yours.inProgress && theirs.inProgress ->
            ("You are " + at(yours) + ", " + them + " is " + at(theirs)) to "Carry on"

        yours.inProgress ->
            ("You are " + at(yours)) to "Carry on"

        theirs.finished ->
            (them + " has finished") to "Take it"

        theirs.inProgress ->
            (them + " is " + at(theirs)) to "Take it"

        else -> "" to "Take it"
    }

    /** "on round 2" where there are rounds, "12 of 32" where there are not. */
    private fun at(standing: Standing): String =
        if (standing.hasRounds) "on round " + (standing.pass + 1) + " of " + standing.passes
        else standing.answered.toString() + " of " + standing.total

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
                    // Show the score so close calls are visible.
                    value = "${r.letter} · ${r.value} of ${r.subscale.range.last}",
                )
            },
            note = readings.minByOrNull { it.margin }?.let {
                if (it.margin > 0.12f) null
                else "${it.subscale.label} was very close. " +
                    "That letter could go the other way another day."
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

    /** Love style badge: the top style as the result, and the runner-up in the note. */
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
                        "equally. Nobody is only one of these."
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
            // Highest score as the headline; the bars show all five.
            result = readings?.maxByOrNull { it.value }?.subscale?.label,
            detail = readings.orEmpty().map { r ->
                BadgeBar(r.subscale.label, r.fraction, "${r.value} of ${r.subscale.range.last}")
            },
            note = readings?.let { "Highest first. All five matter." },
            instrumentId = kit.id,
            minutes = kit.minutes,
            waitingOnThem = run != null && !open && run.has(of.uid),
        )
    }

    // ---- the gap ---------------------------------------------------------

        private fun readings(run: InstrumentRun?, uid: String, pass: Int): List<Reading> =
        if (run?.isOpen != true) emptyList()
        else run.responsesFor(uid)?.let { Instruments.LOVE_LANGUAGES.readings(it, pass) }.orEmpty()

    /** A gap this size or smaller counts as lined up. A quarter of 4..20. */
    private const val ALIGNED = 4

    /** The widest a gap can be on a 4..20 subscale. */
    private const val SPAN = 16

    /** Overall fit as a percent. */
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
