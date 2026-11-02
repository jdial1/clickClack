package com.ampersand.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ampersand.data.MockCouple
import com.ampersand.data.MockQuestions
import com.ampersand.model.BrickColor
import com.ampersand.model.CoupleState
import com.ampersand.model.DateKind
import com.ampersand.model.DayWeather
import com.ampersand.model.Kept
import com.ampersand.model.Mood
import com.ampersand.model.MoodEntry
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import com.ampersand.model.SpecialDate
import com.ampersand.model.Wildcard
import com.ampersand.model.QuestionType
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.history.HistoryScreen
import com.ampersand.ui.quiz.DecksScreen
import com.ampersand.ui.reveal.RevealScreen
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.today.TodayScreen
import com.ampersand.ui.us.SettingsScreen
import com.ampersand.ui.us.UsScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders the main screens to PNGs under app/build/screenshots, from mock
 * data, without a device. Only runs with -Pshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h1900dp-xxhdpi")
class Screenshots {

    @Before fun onlyWhenAsked() = assumeTrue(System.getProperty("shots") != null)

    private val today = LocalDate.of(2026, 9, 13)
    private val base = MockCouple.state(today)
    private val you = base.you
    private val them = base.them

    /** A day with the new things on it: moods, weather, a special date, a written question, moments. */
    private val special: CoupleState = run {
        val written = Wildcard("w1", "What's one small thing I did this week that you noticed?", QuestionType.REFLECT, byUid = them.uid, byName = them.name)
        val noted = base.history[1].copy(notes = mapOf(them.uid to "That floor picnic is still my favorite night."))
        val missed = base.history[3].copy(date = today.minusDays(2), answers = emptyMap(), opened = false, submitted = setOf(them.uid))
        base.copy(
            you = you.copy(mood = MoodEntry(Mood.SNAPPED_IN, today)),
            them = them.copy(mood = MoodEntry(Mood.HANDLE_WITH_CARE, today)),
            today = base.today.copy(question = written.asQuestion(), moodReplies = mapOf(them.uid to "Thinking of you")),
            weather = DayWeather(78, "Sunny", "Kansas City"),
            specialDates = listOf(SpecialDate("d1", "First date", LocalDate.of(2019, 9, 13), DateKind.FIRST_DATE)),
            todayAuthorUid = them.uid,
            capsule = null,
            history = listOf(missed) + base.history.map { if (it.date == noted.date) noted else it },
            kept = mapOf(Kept.day(base.history[1].date) to base.history[1].question.text),
            written = mapOf(you.uid to 3, them.uid to 5),
        )
    }

    private val decks = listOf(
        Quiz("k1", "The asking part", "", QuizTheme.KNOW_ME, MockQuestions.pool.take(4)),
        Quiz("e1", "Sunday plans", "", QuizTheme.EVERYDAY, MockQuestions.pool.drop(4).take(4)),
        Quiz("p1", "Silly stuff", "", QuizTheme.PLAYFUL, MockQuestions.pool.drop(8).take(4)),
        Quiz("t1", "Getting there and back", "", QuizTheme.TRAVEL, MockQuestions.pool.take(3)),
        Quiz("d1", "After midnight", "", QuizTheme.DEEP, MockQuestions.pool.drop(2).take(3)),
        Quiz("f1", "Second helpings", "", QuizTheme.FOOD, MockQuestions.pool.drop(5).take(3)),
    )

    private fun shoot(name: String, content: @Composable () -> Unit) {
        captureRoboImage("build/screenshots/$name.png") {
            AmpersandTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        BrickWall(you = BrickColor.of(you.colorArgb), them = BrickColor.of(them.colorArgb))
                        content()
                    }
                }
            }
        }
    }

    @Test fun today_special_day() = shoot("01-today-special-day") { TodayScreen(special, { _, _ -> }, {}) }

    @Test fun today_answered() = shoot("02-today-answered") {
        val answer = com.ampersand.model.Answer(you.uid, listOf("When you made coffee before I woke up"))
        TodayScreen(special.copy(today = special.today.copy(answers = mapOf(you.uid to answer))), { _, _ -> }, {})
    }

    @Test fun reveal_line_back() = shoot("03-reveal-line-back-keep") {
        RevealScreen(special, special.history.first { it.notes.isNotEmpty() }, onBack = {}, animateIn = false)
    }

    @Test fun decks() = shoot("04-decks") { DecksScreen(special, decks, onOpenDeck = {}) }

    @Test fun shelf_past_weeks() = shoot("05-shelf-past-weeks-kept") { HistoryScreen(special, onOpenDay = {}) }

    @Test fun us() = shoot("06-us") { UsScreen(special) }

    @Test fun us_new_couple() = shoot("06b-us-two-days-in") {
        UsScreen(special.copy(history = special.history.filter { it.date >= today.minusDays(2) }, quizRuns = emptyList()))
    }

    @Test fun pairing() = shoot("08-pairing-code") {
        com.ampersand.ui.auth.PairingScreen("Justin", "WOBBLYCASTLE427", false, null, {}, {})
    }

    @Test fun sign_in() = shoot("00-sign-in") { com.ampersand.ui.auth.SignInScreen(busy = false, error = null, onSignIn = {}) }

    @Test fun settings() = shoot("07-settings") { SettingsScreen(special, onPickColor = {}, onBack = {}, onSignOut = {}) }

    /** Bricks scale with the screen and text does not, so buttons get tested narrow too. */
    @Config(qualifiers = "w320dp-h1900dp-xxhdpi")
    @Test fun settings_narrow() = shoot("13-settings-narrow-phone") {
        SettingsScreen(special, onPickColor = {}, onBack = {}, onSignOut = {})
    }

    @Config(qualifiers = "w320dp-h1900dp-xxhdpi")
    @Test fun today_narrow() = shoot("14-today-narrow-phone") { TodayScreen(special, { _, _ -> }, {}) }

    /** Today once you have both answered: the first thing a new couple ever opens. */
    private val bothIn: CoupleState = base.copy(
        today = base.today.copy(
            question = com.ampersand.model.Question(
                "fw5",
                "What is something small I do that you would miss by Wednesday?",
                QuestionType.REFLECT,
                "Playful",
                com.ampersand.model.Depth(1),
            ),
            answers = mapOf(
                you.uid to com.ampersand.model.Answer(you.uid, listOf("The coffee you leave on the step")),
                them.uid to com.ampersand.model.Answer(them.uid, listOf("You humming when you think I am asleep")),
            ),
            opened = true,
        ),
        history = emptyList(),
        shelf = emptyList(),
        capsule = null,
    )

    @Test fun today_open_it() = shoot("09-today-open-it") { TodayScreen(bothIn, { _, _ -> }, {}) }

    @Test fun first_reveal() = shoot("10-first-reveal") {
        RevealScreen(bothIn, bothIn.today, onBack = {}, animateIn = false)
    }

    @Test fun shelf_first_week() = shoot("11-shelf-first-week-building") { HistoryScreen(bothIn, onOpenDay = {}) }

    @Test fun shelf_nothing_placed() = shoot("12-shelf-nothing-placed") {
        HistoryScreen(bothIn.copy(plate = bothIn.plate.copy(placed = emptyList())), onOpenDay = {})
    }
}
