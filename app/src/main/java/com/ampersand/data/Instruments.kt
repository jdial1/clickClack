package com.ampersand.data

import com.ampersand.model.Cadence
import com.ampersand.model.Fidelity
import com.ampersand.model.Instrument
import com.ampersand.model.InstrumentItem
import com.ampersand.model.Source
import com.ampersand.model.Subscale

/**
 * Where everything in the app came from.
 *
 * Licences are spelled out rather than summarised, because "free to use" is a
 * thing people say about research instruments and is almost never a licence.
 * Two of these have a real one. The rest are ordinary journal articles whose
 * item text belongs to their authors, which is fine for two phones and is not
 * fine for a listing -- see [Source.fidelity] and the note on each.
 */
object Sources {

    val OEJTS = Source(
        id = "oejts",
        authors = "Eric Jorgenson",
        year = 2015,
        title = "Open Extended Jungian Type Scales 1.2",
        venue = "Open Source Psychometrics Project",
        url = "https://openpsychometrics.org/tests/OJTS/development/OEJTS1.2.pdf",
        licence = "CC BY-NC-SA 4.0",
        fidelity = Fidelity.VERBATIM,
    )

    /**
     * The design is Mostova's; the twenty behaviours are Egbert and Polk's.
     *
     * PLOS ONE is CC BY, so the method can be reproduced with attribution -- but
     * the *items* originate in a paywalled Communication Research Reports paper
     * and are not covered by it. So the behaviours below are ours, which is why
     * this is [Fidelity.INSPIRED] and why the credit line names both.
     */
    val LOVE_LANGUAGES = Source(
        id = "lovelang",
        authors = "Mostova, Stolarski & Matthews",
        year = 2022,
        title = "I love the way you love me: Responding to partner's love " +
            "language preferences boosts satisfaction in romantic heterosexual couples",
        venue = "PLOS ONE, 17(6), e0269429",
        url = "https://doi.org/10.1371/journal.pone.0269429",
        licence = "CC BY 4.0 (article). Items after Egbert & Polk (2006), rewritten.",
        fidelity = Fidelity.INSPIRED,
    )

    val ARON = Source(
        id = "aron1997",
        authors = "Aron, Melinat, Aron, Vallone & Bator",
        year = 1997,
        title = "The experimental generation of interpersonal closeness",
        venue = "Personality and Social Psychology Bulletin, 23(4), 363-377",
        url = "https://doi.org/10.1177/0146167297234003",
        licence = "All rights reserved. Used with attribution, non-commercially.",
        fidelity = Fidelity.VERBATIM,
    )

    val RCIT = Source(
        id = "rcit1999",
        authors = "Sedikides, Campbell, Reeder & Elliot",
        year = 1999,
        title = "The Relationship Closeness Induction Task",
        venue = "Representative Research in Social Psychology, 23, 1-4",
        url = "https://psychology.uga.edu/sites/default/files/RCITarticle1999.pdf",
        licence = "All rights reserved. Rewritten for couples.",
        fidelity = Fidelity.ADAPTED,
    )

    private val all = listOf(OEJTS, LOVE_LANGUAGES, ARON, RCIT)

    operator fun get(id: String?): Source? = all.firstOrNull { it.id == id }
}

/**
 * The instruments, whole.
 *
 * Nothing here may be shuffled, subset, or drip-fed into the daily pool. The
 * item order is the published order because the scoring keys index into it, and
 * `Instrument.length` responses is the only input the arithmetic accepts.
 */
object Instruments {

