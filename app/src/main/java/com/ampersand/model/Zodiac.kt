package com.ampersand.model

import java.time.LocalDate
import java.time.MonthDay

/** Western sun signs, each starting on its first day. Capricorn wraps the new year. */
enum class Zodiac(val label: String, val starts: MonthDay) {
    CAPRICORN("Capricorn", MonthDay.of(12, 22)),
    AQUARIUS("Aquarius", MonthDay.of(1, 20)),
    PISCES("Pisces", MonthDay.of(2, 19)),
    ARIES("Aries", MonthDay.of(3, 21)),
    TAURUS("Taurus", MonthDay.of(4, 20)),
    GEMINI("Gemini", MonthDay.of(5, 21)),
    CANCER("Cancer", MonthDay.of(6, 21)),
    LEO("Leo", MonthDay.of(7, 23)),
    VIRGO("Virgo", MonthDay.of(8, 23)),
    LIBRA("Libra", MonthDay.of(9, 23)),
    SCORPIO("Scorpio", MonthDay.of(10, 23)),
    SAGITTARIUS("Sagittarius", MonthDay.of(11, 22));

    companion object {
        fun of(birthday: LocalDate): Zodiac {
            val md = MonthDay.from(birthday)
            // The last sign starting on or before this day; before 20 January is still Capricorn.
            return entries.drop(1).lastOrNull { it.starts <= md && md < CAPRICORN.starts } ?: CAPRICORN
        }
    }
}

/**
 * Chinese zodiac animals, by year.
 * ponytail: the year turns on 5 February here, not the real Lunar New Year (21 January to
 * 20 February); a birthday in that window can land a year off. Add a date table if it matters.
 */
enum class ChineseZodiac(val label: String) {
    RAT("Rat"), OX("Ox"), TIGER("Tiger"), RABBIT("Rabbit"), DRAGON("Dragon"), SNAKE("Snake"),
    HORSE("Horse"), GOAT("Goat"), MONKEY("Monkey"), ROOSTER("Rooster"), DOG("Dog"), PIG("Pig");

    companion object {
        fun of(birthday: LocalDate): ChineseZodiac {
            val year = if (MonthDay.from(birthday) < MonthDay.of(2, 5)) birthday.year - 1 else birthday.year
            // 2020 was a Rat year.
            return entries[(year - 2020).mod(12)]
        }
    }
}

/** The decade a birthday falls in, e.g. 1989 -> 1980. */
fun decadeOf(birthday: LocalDate): Int = birthday.year / 10 * 10

private const val MySign = "{mysign}"
private const val TheirSign = "{theirsign}"

/** Whether a question names a sign, so it needs both birthdays. */
val Question.needsSigns: Boolean
    get() = listOf(MySign, TheirSign).any { t -> t in text || options.any { t in it } }

/** Fills {mysign} and {theirsign} from the viewer's and partner's birthdays. */
fun Question.forSigns(mine: LocalDate?, theirs: LocalDate?): Question {
    if (!needsSigns || mine == null || theirs == null) return this
    fun fill(s: String) = s.replace(MySign, Zodiac.of(mine).label).replace(TheirSign, Zodiac.of(theirs).label)
    return copy(text = fill(text), options = options.map(::fill))
}
