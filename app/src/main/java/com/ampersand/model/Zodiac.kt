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