    /**
     * Jorgenson's Open Extended Jungian Type Scales 1.2, verbatim.
     *
     * Thirty-two pairs of opposed descriptions, each rated 1 to 5 for where you
     * sit between them. Four axes of eight items, each scored 8 to 40 by a
     * signed equation with a constant, cut at 24. Four comparisons, four
     * letters, one type -- in four minutes, which makes it the obvious one to
     * ship first.
     *
     * **This is not the MBTI.** Myers-Briggs is a trademark and a proprietary,
     * per-administration instrument; the OEJTS is an independent open
     * implementation of the same Jungian axes and the app must never call it
     * anything else.
     *
     * Two departures from the PDF, both cosmetic and both noted here so the
     * [Fidelity.VERBATIM] claim stays honest: "sceptical" is spelled the
     * American way, and the original's "where ever" is closed up.
     */
    val JUNGIAN = Instrument(
        id = "oejts",
        sourceId = "oejts",
        title = "Type",
        // Carries the wall heading as well as the top of the sitting, so it
        // has to say what the test asks *and* what comes out of it. A badge
        // reading "INTJ" is a result with no question attached to it, and
        // nobody remembers in April what they answered in January.
        blurb = "Thirty-two either-ors: makes lists or relies on memory, " +
            "energetic or mellow. Eight of them on each of four axes, and where " +
            "you land on all four is the four letters.",
        cadence = Cadence.ONCE,
        minutes = 4,
        items = listOf(
            InstrumentItem("makes lists", "relies on memory"),                                  // 1
            InstrumentItem("skeptical", "wants to believe"),                                    // 2
            InstrumentItem("bored by time alone", "needs time alone"),                          // 3
            InstrumentItem("accepts things as they are", "unsatisfied with the way things are"),// 4
            InstrumentItem("keeps a clean room", "just puts stuff wherever"),                   // 5
            InstrumentItem("thinks \"robotic\" is an insult", "strives to have a mechanical mind"), // 6
            InstrumentItem("energetic", "mellow"),                                              // 7
            InstrumentItem("prefer to take multiple choice test", "prefer essay answers"),      // 8
            InstrumentItem("chaotic", "organized"),                                             // 9
            InstrumentItem("easily hurt", "thick-skinned"),                                     // 10
            InstrumentItem("works best in groups", "works best alone"),                         // 11
            InstrumentItem("focused on the present", "focused on the future"),                  // 12
            InstrumentItem("plans far ahead", "plans at the last minute"),                      // 13
            InstrumentItem("wants people's respect", "wants their love"),                       // 14
            InstrumentItem("gets worn out by parties", "gets fired up by parties"),             // 15
            InstrumentItem("fits in", "stands out"),                                            // 16
            InstrumentItem("keeps options open", "commits"),                                    // 17
            InstrumentItem("wants to be good at fixing things", "wants to be good at fixing people"), // 18
            InstrumentItem("talks more", "listens more"),                                       // 19
            InstrumentItem(
                "when describing an event, will tell people what happened",
                "when describing an event, will tell people what it meant",
            ),                                                                                  // 20
            InstrumentItem("gets work done right away", "procrastinates"),                      // 21
            InstrumentItem("follows the heart", "follows the head"),                            // 22
            InstrumentItem("stays at home", "goes out on the town"),                            // 23
            InstrumentItem("wants the big picture", "wants the details"),                       // 24
            InstrumentItem("improvises", "prepares"),                                           // 25
            InstrumentItem("bases morality on justice", "bases morality on compassion"),        // 26
            InstrumentItem(
                "finds it difficult to yell very loudly",
                "yelling to others when they are far away comes naturally",
            ),                                                                                  // 27
            InstrumentItem("theoretical", "empirical"),                                         // 28
            InstrumentItem("works hard", "plays hard"),                                         // 29
            InstrumentItem("uncomfortable with emotions", "values emotions"),                   // 30
            InstrumentItem("likes to perform in front of other people", "avoids public speaking"), // 31
            InstrumentItem("likes to know \"who?\", \"what?\", \"when?\"", "likes to know \"why?\""), // 32
        ),
        // The published equations, translated to 0-based indices. Signs and
        // constants are exactly as printed -- do not "simplify" these.
        subscales = listOf(
            // IE = 30 - Q3 - Q7 - Q11 + Q15 - Q19 + Q23 + Q27 - Q31
            Subscale(
                id = "IE", label = "Inward or outward", constant = 30, cut = 24,
                low = "I", high = "E",
                terms = mapOf(2 to -1, 6 to -1, 10 to -1, 14 to 1, 18 to -1, 22 to 1, 26 to 1, 30 to -1),
            ),
            // SN = 12 + Q4 + Q8 + Q12 + Q16 + Q20 - Q24 - Q28 + Q32
            Subscale(
                id = "SN", label = "Facts or patterns", constant = 12, cut = 24,
                low = "S", high = "N",
                terms = mapOf(3 to 1, 7 to 1, 11 to 1, 15 to 1, 19 to 1, 23 to -1, 27 to -1, 31 to 1),
            ),
            // FT = 30 - Q2 + Q6 + Q10 - Q14 - Q18 + Q22 - Q26 - Q30
            Subscale(
                id = "FT", label = "Heart or head", constant = 30, cut = 24,
                low = "F", high = "T",
                terms = mapOf(1 to -1, 5 to 1, 9 to 1, 13 to -1, 17 to -1, 21 to 1, 25 to -1, 29 to -1),
            ),
            // JP = 18 + Q1 + Q5 - Q9 + Q13 - Q17 + Q21 - Q25 + Q29
            Subscale(
                id = "JP", label = "Settled or open", constant = 18, cut = 24,
                low = "J", high = "P",
                terms = mapOf(0 to 1, 4 to 1, 8 to -1, 12 to 1, 16 to -1, 20 to 1, 24 to -1, 28 to 1),
            ),
        ),
    )

