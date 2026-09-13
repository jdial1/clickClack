package com.ampersand.data

import com.ampersand.model.leagues
import com.ampersand.model.League
import com.ampersand.model.SunKind
import com.ampersand.model.ZipTable
import com.ampersand.model.UsRegion
import com.ampersand.model.WeatherKind
import com.ampersand.model.Topic
import android.content.Context
import com.ampersand.model.Occasion
import com.ampersand.model.Question
import com.ampersand.model.Quiz
import com.ampersand.model.Taste
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Every question the app knows about, loaded from the bundled decks.
 *
 * The decks are both things at once: a deck is something you sit down and play,
 * and its questions are also the pool the daily question is drawn from. Keeping
 * one source means a question cannot exist in the app and be missing from the
 * pool, which is what happens the moment there are two lists.
 */
class QuestionPool(
    val decks: List<Quiz>,
    /**
     * One small deck per occasion, keyed by [Occasion.id].
     *
     * Kept out of [decks] on purpose: these are the source for a particular
     * day, not something to sit down and play. A Christmas deck listed on the
     * eleventh of March is clutter.
     */
    val occasions: Map<String, Quiz> = emptyMap(),
    /**
     * Questions that only ever arrive as the daily question.
     *
     * The decks and the daily pool used to be one list, so an evening spent on
     * a deck was an evening spent using up daily questions -- and five months in
     * the pool had been round. These widen the daily pool and are never offered
     * as a deck, so nothing can spend them but the day.
     */
    val dailyOnly: List<Quiz> = emptyList(),
    /** A few questions for each kind of weather. See [daily]. */
    val weather: Map<WeatherKind, List<Question>> = emptyMap(),
    /** Questions that join the daily pool only for couples living in that region. */
    val regional: Map<UsRegion, List<Question>> = emptyMap(),
    /** Questions naming the local teams, drawn only once a home ZIP is set. */
    val localTeams: List<Question> = emptyList(),
    /** A few questions for each kind of day the light makes. See [daily]. */
    val sun: Map<SunKind, List<Question>> = emptyMap(),
    /** Every ZIP, for turning a home ZIP into a forecast and a region. */
    val places: ZipTable = ZipTable.empty,
) {

    val all: List<Question> = decks.flatMap { it.questions } + dailyOnly.flatMap { it.questions }

    /**
     * The daily-only questions cut into packs of [PackSize], so they can be
     * played in an evening too.
     *
     * They were only ever reachable one a day, which left the Decks tab showing
     * 99 questions out of six hundred. A pack that the two of you both finish
     * is skipped by the daily draw for six weeks (see `recentlyAsked`), so
     * playing one spends its questions exactly the way a deck always did.
     *
     * Cut in file order, so a pack's id and contents stay put between builds
     * as long as the question files do.
     */
    val packs: List<Quiz> = dailyOnly.flatMap { source ->
        val chunks = source.questions.chunked(PackSize)
        chunks.mapIndexed { i, questions ->
            source.copy(
                id = "${source.id}-${i + 1}",
                title = if (chunks.size == 1) source.title else "${source.title} ${i + 1}",
                blurb = source.blurb.takeUnless { it == "Daily only." }.orEmpty(),
                questions = questions,
            )
        }
    }

    /** Everything that can be sat down and played: the decks, then the packs. */
    val playable: List<Quiz> = decks + packs

    private val byId: Map<String, Question> =
        (all + occasions.values.flatMap { it.questions } + weather.values.flatten() + regional.values.flatten() + localTeams + sun.values.flatten())
            .associateBy { it.id }

    fun question(id: String?): Question? = id?.let { byId[it] }

    fun deck(id: String?): Quiz? = playable.firstOrNull { it.id == id }

    /**
     * Today's question, chosen by date.
     *
     * Deterministic, and that is the whole requirement: both phones have to land
     * on the same question before either has written anything. Anything drawn at
     * random, or stored by whoever opens the app first, gives the second person
     * a different question.
     *
     * Weekdays draw from the shallow end and Friday to Sunday from the whole
     * pool. Depth exists so the app can keep a tired Tuesday light and put the
     * ones worth a real evening where there is an evening to give them -- and a
     * pool that ignores it will eventually ask what you are most afraid of at
     * 7am on a Monday.
     */
    fun daily(
        date: LocalDate,
        anniversary: LocalDate? = null,
        taste: Taste = Taste.none,
        /**
         * Question ids to pass over: asked in the last few weeks, or played in a
         * deck the two of you finished recently. Built from state both phones
         * share, so skipping keeps the two landing on the same question.
         */
        recent: Set<String> = emptySet(),
        /**
         * The category one of you picked for today last night, if anybody did.
         *
         * Narrows the draw to that theme. Depth still holds on a weekday where
         * the theme has anything shallow enough; if everything in the theme was
         * asked lately, the pick gives way to the whole pool rather than
         * repeating a question to honour it.
         */
        theme: String? = null,
        /**
         * A day worth more than a Tuesday -- see [com.ampersand.model.Milestones].
         * Draws from the deepest questions whatever the weekday, falling back
         * to the ordinary rules when none are left.
         */
        deeper: Boolean = false,
        /** Topics either of you took out. A filter, because you asked for one. */
        hidden: Set<Topic> = emptySet(),
        /**
         * The kind of day the saved forecast says it is. On every other day of
         * that weather -- by date, so both phones agree -- the question comes
         * from its small set, so a wet week is not every day about rain.
         */
        weatherKind: WeatherKind? = null,
        /** Where you live. Adds that region's questions to the pool. */
        region: UsRegion? = null,
        /** The home state, for days that belong to a place: Mardi Gras, Derby Day. */
        state: String? = null,
        /** The kind of day the light makes at home, on the days weather questions are not. */
        sunKind: SunKind? = null,
        /** Leagues with a local team in season. A team question needs all of its leagues here. */
        leagues: Set<League> = emptySet(),
    ): Question {
        require(all.isNotEmpty()) { "no questions loaded" }
        fun shown(q: Question) = hidden.none { it.matches(q) }
        val teams = localTeams.filter { q -> q.leagues.isNotEmpty() && leagues.containsAll(q.leagues) }
        val everything = all + region?.let { regional[it].orEmpty() + teams }.orEmpty()
        val open = everything.filter(::shown).ifEmpty { everything }

        // A day that is not like the others gets its own question. Rotated by
        // year, so the same holiday does not ask the same thing every time.
        Occasion.on(date, anniversary, state)?.let { occasion ->
            occasions[occasion.id]?.questions?.takeIf { it.isNotEmpty() }?.let { pool ->
                return pool[date.year.mod(pool.size)]
            }
        }

        val weekend = date.dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        fun fits(q: Question) = weekend || q.depth.level <= 2
        fun deep(q: Question) = q.depth.level >= 3

        theme?.let { t ->
            val themed = open.filter { it.category == t }
            if (deeper) walk(themed.filter(::deep), date, weekend, taste, recent)?.let { return it }
            val picked = themed.filter(::fits).ifEmpty { themed }
            if (picked.isNotEmpty()) walk(picked, date, weekend, taste, recent)?.let { return it }
        }

        if (deeper) walk(open.filter(::deep), date, weekend, taste, recent)?.let { return it }

        if (weatherKind != null && date.toEpochDay().mod(2L) == 0L) {
            weather[weatherKind]?.filter(::shown)?.let { walk(it, date, weekend, taste, recent) }?.let { return it }
        }

        // The other half of the days: the light, where the day is one worth
        // mentioning. Never both on one day, so a snowy solstice stays readable.
        if (sunKind != null && date.toEpochDay().mod(2L) == 1L) {
            sun[sunKind]?.filter(::shown)?.let { walk(it, date, weekend, taste, recent) }?.let { return it }
        }

        val candidates = open.filter(::fits).ifEmpty { open }
        return walk(candidates, date, weekend, taste, recent)
            ?: slot(tiltedOrNot(candidates, taste), date, weekend).let { (draw, start) -> draw[start] }
    }

    /** Every category a daily question can come from, in the order the pool holds them. */
    val themes: List<String> get() = all.map { it.category }.distinct()

    /**
     * Walk on from the day's own slot to the first question not asked lately.
     * Every candidate is tried once; null when all of them were recent.
     */
    private fun walk(
        candidates: List<Question>,
        date: LocalDate,
        weekend: Boolean,
        taste: Taste,
        recent: Set<String>,
    ): Question? {
        if (candidates.isEmpty()) return null
        val (draw, start) = slot(tiltedOrNot(candidates, taste), date, weekend)
        return draw.indices.asSequence()
            .map { draw[(start + it).mod(draw.size)] }
            .firstOrNull { it.id !in recent }
    }

    private fun tiltedOrNot(candidates: List<Question>, taste: Taste) =
        if (taste.isEmpty) candidates else tilted(candidates, taste)

    private fun slot(draw: List<Question>, date: LocalDate, weekend: Boolean): Pair<List<Question>, Int> =
        draw to (sameKindDay(date, weekend) * strideFor(draw.size)).mod(draw.size.toLong()).toInt()

    /**
     * A day number that counts only days of the same kind.
     *
     * Weekends and weekdays draw from different lists, and the calendar day
     * number repeats its day of the week every seven. Stepping a weekend list by
     * calendar days meant every weekend slot was the same distance apart each
     * week, so a weekend list whose length was a multiple of seven only ever
     * reached three sevenths of itself. Counting Fridays to Sundays as 0, 1, 2,
     * 3, 4... and Mondays to Thursdays likewise makes consecutive days of a kind
     * consecutive numbers, and the stride then reaches everything.
     */
    private fun sameKindDay(date: LocalDate, weekend: Boolean): Long {
        val week = Math.floorDiv(date.with(DayOfWeek.MONDAY).toEpochDay(), 7L)
        val dow = date.dayOfWeek.value // Monday 1 .. Sunday 7
        return if (weekend) week * 3 + (dow - DayOfWeek.FRIDAY.value)
        else week * 4 + (dow - DayOfWeek.MONDAY.value)
    }

    /**
     * How far the daily slot moves each day.
     *
     * A prime stride walks the list rather than stepping through it in order,
     * so consecutive days are not neighbours in the same deck. It has to share
     * no factor with the list's length, or it only ever reaches a fraction of
     * it: a stride of seven over a list of 140 visits twenty questions and never
     * the other hundred and twenty. A fixed seven was safe while the pool was 76
     * or 99; the weighting changes the length day to day, so the stride is chosen
     * to fit.
     */
    private fun strideFor(size: Int): Long =
        Strides.first { size % it != 0 }.toLong()

    /**
     * The pool, with what the tests found leaning on it.
     *
     * Laid out in tiers rather than by repeating each question in place: tier
     * one is every candidate, tier two is every candidate the profile weights
     * at two or more, and so on. Grouping a question's copies together would
     * put them within a few strides of each other and the same question would
     * come round twice in a fortnight; a tier is a whole pool apart.
     *
     * Every question survives tier one whatever the profile says. The tilt
     * changes the odds and never the reachable set -- an app that quietly
     * retires a third of its questions because of a test somebody took in March
     * is a worse app than one that never asked.
     */
    private fun tilted(candidates: List<Question>, taste: Taste): List<Question> {
        val top = candidates.maxOf { taste.weightOf(it) }
        return (1..top).flatMap { tier -> candidates.filter { taste.weightOf(it) >= tier } }
    }

    companion object {
        fun load(context: Context): QuestionPool = QuestionPool(
            decks = QuizJson.loadFolder(context, "quizzes"),
            occasions = QuizJson.loadFolder(context, "occasions").associateBy { it.id },
            dailyOnly = QuizJson.loadFolder(context, "daily"),
            weather = QuizJson.loadFolder(context, "weather").mapNotNull { deck ->
                WeatherKind.entries.firstOrNull { "weather-${it.name.lowercase()}" == deck.id }?.let { it to deck.questions }
            }.toMap(),
            regional = QuizJson.loadFolder(context, "regions").mapNotNull { deck ->
                UsRegion.entries.firstOrNull { "region-${it.name.lowercase()}" == deck.id }?.let { it to deck.questions }
            }.toMap(),
            localTeams = QuizJson.loadFolder(context, "local").flatMap { it.questions },
            sun = QuizJson.loadFolder(context, "sun").mapNotNull { deck ->
                SunKind.entries.firstOrNull { "sun-${it.name.lowercase().replace('_', '-')}" == deck.id }
                    ?.let { it to deck.questions }
            }.toMap(),
            // ponytail: parsed on first launch of the screen, about 34k lines;
            // index lazily or by prefix if it ever shows up in a startup trace.
            places = context.assets.open("zips.csv").bufferedReader().useLines { ZipTable.parse(it) },
        )

        /** Primes to try as a stride, smallest first. One always fits: no list is that long. */
        private val Strides = listOf(7, 11, 13, 17, 19, 23)

        /** Questions to a pack: an evening's worth, like the hand-written decks. */
        const val PackSize = 8

        /** How far back "asked lately" reaches: six weeks. */
        const val RecentDays = 42L
    }
}
