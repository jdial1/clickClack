package com.ampersand.data

import com.ampersand.model.InstrumentRun
import com.ampersand.model.Member
import com.ampersand.model.Reading

/**
 * Two finished results side by side, then one question about them. Null until
 * both of you have finished.
 *
 * Never what the results mean together, and never advice: the app does not
 * interpret, it asks. The reading is the couple's to do (DESIGN.md, "Never coach").
 */
object CoupleReadings {

    fun of(instrumentId: String, run: InstrumentRun?, you: Member, them: Member): String? {
        if (run?.isOpen != true) return null
        val mine = run.responsesFor(you.uid) ?: return null
        val theirs = run.responsesFor(them.uid) ?: return null
        val a = you.name.ifBlank { "You" }
        val b = them.name.ifBlank { "your partner" }
        return when (instrumentId) {
            Instruments.JUNGIAN.id -> type(Instruments.JUNGIAN.readings(mine), Instruments.JUNGIAN.readings(theirs), a, b)
            Instruments.LOVE_ATTITUDES.id -> loving(Instruments.LOVE_ATTITUDES.readings(mine), Instruments.LOVE_ATTITUDES.readings(theirs), a, b)
            Instruments.LOVE_LANGUAGES.id -> languages(mine, theirs, a, b)
            Instruments.ATTACHMENT.id -> attachment(
                BadgeWall.attachmentStyle(Instruments.ATTACHMENT.readings(mine)),
                BadgeWall.attachmentStyle(Instruments.ATTACHMENT.readings(theirs)), a, b,
            )
            Instruments.DISC.id -> disc(Instruments.DISC.readings(mine), Instruments.DISC.readings(theirs), a, b)
            Instruments.ENNEAGRAM.id -> enneagram(Instruments.ENNEAGRAM.readings(mine), Instruments.ENNEAGRAM.readings(theirs), a, b)
            Instruments.LOVE_SPECTRUM.id -> spectrum(Instruments.autismTraits(mine), Instruments.autismTraits(theirs), a, b)
            else -> null
        }
    }

    private val TypeMeaning = mapOf(
        "I" to "recharge with time alone", "E" to "get energy from being around people",
        "S" to "trust facts and what's in front of them", "N" to "look for patterns and possibilities",
        "T" to "decide with the head", "F" to "decide with the heart",
        "J" to "like plans settled", "P" to "like keeping options open",
    )

    private fun type(mine: List<Reading>, theirs: List<Reading>, a: String, b: String): String {
        val pairs = mine.zip(theirs)
        val shared = pairs.filter { (x, y) -> x.letter == y.letter }.mapNotNull { TypeMeaning[it.first.letter] }
        val differ = pairs.filter { (x, y) -> x.letter != y.letter }
        return buildString {
            if (shared.isNotEmpty()) append("You both ${shared.joinToString(", ")}. ")
            if (differ.isEmpty()) {
                append("When has being alike made things easy, and when has it caught you both out?")
            } else {
                differ.forEach { (x, y) -> append("$a tends to ${TypeMeaning[x.letter]}, and $b tends to ${TypeMeaning[y.letter]}. ") }
                append("Where does that show up on an ordinary Sunday?")
            }
        }
    }

    private val LovingMeaning = mapOf(
        "eros" to "feels love fast and physically",
        "storge" to "loves through deep friendship",
        "pragma" to "loves with an eye on the life you're building",
        "agape" to "loves by putting the other first",
    )

    private fun loving(mine: List<Reading>, theirs: List<Reading>, a: String, b: String): String {
        val ta = mine.maxBy { it.value }.subscale
        val tb = theirs.maxBy { it.value }.subscale
        return if (ta.id == tb.id) {
            "You're both ${ta.label}: you both ${LovingMeaning[ta.id]?.replaceFirst("feels", "feel")?.replaceFirst("loves", "love")}. " +
                "When did you first notice that in each other?"
        } else {
            "$a is ${ta.label} and $b is ${tb.label}. $a ${LovingMeaning[ta.id]}, and $b ${LovingMeaning[tb.id]}. " +
                "What does each of you do that the other might not read as love?"
        }
    }

    private fun languages(mine: List<Int>, theirs: List<Int>, a: String, b: String): String {
        val kit = Instruments.LOVE_LANGUAGES
        fun top(responses: List<Int>, pass: Int) = kit.readings(responses, pass).maxBy { it.value }.subscale.label.lowercase()
        val aWants = top(mine, Instruments.FELT)
        val aGives = top(mine, Instruments.GIVEN)
        val bWants = top(theirs, Instruments.FELT)
        val bGives = top(theirs, Instruments.GIVEN)
        return "$a feels most loved through $aWants, and $b tends to give $bGives. " +
            "$b feels most loved through $bWants, and $a tends to give $aGives. " +
            "When did one of you last get it exactly right?"
    }

    private fun attachment(x: String, y: String, a: String, b: String): String {
        return "$a is $x and $b is $y. When things get tense, what does each of you reach for first? Styles can shift over time."
    }

    private val DiscMeaning = mapOf(
        "dominant" to "decides quickly and pushes things forward",
        "influencer" to "brings the energy and talks things through",
        "steady" to "keeps things calm and steady",
        "conscientious" to "gets the details right",
    )

    private fun disc(mine: List<Reading>, theirs: List<Reading>, a: String, b: String): String {
        val ta = mine.maxBy { it.value }.subscale
        val tb = theirs.maxBy { it.value }.subscale
        return if (ta.id == tb.id) {
            "You're both ${ta.label}: you each ${DiscMeaning[ta.id]?.replace("decides", "decide")?.replace("brings", "bring")?.replace("keeps", "keep")?.replace("gets", "get")?.replace("pushes", "push")?.replace("talks", "talk")}. Who ends up taking on the thing neither of you wants?"
        } else {
            "$a ${DiscMeaning[ta.id]}, and $b ${DiscMeaning[tb.id]}. Where does each of you end up leading without deciding to?"
        }
    }

    private val Gifts = listOf(
        "high standards", "care for others", "drive", "depth of feeling", "curiosity",
        "loyalty", "sense of fun", "strength", "calm",
    )

    private fun enneagram(mine: List<Reading>, theirs: List<Reading>, a: String, b: String): String {
        val ia = mine.indexOf(mine.maxBy { it.value })
        val ib = theirs.indexOf(theirs.maxBy { it.value })
        return if (ia == ib) {
            "You're both the ${Instruments.EnneagramTypes[ia]}, sharing the same ${Gifts[ia]}. What does the other one see in you that you don't?"
        } else {
            "$a brings ${Gifts[ia]} as the ${Instruments.EnneagramTypes[ia]}, and $b brings ${Gifts[ib]} as the ${Instruments.EnneagramTypes[ib]}. When did you last lean on what the other brings?"
        }
    }

    private fun spectrum(x: Int, y: Int, a: String, b: String): String {
        val many = Instruments.AutismReferral
        return when {
            x < 3 && y < 3 -> "Neither of you shows many autistic traits here."
            x >= many && y >= many -> "You both show many autistic traits."
            else -> {
                val more = if (x > y) a else b
                "$more shows more autistic traits than the other."
            }
        } + " How does each of you notice details and read a room? This is a screen, not a diagnosis."
    }
}
