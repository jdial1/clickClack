package com.ampersand.data

import com.ampersand.model.Together
import com.ampersand.model.ChineseZodiac
import com.ampersand.model.DateKind
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

/** Every bundled question. Deck questions double as the daily question pool. */
class QuestionPool(
    val decks: List<Quiz>,
    /** One small deck per occasion, keyed by [Occasion.id]. Not listed as playable decks. */
    val occasions: Map<String, Quiz> = emptyMap(),
    /** Questions for the daily pool only; also offered as [packs]. */
    val dailyOnly: List<Quiz> = emptyList(),
    /** A few questions for each kind of weather. See [daily]. */
    val weather: Map<WeatherKind, List<Question>> = emptyMap(),
    /** Questions that join the daily pool only for couples living in that region. */
    val regional: Map<UsRegion, List<Question>> = emptyMap(),
    /** Questions naming the local teams, drawn only once a home ZIP is set. */
    val localTeams: List<Question> = emptyList(),
    /** Sign questions, only in the draw when one of you asked for horoscopes and both birthdays are set. */
    val horoscope: List<Question> = emptyList(),
    /** Questions for relationship milestones, by tier (see [Together]). */
    val milestones: Map<Together.Tier, List<Question>> = emptyMap(),
    /** Growing-up questions for each birth decade (1950, 1960...). */
    val eras: Map<Int, List<Question>> = emptyMap(),
    /** A few questions for each Chinese zodiac animal, drawn with horoscopes. */
    val chinese: Map<ChineseZodiac, List<Question>> = emptyMap(),
    /** Old daylight questions, kept only so past days that asked one still resolve. */
    val sun: Map<SunKind, List<Question>> = emptyMap(),
    /** A few questions for each kind of special date, asked on the day. */
    val dates: Map<DateKind, List<Question>> = emptyMap(),
    /** Every ZIP, for turning a home ZIP into a forecast and a region. */
    val places: ZipTable = ZipTable.empty,
    /** Chosen questions for the first seven days you both answer, in order. See [firstWeekDay]. */
    val firstWeek: List<Question> = emptyList(),
) {

    val all: List<Question> = decks.flatMap { it.questions } + dailyOnly.flatMap { it.questions }

    /**
     * [dailyOnly] cut into playable packs of [PackSize], in file order so ids stay stable.
     * A finished pack is skipped by the daily draw for six weeks (see `recentlyAsked`).
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
        (all + firstWeek + occasions.values.flatMap { it.questions } + weather.values.flatten() + regional.values.flatten() + localTeams + horoscope + milestones.values.flatten() + eras.values.flatten() + chinese.values.flatten() + sun.values.flatten() + dates.values.flatten())
            .associateBy { it.id }

    fun question(id: String?): Question? = id?.let { byId[it] }

    fun deck(id: String?): Quiz? = playable.firstOrNull { it.id == id }

    /**
     * Today's question, chosen by date so both phones land on the same one.
     *
     * A few strong signals, first match wins: one of your dates, a holiday,
     * the theme picked last night, notable weather (every other such day).
     * Otherwise a draw leaning toward the topics you asked for more of.
     * Weekdays stay at depth 1-2; Friday to Sunday use the whole pool.
     * After a quiet spell ([gentle]) only depth 1 is drawn, and nothing says why.
     */
    fun daily(
        date: LocalDate,
        anniversary: LocalDate? = null,
        taste: Taste = Taste.none,
        /** Question ids to skip (recently asked or in a recently finished deck). Must be shared state so both phones agree. */
        recent: Set<String> = emptySet(),
        /** The category picked for today last night. Falls back to the whole pool if it is used up. */
        theme: String? = null,
        /** Topics either of you hid. Filtered out. */
        hidden: Set<Topic> = emptySet(),
        /** Notable weather. Used on every other such day. */
        weatherKind: WeatherKind? = null,
        /** Where you live. Adds that region's questions to the pool. */
        region: UsRegion? = null,
        /** The home state, for days that belong to a place: Mardi Gras, Derby Day. */
        state: String? = null,
        /** Leagues with a local team in season. A team question needs all of its leagues here. */
        leagues: Set<League> = emptySet(),
        /** Both birthdays are set and one of you picked horoscopes, so sign questions can be drawn. */
        signs: Boolean = false,
        /** A relationship milestone today; its set outranks everything else. */
        together: Together? = null,
        /** Birth decades of either of you, for growing-up questions. */
        decades: Set<Int> = emptySet(),
        /** Chinese zodiac animals of either of you, when horoscopes are on. */
        animals: Set<ChineseZodiac> = emptySet(),
        /** Kinds of special date that fall today; see [com.ampersand.model.Dates.kindsOn]. */
        dateKinds: List<DateKind> = emptyList(),
        /** Neither of you answered for a few days. Coming back should cost nothing. See [gentleReturn]. */
        gentle: Boolean = false,
        /** How far through the first week you are, or null once it is over. See [firstWeekDay]. */
        firstWeekDay: Int? = null,
    ): Question {
        require(all.isNotEmpty()) { "no questions loaded" }
        fun shown(q: Question) = hidden.none { it.matches(q) }
        val teams = localTeams.filter { q -> q.leagues.isNotEmpty() && leagues.containsAll(q.leagues) }
        val everything = all + region?.let { regional[it].orEmpty() + teams }.orEmpty() + (if (signs) horoscope else emptyList()) +
            decades.flatMap { eras[it].orEmpty() } + animals.flatMap { chinese[it].orEmpty() }
        val open = everything.filter(::shown).ifEmpty { everything }

        together?.let { milestones[it.tier]?.takeIf { q -> q.isNotEmpty() } }?.let { pool ->
            return pool[date.toEpochDay().mod(pool.size.toLong()).toInt()]
        }

        // Your own dates, then holidays. Rotated by year.
        dateKinds.firstNotNullOfOrNull { dates[it]?.takeIf { q -> q.isNotEmpty() } }?.let { pool ->
            return pool[date.year.mod(pool.size)]
        }

        Occasion.on(date, anniversary, state)?.let { occasion ->
            occasions[occasion.id]?.questions?.takeIf { it.isNotEmpty() }?.let { pool ->
                return pool[date.year.mod(pool.size)]
            }
        }

        val weekend = date.dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        fun fits(q: Question) = if (gentle) q.depth.level == 1 else weekend || q.depth.level <= 2

        theme?.let { t ->
            val themed = open.filter { it.category == t }
            val picked = themed.filter(::fits).ifEmpty { themed }
            if (picked.isNotEmpty()) walk(picked, date, weekend, taste, recent)?.let { return it }
        }

        firstWeekDay?.let { n ->
            firstWeek.drop(n).firstOrNull { it.id !in recent && shown(it) }?.let { return it }
        }

        if (weatherKind != null && date.toEpochDay().mod(2L) == 0L) {
            weather[weatherKind]?.filter(::shown)?.let { walk(it, date, weekend, taste, recent) }?.let { return it }
        }

        val candidates = open.filter(::fits).ifEmpty { open }
        return walk(candidates, date, weekend, taste, recent)
            ?: slot(tiltedOrNot(candidates, taste), date, weekend).let { (draw, start) -> draw[start] }
    }

    /**
     * The short tag for why [question] is today's, based on which set it came from.
     * Theme picks, wildcards and repeats are labeled by the screen instead.
     */
    fun why(
        question: Question,
        date: LocalDate,
        anniversary: LocalDate?,
        state: String?,
        dateKinds: List<DateKind>,
        interests: Set<Topic>,
        together: Together? = null,
    ): String {
        fun inSet(list: List<Question>?) = list.orEmpty().any { it.id == question.id }
        together?.takeIf { inSet(milestones[it.tier]) }?.let { return it.title }
        dateKinds.firstOrNull { inSet(dates[it]) }?.let { return it.label }
        Occasion.on(date, anniversary, state)?.takeIf { inSet(occasions[it.id]?.questions) }?.let { return it.label }
        if (inSet(firstWeek)) return "First week"
        if (weather.values.any(::inSet)) return "Weather"
        if (regional.values.any(::inSet) || inSet(localTeams)) return "Local"
        if (inSet(horoscope)) return "Horoscope"
        if (eras.values.any(::inSet)) return "Growing up"
        if (chinese.values.any(::inSet)) return "Chinese zodiac"
        interests.firstOrNull { it.matches(question) }?.let { return it.label.substringBefore(" and ") }
        return question.category
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
     * A day index counting only weekend days (Fri-Sun) or only weekdays (Mon-Thu).
     * Calendar day numbers left gaps that stopped the stride from reaching the whole list.
     */
    private fun sameKindDay(date: LocalDate, weekend: Boolean): Long {
        val week = Math.floorDiv(date.with(DayOfWeek.MONDAY).toEpochDay(), 7L)
        val dow = date.dayOfWeek.value // Monday 1 .. Sunday 7
        return if (weekend) week * 3 + (dow - DayOfWeek.FRIDAY.value)
        else week * 4 + (dow - DayOfWeek.MONDAY.value)
    }

    /**
     * How far the daily slot moves each day: the smallest prime that does not divide
     * [size], so the walk reaches every question.
     */
    private fun strideFor(size: Int): Long =
        Strides.first { size % it != 0 }.toLong()

    /**
     * The pool weighted by [taste], laid out in tiers: tier 1 is every candidate, tier 2
     * those weighted 2+, and so on. Tiers keep a question's copies far apart in the walk.
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
            horoscope = QuizJson.loadFolder(context, "horoscope").flatMap { it.questions },
            milestones = QuizJson.loadFolder(context, "milestones").mapNotNull { deck ->
                Together.Tier.entries.firstOrNull { it.id == deck.id }?.let { it to deck.questions }
            }.toMap(),
            eras = QuizJson.loadFolder(context, "eras").mapNotNull { deck ->
                deck.id.removePrefix("era-").toIntOrNull()?.let { it to deck.questions }
            }.toMap(),
            chinese = QuizJson.loadFolder(context, "chinese").mapNotNull { deck ->
                ChineseZodiac.entries.firstOrNull { "chinese-${it.name.lowercase()}" == deck.id }?.let { it to deck.questions }
            }.toMap(),
            dates = QuizJson.loadFolder(context, "dates").mapNotNull { deck ->
                DateKind.entries.firstOrNull { "date-${it.id}" == deck.id }?.let { it to deck.questions }
            }.toMap(),
            sun = QuizJson.loadFolder(context, "sun").mapNotNull { deck ->
                SunKind.entries.firstOrNull { "sun-${it.name.lowercase().replace('_', '-')}" == deck.id }
                    ?.let { it to deck.questions }
            }.toMap(),
            // ponytail: parsed on first launch of the screen, about 34k lines;
            // index lazily or by prefix if it ever shows up in a startup trace.
            firstWeek = QuizJson.loadFolder(context, "firstweek").flatMap { it.questions },
            places = context.assets.open("zips.csv").bufferedReader().useLines { ZipTable.parse(it) },
        )

        /** Primes to try as a stride, smallest first. */
        private val Strides = listOf(7, 11, 13, 17, 19, 23)

        /** Questions per pack. */
        const val PackSize = 8

        /** How far back "recently asked" reaches: six weeks. */
        const val RecentDays = 42L
    }
}

/**
 * The Bad-Month Test as a rule: after [QuietDays] days that neither of you
 * answered, the next question is a light one. Only for couples who have
 * answered before, and computed from shared history so both phones agree.
 */
/**
 * Which of the first week's questions is next: the count of days you have both opened,
 * or null after seven, or once a sculpture has sealed (history is a window, so a couple
 * back from a long gap must not start over). Shared state only, so both phones agree.
 */
fun firstWeekDay(history: List<com.ampersand.model.DayEntry>, sealedSculpture: Boolean): Int? {
    if (sealedSculpture) return null
    return history.count { it.isRevealed }.takeIf { it < 7 }
}

fun gentleReturn(today: java.time.LocalDate, history: List<com.ampersand.model.DayEntry>): Boolean {
    if (history.none { it.submitted.isNotEmpty() || it.isRevealed }) return false
    return (1L..QuietDays).all { back ->
        history.none { it.date == today.minusDays(back) && (it.submitted.isNotEmpty() || it.isRevealed) }
    }
}

const val QuietDays = 3L
