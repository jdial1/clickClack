package com.ampersand.data

import com.ampersand.model.InstrumentRun
import com.ampersand.model.Member
import com.ampersand.model.Reading

/**
 * A few sentences on what two finished results mean together. Null until both of
 * you have finished. Plain and warm; never a verdict on the relationship.
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
                append("With the same type you'll rarely need to explain yourselves, so watch for the blind spots you share.")
            } else {
                differ.forEach { (x, y) -> append("$a tends to ${TypeMeaning[x.letter]}, while $b tends to ${TypeMeaning[y.letter]}. ") }
                append("Those differences are where you cover for each other.")
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
                "You probably recognize love when it arrives the same way, so try it the other way now and then."
        } else {
            "$a is ${ta.label} and $b is ${tb.label}. $a ${LovingMeaning[ta.id]}, and $b ${LovingMeaning[tb.id]}. " +
                "Neither is more loving; say out loud what makes love feel real, because you may be showing it differently."
        }
    }

    private fun languages(mine: List<Int>, theirs: List<Int>, a: String, b: String): String {
        val kit = Instruments.LOVE_LANGUAGES
        fun top(responses: List<Int>, pass: Int) = kit.readings(responses, pass).maxBy { it.value }.subscale.label.lowercase()
        val aWants = top(mine, Instruments.FELT)
        val aGives = top(mine, Instruments.GIVEN)
        val bWants = top(theirs, Instruments.FELT)
        val bGives = top(theirs, Instruments.GIVEN)
        val match = aWants == bGives && bWants == aGives
        return "$a feels most loved through $aWants, and $b tends to give $bGives. " +
            "$b feels most loved through $bWants, and $a tends to give $aGives. " +
            if (match) "You're already speaking each other's language." else "A little more $aWants for $a and $bWants for $b would land well."
    }

    private fun attachment(x: String, y: String, a: String, b: String): String {
        val pair = setOf(x, y)
        val body = when {
            x == "Secure" && y == "Secure" -> "You're both secure: you can lean on each other, and give each other space, without either feeling like a threat."
            pair == setOf("Anxious", "Avoidant") -> {
                val anx = if (x == "Anxious") a else b
                val avo = if (x == "Avoidant") a else b
                "When things get tense, $anx may reach for closeness while $avo needs room. It's a common pattern; naming it out loud takes the sting out of it."
            }
            "Secure" in pair -> {
                val steady = if (x == "Secure") a else b
                val other = if (x == "Secure") b else a
                "$steady is steady ground, which helps $other feel safe to be close. Keep checking in, even when things feel fine."
            }
            x == y && x == "Anxious" -> "You both notice distance quickly. Small, regular reassurance goes a long way for both of you."
            x == y && x == "Avoidant" -> "You both value independence. Make time for closeness on purpose, so it doesn't only happen by accident."
            else -> "At least one of you can want closeness and worry about it at the same time. Patience and consistency help most."
        }
        return "$a is $x and $b is $y. $body Styles can shift over time."
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
            "You're both ${ta.label}: you each ${DiscMeaning[ta.id]?.replace("decides", "decide")?.replace("brings", "bring")?.replace("keeps", "keep")?.replace("gets", "get")?.replace("pushes", "push")?.replace("talks", "talk")}, so you'll understand each other's pace. Watch for the moments neither of you wants to take on."
        } else {
            "$a ${DiscMeaning[ta.id]}, and $b ${DiscMeaning[tb.id]}. Let each of you lead where you're strongest."
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
            "You're both the ${Instruments.EnneagramTypes[ia]}, sharing the same ${Gifts[ia]}. You'll get each other easily; make room for what neither of you naturally brings."
        } else {
            "$a brings ${Gifts[ia]} as the ${Instruments.EnneagramTypes[ia]}, and $b brings ${Gifts[ib]} as the ${Instruments.EnneagramTypes[ib]}. Together that's a wider range than either of you alone."
        }
    }

    private fun spectrum(x: Int, y: Int, a: String, b: String): String {
        val many = Instruments.AutismReferral
        return when {
            x < 3 && y < 3 -> "Neither of you shows many autistic traits here."
            x >= many && y >= many -> "You both show many autistic traits. You may already share a way of seeing the world that others miss."
            else -> {
                val more = if (x > y) a else b
                "$more shows more autistic traits than the other. Talk about how you each notice details and read people; different is not wrong."
            }
        } + " This is a screen, not a diagnosis."
    }
}
