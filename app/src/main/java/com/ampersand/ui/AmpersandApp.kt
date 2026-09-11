package com.ampersand.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ampersand.data.CoupleRepository
import com.ampersand.data.FakeCoupleRepository
import com.ampersand.data.MockQuestions
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.build.BuildScreen
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.brickFace
import com.ampersand.ui.gallery.GalleryScreen
import com.ampersand.ui.history.HistoryScreen
import com.ampersand.ui.reveal.RevealScreen
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.sound.rememberSoundKit
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.glow
import com.ampersand.ui.today.TodayScreen
import com.ampersand.ui.us.UsScreen
import java.time.LocalDate

/** Swapped for a Firestore-backed repository later; nothing above it changes. */
class AmpersandViewModel(
    val repo: CoupleRepository = FakeCoupleRepository(),
) : ViewModel()

/** The pack has no "black" in [BrickColor]; it is structure, not a choice. */
private const val JoinerSlug = "black"

private enum class Tab(val label: String) {
    TODAY("Today"),
    BUILD("Build"),
    // "Shelf", not "History": it is where the finished sculptures live, and a
    // log is not what that screen is for. It is also the only label that fits a
    // 2-stud tile without shrinking the type.
    SHELF("Shelf"),
    US("Us"),
}

@Composable
fun AmpersandApp(vm: AmpersandViewModel = viewModel()) {
    val state by vm.repo.state.collectAsStateWithLifecycle()

    val sounds = rememberSoundKit()

    AmpersandTheme(
        you = Color(state.you.colorArgb),
        them = Color(state.them.colorArgb),
    ) {
      CompositionLocalProvider(LocalSounds provides sounds) {
        var tab by remember { mutableStateOf(Tab.TODAY) }
        // The *date* of the open day, not the day itself. Holding the entry
        // would freeze it at the moment it was opened, so a reaction added on
        // the reveal screen would never appear on the reveal screen.
        var openDate by remember { mutableStateOf<LocalDate?>(null) }
        var showScaffoldControls by remember { mutableStateOf(false) }
        // Scaffold only, and off the tab bar: five pills do not fit a phone.
        var showKit by remember { mutableStateOf(false) }

        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column {
                        AnimatedVisibility(visible = showScaffoldControls) {
                            ScaffoldControls(
                                repo = vm.repo,
                                today = state.today.date,
                                kitOpen = showKit,
                                onToggleKit = { showKit = !showKit },
                            )
                        }
                        BottomBar(
                            current = tab,
                            color = BrickColor.of(state.you.colorArgb),
                            enabled = openDate == null && !showKit,
                            onSelect = { tab = it; openDate = null; showKit = false },
                        )
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize()) {
                    // The wall everything sits on. Outside the Scaffold's inset
                    // padding so it runs under the nav bar rather than stopping
                    // short of it.
                    BrickWall(
                        you = BrickColor.of(state.you.colorArgb),
                        them = BrickColor.of(state.them.colorArgb),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Re-resolved every composition, so the screen sees the live
                    // day rather than the one that was open when it was tapped.
                    val day = openDate?.let { date ->
                        if (state.today.date == date) {
                            state.today
                        } else {
                            state.history.firstOrNull { it.date == date }
                        }
                    }

                    when {
                        showKit -> GalleryScreen(state = state)

                        day != null -> RevealScreen(
                            state = state,
                            day = day,
                            onBack = { openDate = null },
                            onReact = { vm.repo.react(day.date, it) },
                        )

                        tab == Tab.TODAY -> TodayScreen(
                            state = state,
                            onSubmit = vm.repo::submitToday,
                            onOpenReveal = { openDate = state.today.date },
                        )

                        tab == Tab.BUILD -> BuildScreen(
                            state = state,
                            plate = state.plate,
                            onPlace = vm.repo::placeBlock,
                        )

                        tab == Tab.SHELF -> HistoryScreen(
                            state = state,
                            onOpenDay = { openDate = it.date },
                        )

                        else -> UsScreen(
                            state = state,
                            onPickColor = vm.repo::setColor,
                        )
                    }

                    // Tap target for the scaffold panel. Deliberately unlabelled
                    // and tiny; it comes out with the mock repository.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clickable { showScaffoldControls = !showScaffoldControls },
                    )
                }
            }
        }
      }
    }
}

