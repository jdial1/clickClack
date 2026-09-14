package com.ampersand.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.R
import com.ampersand.data.Instruments
import com.ampersand.data.MockCouple
import com.ampersand.data.MockQuestions
import com.ampersand.model.Answer
import com.ampersand.model.BrickColor
import com.ampersand.model.ChatMessage
import com.ampersand.model.Chats
import com.ampersand.model.CoupleState
import com.ampersand.model.DateKind
import com.ampersand.model.DayWeather
import com.ampersand.model.InstrumentRun
import com.ampersand.model.Mood
import com.ampersand.model.MoodEntry
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import com.ampersand.model.Reaction
import com.ampersand.model.SpecialDate
import com.ampersand.ui.brick.glass
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.chat.ChatScreen
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.history.HistoryScreen
import com.ampersand.ui.quiz.DecksScreen
import com.ampersand.ui.reveal.RevealScreen
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.today.TodayScreen
import com.ampersand.ui.us.ProfileBadges
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Google Play listing images, rendered from mock data into store/graphics.
 * Only runs when asked: gradlew testDebugUnitTest --tests com.ampersand.ui.StoreAssets -Pshots
 *
 * Phone screenshots are 1236x2196 (9:16 at xxhdpi), the feature graphic 1024x500,
 * the icon 512x512.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h732dp-xxhdpi")
class StoreAssets {

    @Before fun onlyWhenAsked() = assumeTrue(System.getProperty("shots") != null)

    private val out = "../store/graphics"
    private val today = LocalDate.of(2026, 9, 13)
    private val base = MockCouple.state(today)
    private val you = base.you
    private val them = base.them

    private val state: CoupleState = base.copy(
        you = you.copy(mood = MoodEntry(Mood.SNAPPED_IN, today)),
        them = them.copy(mood = MoodEntry(Mood.HANDLE_WITH_CARE, today)),
        weather = DayWeather(78, "Sunny", "Kansas City"),
        specialDates = listOf(SpecialDate("d1", "First date", LocalDate.of(2019, 9, 13), DateKind.FIRST_DATE)),
        capsule = null,
        written = mapOf(you.uid to 3, them.uid to 5),
        instrumentRuns = listOf(Instruments.JUNGIAN, Instruments.LOVE_ATTITUDES, Instruments.ATTACHMENT).map { i ->
            val n = i.items.size * i.passes.size
            InstrumentRun(i.id, mapOf(you.uid to List(n) { if (it % 3 == 0) 5 else 2 }, them.uid to List(n) { if (it % 2 == 0) 4 else 1 }))
        },
    )

    private val decks = listOf(
        Quiz("k1", "The asking part", "", QuizTheme.KNOW_ME, MockQuestions.pool.take(4)),
        Quiz("e1", "Sunday plans", "", QuizTheme.EVERYDAY, MockQuestions.pool.drop(4).take(4)),
        Quiz("p1", "Silly stuff", "", QuizTheme.PLAYFUL, MockQuestions.pool.drop(8).take(4)),
        Quiz("t1", "Getting there and back", "", QuizTheme.TRAVEL, MockQuestions.pool.take(3)),
        Quiz("d1", "After midnight", "", QuizTheme.DEEP, MockQuestions.pool.drop(2).take(3)),
        Quiz("f1", "Second helpings", "", QuizTheme.FOOD, MockQuestions.pool.drop(5).take(3)),
    )