    /**
     * Love languages, in both directions.
     *
     * Twenty behaviours, four per language, answered twice: once for what makes
     * you feel loved and once for what you actually do. Mostova and colleagues
     * ran it this way across a hundred couples and found the *mismatch* -- the
     * gap between what one partner wants and what the other gives -- correlated
     * with relationship satisfaction (r about -.36 and -.40). That gap is the
     * whole reason this is here: it is the only source in the app whose headline
     * finding is already a two-person calculation rather than one person's score.
     *
     * The behaviours are written here rather than lifted. The five-category
     * model itself is shakier than its popularity suggests -- Impett, Park and
     * Muise (2024) found little support for the claims that each person has one
     * language, that there are five, or that matching is what drives
     * satisfaction, and proposed love as a balanced diet of needs instead. So
     * the UI shows all five as a shape and names the highest as a headline,
     * rather than telling anybody what they *are*. The mismatch number survives
     * that critique regardless, because it measures the gap and not the label.
     *
     * Items are subject-free noun phrases on purpose: the same twenty have to
     * read correctly under both stems without changing a pronoun.
     */
    val LOVE_LANGUAGES = Instrument(
        id = "lovelang",
        sourceId = "lovelang",
        title = "Love language",
        blurb = "Twenty small things, rated twice: once for what makes you feel " +
            "loved, once for what you tend to do for them. The part that matters " +
            "is the gap between what one of you wants and what the other gives.",
        cadence = Cadence.YEARLY,
        minutes = 6,
        passes = listOf(
            "This is what makes me feel loved",
            "This is how I tend to show it",
        ),
        items = listOf(
            // Words
            InstrumentItem("Not really", "Very much", "Appreciation said out loud"),
            InstrumentItem("Not really", "Very much", "Praise for something done well"),
            InstrumentItem("Not really", "Very much", "Encouragement when something is hard"),
            InstrumentItem("Not really", "Very much", "A note, a message, a card"),
            // Time
            InstrumentItem("Not really", "Very much", "Full attention, phone out of reach"),
            InstrumentItem("Not really", "Very much", "An evening set aside for two"),
            InstrumentItem("Not really", "Very much", "An ordinary errand done together"),
            InstrumentItem("Not really", "Very much", "Sitting and talking long after the meal"),
            // Gifts
            InstrumentItem("Not really", "Very much", "Something small brought back from a trip"),
            InstrumentItem("Not really", "Very much", "A thing mentioned months ago, remembered"),
            InstrumentItem("Not really", "Very much", "An occasion marked with something to keep"),
            InstrumentItem("Not really", "Very much", "A favorite thing picked up unasked"),
            // Acts
            InstrumentItem("Not really", "Very much", "A chore taken off the list"),
            InstrumentItem("Not really", "Very much", "The dreaded errand handled"),
            InstrumentItem("Not really", "Very much", "A favorite meal made"),
            InstrumentItem("Not really", "Very much", "Something broken, quietly fixed"),
            // Touch
            InstrumentItem("Not really", "Very much", "A hand reached for without thinking"),
            InstrumentItem("Not really", "Very much", "A hug longer than a greeting"),
            InstrumentItem("Not really", "Very much", "Sitting close enough to touch"),
            InstrumentItem("Not really", "Very much", "A kiss on the way past"),
        ),
        subscales = listOf(
            Subscale("words", "Words", terms = (0..3).associateWith { 1 }),
            Subscale("time", "Time", terms = (4..7).associateWith { 1 }),
            Subscale("gifts", "Gifts", terms = (8..11).associateWith { 1 }),
            Subscale("acts", "Acts of service", terms = (12..15).associateWith { 1 }),
            Subscale("touch", "Touch", terms = (16..19).associateWith { 1 }),
        ),
    )

    /** Shortest first: the four-minute one is the one people actually finish. */
    val all = listOf(JUNGIAN, LOVE_LANGUAGES)

    operator fun get(id: String?): Instrument? = all.firstOrNull { it.id == id }

    /** Which pass of [instrument] a response index belongs to. */
    const val FELT = 0
    const val GIVEN = 1
}