/**
 * The nav bar is one continuous run of brick: four 2-stud tabs in the user's
 * colour, joined by 1-stud black bricks.
 *
 * Eleven studs wide (4 x 2 + 3 x 1), laid out on stud weights rather than fixed
 * sizes so the run stays true at any screen width. The black joiners are what
 * turn four separate tiles into a single bar -- without them the tabs float,
 * and the bar stops reading as something built.
 *
 * Labels sit *on* the bricks, inset below the nubs so the text never collides
 * with them.
 */
@Composable
private fun BottomBar(
    current: Tab,
    color: BrickColor,
    enabled: Boolean,
    onSelect: (Tab) -> Unit,
) {
    val sounds = LocalSounds.current
    val tabShape = BrickShape(2, BrickHeight.MEDIUM)
    val joinShape = BrickShape(1, BrickHeight.MEDIUM)
    val joinAsset = BrickArt.assetFor(JoinerSlug, joinShape)
    val atlas = rememberBricks(listOf(color.asset(tabShape), joinAsset))

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        val stud = maxWidth / 11f
        val barHeight = stud * (tabShape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)
        // Body starts below the nub; centring in the whole tile would sit the
        // label half on the studs.
        val nub = stud * BrickArt.NUB_RATIO

        Row(Modifier.fillMaxWidth().height(barHeight)) {
            Tab.entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    Canvas(Modifier.width(stud).height(barHeight)) {
                        drawBrickArt(
                            atlas = atlas,
                            asset = joinAsset,
                            studs = joinShape.w,
                            rows = joinShape.h,
                            left = 0f,
                            bottom = size.height,
                            studPx = size.width,
                        )
                    }
                }

                val on = enabled && entry == current
                val lift by animateFloatAsState(if (on) 1f else 0f, label = "tabLift")
                val fg by animateColorAsState(
                    if (on) Duo.inkOn(Color(color.argb)) else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabFg",
                )

                Box(
                    Modifier
                        .width(stud * 2)
                        .height(barHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { sounds.play(Sfx.NAV); onSelect(entry) },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawBrickArt(
                            atlas = atlas,
                            asset = color.asset(tabShape),
                            studs = tabShape.w,
                            rows = tabShape.h,
                            left = 0f,
                            bottom = size.height,
                            studPx = size.width / tabShape.w,
                            // Unselected tabs are the same brick, further away.
                            alpha = 0.34f + 0.66f * lift,
                        )
                    }
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.2.sp,
                        ),
                        color = fg,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = nub),
                    )
                }
            }
        }
    }
}

/**
 * Scaffolding only. Drives the states that normally need a second person and a
 * second device: the partner answering, and each question type in turn.
 */
@Composable
private fun ScaffoldControls(
    repo: CoupleRepository,
    today: LocalDate,
    kitOpen: Boolean,
    onToggleKit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
    ) {
        SectionLabel("Scaffold controls")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Partner answers") { repo.simulatePartnerAnswer() }
            Chip("Partner reacts") { repo.simulatePartnerReaction(today) }
            Chip("Reset day") { repo.resetToday() }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Seal the week") { repo.sealPlate() }
            Chip(if (kitOpen) "Close kit" else "Form kit", onClick = onToggleKit)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Reflect") { repo.previewQuestion(MockQuestions.byId("r2")) }
            Chip("Guess Me") { repo.previewQuestion(MockQuestions.byId("g1")) }
            Chip("This or That") { repo.previewQuestion(MockQuestions.byId("t3")) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Rank") { repo.previewQuestion(MockQuestions.byId("k1")) }
            Chip("Multi") { repo.previewQuestion(MockQuestions.byId("m1")) }
            Chip("Scale") { repo.previewQuestion(MockQuestions.byId("s1")) }
            Chip("Dropdown") { repo.previewQuestion(MockQuestions.byId("d1")) }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