    @Composable
    private fun Wall(content: @Composable () -> Unit) {
        AmpersandTheme(darkTheme = false) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize()) {
                    BrickWall(you = BrickColor.of(you.colorArgb), them = BrickColor.of(them.colorArgb))
                    content()
                }
            }
        }
    }

    /** A headline on glass over the wall, with the screen itself below it, shrunk into a phone frame. */
    private fun phone(name: String, headline: String, screen: @Composable () -> Unit) {
        captureRoboImage("$out/phone/$name.png") {
            Wall {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        headline,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().glass(inkOnBrick(structureSlug()), corner = 16.dp, strength = 1.4f)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    // The screen lays out at full phone size, then shrinks to fit under the headline.
                    Box(
                        Modifier.size(412.dp * 0.76f, 732.dp * 0.76f)
                            .clip(RoundedCornerShape(22.dp))
                            .border(4.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.requiredSize(412.dp, 732.dp).graphicsLayer(scaleX = 0.76f, scaleY = 0.76f)) {
                            Wall(screen)
                        }
                    }
                }
            }
        }
    }

    @Test fun phone1_today() = phone("1-today", "One question a day, just for you two") {
        TodayScreen(state, { _, _ -> }, {})
    }

    @Test fun phone2_sealed() = phone("2-sealed", "Sealed until you both answer") {
        val answer = Answer(you.uid, listOf("When you made coffee before I woke up"))
        TodayScreen(state.copy(today = state.today.copy(answers = mapOf(you.uid to answer))), { _, _ -> }, {})
    }

    @Test fun phone3_build() = phone("3-build", "Every answer earns bricks. Build together") {
        com.ampersand.ui.build.BuildScreen(state, state.plate, onPlace = { _, _, _ -> })
    }

    @Test fun phone4_reveal() = phone("4-reveal", "Then open them together") {
        RevealScreen(state, state.history.first { it.isRevealed }, onBack = {}, animateIn = false)
    }

    @Test fun phone5_shelf() = phone("5-shelf", "A sculpture for every week, kept forever") {
        // Just the shelf at the top: no photos, kept days or anniversaries above it.
        // Every week named, with a line from each of you, as a shelf looks a few weeks in.
        val words = listOf(
            com.ampersand.model.NameWords("Wobbly", "Castle") to com.ampersand.model.NameWords("Cosmic", "Pretzel"),
            com.ampersand.model.NameWords("Brave", "Couch") to com.ampersand.model.NameWords("Spicy", "Dragon"),
            com.ampersand.model.NameWords("Lucky", "Garden") to com.ampersand.model.NameWords("Chaotic", "Trombone"),
        )
        val shelf = state.shelf.mapIndexed { i, p ->
            val (a, b) = words[i % words.size]
            p.copy(
                words = mapOf(you.uid to a, them.uid to b),
                notes = mapOf(you.uid to "The week we finally fixed the porch light", them.uid to "Pancakes on a Tuesday. Worth it."),
            )
        }
        HistoryScreen(state.copy(shelf = shelf, kept = emptyMap(), photos = emptyList(), history = state.history.filter { it.date > today.minusDays(20) }), onOpenDay = {})
    }

    @Test fun phone6_chat() = phone("6-chat", "Keep talking about it") {
        val day = state.history.first { it.isRevealed }
        val id = Chats.day(day.date)
        ChatScreen(
            state = state,
            title = Chats.title(id, them.name),
            subtitle = day.question.text,
            messages = listOf(
                ChatMessage("1", them.uid, "I had no idea you remembered that"),
                ChatMessage("2", you.uid, "Of course I did. Best night of that whole summer"),
                ChatMessage("3", them.uid, emote = Reaction.MELT),
                ChatMessage("4", them.uid, "Same place Friday?"),
                ChatMessage("5", you.uid, "It's a date"),
            ),
            onBack = {},
            onSend = { _, _ -> },
        )
    }

    @Test fun phone7_profiles() = phone("7-profiles", "See how you two fit together") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
            ProfileBadges(state, onTake = {})
        }
    }

    @Test fun phone8_decks() = phone("8-decks", "Decks for every kind of night") {
        DecksScreen(state, decks, onOpenDeck = {})
    }

    /** The two-brick mark on its own ground, rounded as a launcher would show it. */
    @Composable
    private fun Mark(size: androidx.compose.ui.unit.Dp) {
        Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.22f))) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(size * 1.5f))
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * 1.5f))
        }
    }

    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    @Test fun featureGraphic() = captureRoboImage("$out/feature-graphic.png") {
        Wall {
            // Words on the left, a finished week's sculpture on its base on the right.
            Row(Modifier.fillMaxSize().padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).glass(inkOnBrick(structureSlug()), corner = 24.dp, strength = 1.4f).padding(horizontal = 32.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Mark(64.dp)
                        Spacer(Modifier.width(16.dp))
                        Text("Click Clack", fontSize = 58.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Text("A daily question for two.", fontSize = 30.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("Every answer earns bricks. Build together.", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.width(40.dp))
                Column(Modifier.width(400.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    com.ampersand.ui.brick.Sculpture(state.shelf.first(), Modifier.fillMaxWidth())
                    com.ampersand.ui.brick.BrickSlab(
                        slugs = listOf(null),
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        studs = 12,
                        height = com.ampersand.model.BrickHeight.LOW,
                    )
                }
            }
        }
    }

    /** What the launch splash looks like: the mark on the night ground. */
    @Config(qualifiers = "w360dp-h640dp-xxxhdpi")
    @Test fun splash() = captureRoboImage("$out/splash.png") {
        Box(Modifier.fillMaxSize().background(Color(0xFF0E1226)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(160.dp).clip(RoundedCornerShape(80.dp))) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(240.dp))
            }
        }
    }

    /** Play's hi-res icon: 512x512, full square; Play applies its own mask. */
    @Test fun icon512() {
        val res = RuntimeEnvironment.getApplication().resources
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Adaptive layers are 108dp with a 72dp visible middle, so draw them half again as large.
        listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach {
            res.getDrawable(it, null).apply { setBounds(-128, -128, 640, 640) }.draw(canvas)
        }
        File("$out/icon-512.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
