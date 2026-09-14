package com.ampersand.data

import com.ampersand.model.Fidelity
import com.ampersand.model.Instrument
import com.ampersand.model.InstrumentItem
import com.ampersand.model.Source
import com.ampersand.model.Subscale

/**
 * Content sources and their licenses, spelled out.
 * Journal articles without an open license keep item text rights with their
 * authors, so check [Source.fidelity] and each note before any public listing.
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
     * Method from Mostova (PLOS ONE, CC BY); behaviors after Egbert and Polk, whose
     * paywalled items are not covered, so ours are rewritten ([Fidelity.INSPIRED]).
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

    /**
     * Shortened by us. The Fetzer Institute compendium reproduces all 42 items with
     * no permission flag for this one; fine for non-commercial use with attribution, not a license.
     */
    val LOVE_ATTITUDES = Source(
        id = "loveattitudes",
        authors = "Hendrick & Hendrick",
        year = 1986,
        title = "A theory and method of love",
        venue = "Journal of Personality and Social Psychology, 50(2), 392-402",
        url = "https://doi.org/10.1037/0022-3514.50.2.392",
        licence = "All rights reserved. Four subscales of six, reworded for couples.",
        fidelity = Fidelity.ADAPTED,
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

    /**
     * Public domain item text. The daily-ipip decks use IPIP-NEO facets, IPIP-VIA,
     * IPIP-HEXACO, Barchard's emotional-intelligence components and the Oregon
     * interest scales, minus clinical and political items. Credited anyway.
     */
    val IPIP = Source(
        id = "ipip",
        authors = "Goldberg, Johnson, Eber, Hogan, Ashton, Cloninger & Gough",
        year = 2006,
        title = "The International Personality Item Pool and the future of public-domain personality measures",
        venue = "Journal of Research in Personality, 40(1), 84-96",
        url = "https://ipip.ori.org/",
        licence = "Public domain. Items from the IPIP-NEO facet scales, reworded.",
        fidelity = Fidelity.ADAPTED,
    )

    /** American Time Use Survey activity lexicon (BLS). Public domain; names reworded. */
    val ATUS = Source(
        id = "atus",
        authors = "US Bureau of Labor Statistics",
        year = 2024,
        title = "American Time Use Survey Activity Lexicon",
        venue = "US Department of Labor",
        url = "https://www.bls.gov/tus/lexicons/lexiconnoex2024.pdf",
        licence = "Public domain (US federal work). Activity names reworded.",
        fidelity = Fidelity.ADAPTED,
    )

    /** CC BY 4.0: credit required, and the rewording must be stated. */
    val HAREL = Source(
        id = "harel2024",
        authors = "Harel & Koslowsky",
        year = 2024,
        title = "Development and validation of the relational behavior interactions scale for couples",
        venue = "Scientific Reports, 14, 8086",
        url = "https://doi.org/10.1038/s41598-024-58901-2",
        licence = "CC BY 4.0. Items adapted to address your partner.",
        fidelity = Fidelity.ADAPTED,
    )

    val CFPB = Source(
        id = "cfpb",
        authors = "Consumer Financial Protection Bureau",
        year = 2015,
        title = "CFPB Financial Well-Being Scale",
        venue = "Consumer Financial Protection Bureau",
        url = "https://www.consumerfinance.gov/data-research/research-reports/financial-well-being-scale/",
        licence = "Public domain (US federal work). Reworded for a couple.",
        fidelity = Fidelity.INSPIRED,
    )

    val ALBUM = Source(
        id = "album",
        authors = "Marcel Proust and Karl Marx",
        year = 1886,
        title = "Confession-album questions (Proust c. 1886 and 1891; Marx 1865)",
        venue = "Victorian parlor albums",
        url = "https://en.wikipedia.org/wiki/Proust_Questionnaire",
        licence = "Public domain. Original album questions, reworded for two.",
        fidelity = Fidelity.ADAPTED,
    )

    val ONESOURCE = Source(
        id = "onesource",
        authors = "Military OneSource, US Department of Defense",
        year = 2024,
        title = "What to Ask Your Partner Before Marriage",
        venue = "militaryonesource.mil",
        url = "https://www.militaryonesource.mil/relationships/married-domestic-partner/questions-to-ask-before-marriage/",
        licence = "Public domain (US federal work). Reworded.",
        fidelity = Fidelity.ADAPTED,
    )

    /** MIT, so the notice travels with the questions: see licenses/README.md. */
    val AZURA = Source(
        id = "azura",
        authors = "OperationAzura",
        year = 2023,
        title = "questionServer",
        venue = "GitHub",
        url = "https://github.com/OperationAzura/questionServer",
        licence = "MIT License, Copyright (c) 2023 OperationAzura. Selected and reworded.",
        fidelity = Fidelity.ADAPTED,
    )

    /** Everything credited, in the order the credits screen lists them. */
    val all = listOf(OEJTS, LOVE_LANGUAGES, LOVE_ATTITUDES, ARON, RCIT, IPIP, ATUS, HAREL, CFPB, ALBUM, ONESOURCE, AZURA)

    operator fun get(id: String?): Source? = all.firstOrNull { it.id == id }
}

/**
 * The instruments, in published item order (scoring keys index into it).
 * Never shuffle, subset, or mix these into the daily pool.
 */
object Instruments {

    /**
     * Jorgenson's Open Extended Jungian Type Scales 1.2, verbatim: 32 pole pairs,
     * four axes of eight items scored 8..40, cut at 24.
     *
     * Not the MBTI (a trademark); never call it that.
     * Cosmetic departures from the PDF: "sceptical" -> "skeptical", "where ever" -> "wherever".
     */
    val JUNGIAN = Instrument(
        id = "oejts",
        sourceId = "oejts",
        title = "Personality type",
        // Shown under the wall heading and atop the sitting. Tests require > 60 chars.
        blurb = "Thirty-two this-or-that pairs, like makes lists or relies on memory. " +
            "Your answers add up to a four-letter type.",
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
        // Published equations with 0-based indices. Signs and constants exactly as printed; do not simplify.
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
     * Love languages: 20 behaviors (four per language) rated twice, for [FELT] and [GIVEN].
     *
     * Mostova et al. found the want/give mismatch correlated with satisfaction
     * (r about -.36 and -.40). Impett, Park and Muise (2024) question the five-category
     * model, so the UI shows all five rather than one label.
     * Items are subject-free phrases so they read under both stems.
     */
    val LOVE_LANGUAGES = Instrument(
        id = "lovelang",
        sourceId = "lovelang",
        title = "Love languages",
        blurb = "Twenty small things, rated twice: what makes you feel loved, and " +
            "what you do for them. Then see how well those match up.",
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


    /**
     * Four of Hendrick and Hendrick's six love styles ([Fidelity.ADAPTED]), 28 items.
     *
     * Ludus and Mania are left out entirely: they include infidelity and suicidal-thinking
     * items that are not appropriate here. "My lover" is reworded to "my partner".
     */
    val LOVE_ATTITUDES = Instrument(
        id = "loveattitudes",
        sourceId = "loveattitudes",
        title = "Ways of loving",
        blurb = "Twenty-eight statements about how you love. Four styles: " +
            "love at first sight, friends first, thought through, and giving way.",
        minutes = 5,
        items = listOf(
            // Eros -- passionate, immediate, physical. Original items 1-7.
            like("My partner and I were attracted to each other immediately after we first met."),
            like("My partner and I have the right physical \"chemistry\" between us."),
            like("Our lovemaking is very intense and satisfying."),
            like("I feel that my partner and I were meant for each other."),
            like("My partner and I became emotionally involved rather quickly."),
            like("My partner and I really understand each other."),
            like("My partner fits my ideal standards of physical beauty."),
            // Storge -- friendship that became love. Original items 15-21.
            like("It is hard to say exactly where friendship ends and love begins."),
            like("Genuine love first requires caring for a while."),
            like("I expect to always be friends with the one I love."),
            like("The best kind of love grows out of a long friendship."),
            like("Our friendship merged gradually into love over time."),
            like("Love is really a deep friendship, not a mysterious, mystical emotion."),
            like("My most satisfying love relationships have developed from good friendships."),
            // Pragma -- compatibility, reasoned. Original items 22-28.
            like("I consider what a person is going to become in life before I commit myself."),
            like("I try to plan my life carefully before choosing a partner."),
            like("It is best to love someone with a similar background."),
            like("A main consideration in choosing a partner is how they reflect on my family."),
            like("An important factor in choosing a partner is whether they will be a good parent."),
            like("One consideration in choosing a partner is how they will reflect on my career."),
            like("Before getting very involved with anyone, I try to figure out how compatible we would be as parents."),
            // Agape -- selfless, giving way. Original items 36-42.
            like("I try to always help my partner through difficult times."),
            like("I would rather suffer myself than let my partner suffer."),
            like("I cannot be happy unless I place my partner's happiness before my own."),
            like("I am usually willing to sacrifice my own wishes to let my partner achieve theirs."),
            like("Whatever I own is my partner's to use as they choose."),
            like("When my partner gets angry with me, I still love them fully and unconditionally."),
            like("I would endure all things for the sake of my partner."),
        ),
        // Plain sums, 7..35. The published coding has 1 = strongly agree; ours is reversed
        // (stored = 6 - published) so every scale in the app rises to the right.
        subscales = listOf(
            Subscale("eros", "Straight away", terms = (0..6).associateWith { 1 }),
            Subscale("storge", "Friends first", terms = (7..13).associateWith { 1 }),
            Subscale("pragma", "Thought through", terms = (14..20).associateWith { 1 }),
            Subscale("agape", "Giving way", terms = (21..27).associateWith { 1 }),
        ),
    )

    /** A statement rated for how true it is, rising to the right. */
    private fun like(text: String) = InstrumentItem("Not like me", "Very like me", text)

    /** Shortest first. */
    val all = listOf(JUNGIAN, LOVE_ATTITUDES, LOVE_LANGUAGES)

    operator fun get(id: String?): Instrument? = all.firstOrNull { it.id == id }

    /** Which pass of [instrument] a response index belongs to. */
    const val FELT = 0
    const val GIVEN = 1
}
