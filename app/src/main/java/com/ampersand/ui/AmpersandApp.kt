package com.ampersand.ui

import com.ampersand.model.Kept
import com.ampersand.ui.today.PastAnswerScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.ampersand.ui.quiz.BrickChip
import com.ampersand.ui.us.InterestsSetupScreen
import com.ampersand.ui.quiz.DecksScreen
import com.ampersand.ui.us.SettingsScreen
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ampersand.data.CoupleRepository
import com.ampersand.data.FirestoreCoupleRepository
import com.ampersand.data.createCoupleRepository
import com.ampersand.data.Instruments
import com.ampersand.data.QuestionPool
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import com.ampersand.auth.AuthRepository
import com.ampersand.auth.AuthState
import com.ampersand.auth.PairState
import com.ampersand.auth.PairingRepository
import com.ampersand.auth.createAuthRepository
import com.ampersand.auth.createPairingRepository
import com.ampersand.auth.hasFirebaseConfigFor
import com.ampersand.auth.findActivity
import com.ampersand.model.CoupleState
import com.ampersand.model.QuizTheme
import com.ampersand.ui.auth.PairingScreen
import com.ampersand.ui.auth.SignInScreen
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.build.BuildScreen
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.components.pressDepth
import com.ampersand.ui.history.HistoryScreen
import com.ampersand.notify.Push
import com.ampersand.notify.Reminder
import com.ampersand.ui.notify.ReminderSetupScreen
import com.ampersand.ui.quiz.DeckScreen
import com.ampersand.ui.reveal.RevealScreen
import com.ampersand.ui.splash.SplashScreen
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.feel.rememberFeelKit
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.sound.rememberSoundKit
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.LocalAppearance
import com.ampersand.ui.theme.rememberAppearance
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.today.TodayScreen
import com.ampersand.ui.us.UsScreen
import com.ampersand.ui.instrument.InstrumentScreen
import com.ampersand.ui.wildcard.WildcardScreen
import java.time.LocalDate

/**
 * Holds the couple repository for one pairing. Built per couple because the
 * Firestore listeners are bound to a couple id and both uids.
 */
class AmpersandViewModel(val repo: CoupleRepository) : ViewModel() {
    override fun onCleared() {
        (repo as? FirestoreCoupleRepository)?.close()
    }

    companion object {
        fun factory(
            hasFirebase: Boolean,
            coupleId: String,
            myUid: String,
            partnerUid: String,
            pool: QuestionPool,
        ) = viewModelFactory {
            initializer {
                AmpersandViewModel(
                    createCoupleRepository(hasFirebase, coupleId, myUid, partnerUid, pool),
                )
            }
        }
    }
}

/** Holds the auth repository across configuration changes. Needs a Context, unlike [AmpersandViewModel]. */
class AuthViewModel(val auth: AuthRepository) : ViewModel() {
    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { AuthViewModel(createAuthRepository(context)) }
        }
    }
}

/** Pairing for the signed-in account. Built after sign-in because it watches `users/{uid}`. */
class PairingViewModel(val pairing: PairingRepository) : ViewModel() {
    companion object {
        fun factory(context: Context, uid: String, name: String) = viewModelFactory {
            initializer { PairingViewModel(createPairingRepository(context, uid, name)) }
        }
    }
}

private enum class Tab(val label: String) {
    TODAY("Today"),
    DECKS("Decks"),
    // History screen; "Shelf" fits a 2-stud tile without shrinking the type.
    SHELF("Shelf"),
    US("Us"),
}

/**
 * Today's date, checked again every time the app comes back to the front.
 *
 * ponytail: yesterday's ViewModel stays in the store until the Activity
 * finishes, listeners and all; one extra per day the app is left open. Clear
 * the store on a date change if that ever shows up in battery or read counts.
 */
@Composable
private fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose { }
    }
    return today
}

@Composable
fun AmpersandApp() {
    val context = LocalContext.current
    val authVm: AuthViewModel = viewModel(factory = AuthViewModel.factory(context))
    val auth by authVm.auth.state.collectAsStateWithLifecycle()

    val sounds = rememberSoundKit()
    val feel = rememberFeelKit()
    val appearance = rememberAppearance(context)

    AmpersandTheme(darkTheme = appearance.isDark()) {
      CompositionLocalProvider(
          LocalSounds provides sounds,
          LocalFeel provides feel,
          LocalAppearance provides appearance,
      ) {
        // Splash waits for its own animation, not auth. Saveable so rotation doesn't replay it.
        var splashDone by rememberSaveable { mutableStateOf(false) }
        if (!splashDone) {
            // Fixed colors: no profile exists before sign-in.
            SplashScreen(
                you = BrickColor.RED,
                them = BrickColor.BLUE,
                onDone = { splashDone = true },
            )
            return@CompositionLocalProvider
        }

        when (auth) {
            // Blank, not a spinner: the cached session resolves in a frame or two.
            AuthState.Loading -> Surface(color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize())
            }

            AuthState.SignedOut -> SignedOutScreen(authVm.auth)

            is AuthState.SignedIn -> {
                val account = auth as AuthState.SignedIn
                PairedGate(
                    account = account,
                    onSignOut = {
                        Reminder.cancel(context)
                        Push.unregister(context, account.uid) { authVm.auth.signOut() }
                    },
                )
            }
        }
      }
    }
}

/** Sign-in, and the one place the app talks to Credential Manager. */
@Composable
private fun SignedOutScreen(auth: AuthRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(color = MaterialTheme.colorScheme.background) {
        SignInScreen(
            busy = busy,
            error = error,
            onSignIn = {
                val activity = context.findActivity()
                if (activity == null) {
                    error = "Couldn't open Google sign-in."
                    return@SignInScreen
                }
                busy = true
                error = null
                scope.launch {
                    val result = auth.signInWithGoogle(activity)
                    busy = false
                    // A dismissed sheet returns success with no session; only show real failures.
                    error = result.exceptionOrNull()?.let {
                        it.message ?: "Sign-in failed."
                    }
                }
            },
        )
    }
}

/**
 * Signed in; shows pairing until paired. Keyed on uid so switching accounts
 * doesn't leave the old account's listener attached.
 */
@Composable
private fun PairedGate(
    account: AuthState.SignedIn,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val hasFirebase = remember(context) { hasFirebaseConfigFor(context) }
    // Loaded once; shared by decks and the daily question.
    val pool = remember(context) { QuestionPool.load(context) }
    val pairVm: PairingViewModel = viewModel(
        key = "pairing_${account.uid}",
        factory = PairingViewModel.factory(context, account.uid, account.displayName),
    )
    val pair by pairVm.pairing.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    when (val p = pair) {
        PairState.Loading -> Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize())
        }

        is PairState.Paired -> {
            // Reminder permission is asked once, after pairing.
            var askedReminder by remember { mutableStateOf(Reminder.hasBeenAsked(context)) }
            if (!askedReminder) {
                ReminderSetupScreen(
                    color = BrickColor.RED,
                    onDone = { askedReminder = true },
                )
                return@PairedGate
            }

            // Keyed on couple and day: the repository fixes its date at build time,
            // so without the day key an overnight process wrote to yesterday.
            val today = rememberToday()
            val vm: AmpersandViewModel = viewModel(
                key = "couple_${p.coupleId}_$today",
                factory = AmpersandViewModel.factory(
                    hasFirebase = hasFirebase,
                    coupleId = p.coupleId,
                    myUid = account.uid,
                    partnerUid = p.partnerUid,
                    pool = pool,
                ),
            )
            val state by vm.repo.state.collectAsStateWithLifecycle()
            AmpersandTheme(
                you = Color(state.you.colorArgb),
                them = Color(state.them.colorArgb),
                // Pass dark mode again, or the inner theme falls back to the system setting.
                darkTheme = LocalAppearance.current?.isDark() ?: isSystemInDarkTheme(),
            ) {
                SignedInApp(
                    vm = vm,
                    state = state,
                    account = account,
                    pool = pool,
                    onSignOut = onSignOut,
                    onUnpair = {
                        Reminder.cancel(context)
                        scope.launch { pairVm.pairing.unpair() }
                    },
                )
            }
        }

        else -> Surface(color = MaterialTheme.colorScheme.background) {
            PairingScreen(
                name = account.displayName,
                code = (p as? PairState.Inviting)?.code,
                busy = busy,
                error = error,
                onCreateInvite = {
                    busy = true; error = null
                    scope.launch {
                        val r = pairVm.pairing.createInvite()
                        busy = false
                        error = r.exceptionOrNull()?.message
                    }
                },
                onRedeem = { code ->
                    busy = true; error = null
                    scope.launch {
                        val r = pairVm.pairing.redeem(code)
                        busy = false
                        error = r.exceptionOrNull()?.message
                    }
                },
            )
        }
    }
}

/**
 * The current screen as one value, so AnimatedContent has a stable target
 * (comparing `CoupleState` would restart it on every Firestore update).
 */
private sealed interface Screen {
    data class Deck(val id: String) : Screen
    data class Sitting(val instrumentId: String) : Screen
    data class Reveal(val date: LocalDate) : Screen
    data class Past(val date: LocalDate) : Screen
    data object Wildcard : Screen
    data object Settings : Screen
    data class Main(val tab: Tab) : Screen
}

/** Main app for a signed-in, paired user. */
@Composable
private fun SignedInApp(
    vm: AmpersandViewModel,
    state: CoupleState,
    account: AuthState.SignedIn,
    pool: QuestionPool,
    onSignOut: () -> Unit,
    onUnpair: () -> Unit,
) {
        // Bind the local user once per account.
        val appContext = LocalContext.current
        LaunchedEffect(account.uid, account.displayName) {
            vm.repo.bindLocalUser(account.uid, account.displayName)
            // Registered on every launch: tokens rotate and stale ones fail silently.
            Push.register(appContext, account.uid)
        }

        // Interests setup, asked once. null means the profile is still loading.
        if (state.interestsAsked == false) {
            InterestsSetupScreen(color = BrickColor.of(state.you.colorArgb), onDone = { following, leaveOut ->
                leaveOut.forEach { vm.repo.setTopicHidden(it, true) }
                vm.repo.setFollowing(following)
            })
            return
        }

        // Cache what's outstanding for the reminder, which runs without Firestore access.
        val context = LocalContext.current
        val waiting = state.decksAwaitingYou(pool.playable.map { it.id })
        val unanswered = state.today.answerFor(account.uid) == null
        LaunchedEffect(waiting, unanswered, state.them.name) {
            Reminder.cacheOutstanding(
                context = context,
                questionPending = unanswered,
                decksWaiting = waiting,
                partnerName = state.them.name,
            )
        }

        // Saveable so rotation keeps the open screen.
        var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
        // A date, not the entry, so live updates (reactions) still show.
        var openDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
        // An id, not the Quiz, so the screen re-resolves the latest deck.
        var openDeck by rememberSaveable { mutableStateOf<String?>(null) }
        // Day whose reveal animation already played. Hoisted so it doesn't replay on return.
        var openedReveal by rememberSaveable { mutableStateOf<String?>(null) }
        var writing by rememberSaveable { mutableStateOf(false) }
        // A missed past day being answered late.
        var answering by rememberSaveable { mutableStateOf<LocalDate?>(null) }
        // Past days load their answers when opened.
        LaunchedEffect(openDate) { openDate?.let { vm.repo.loadAnswers(it) } }
        var sitting by rememberSaveable { mutableStateOf<String?>(null) }
        var settings by rememberSaveable { mutableStateOf(false) }

        val sounds = LocalSounds.current
        val feel = LocalFeel.current

        // No nav back stack, so handle back manually: close overlays, then return to Today.
        // Disabled on Today so back exits the app.
        BackHandler(
            enabled = settings || writing || openDeck != null || openDate != null || answering != null || tab != Tab.TODAY,
        ) {
            sounds.play(Sfx.NAV)
            feel.play(Feel.NAV)
            when {
                settings -> settings = false
                writing -> writing = false
                answering != null -> answering = null
                openDeck != null -> openDeck = null
                openDate != null -> openDate = null
                else -> tab = Tab.TODAY
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    BottomBar(
                        current = tab,
                        color = BrickColor.of(state.you.colorArgb),
                        enabled = openDate == null && openDeck == null && !writing && answering == null &&
                            sitting == null && !settings,
                        onSelect = { tab = it; openDate = null; openDeck = null },
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize()) {
                    // Background wall, outside the inset padding so it runs under the nav bar.
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
                    val here: Screen = when {
                        sitting != null -> Screen.Sitting(sitting!!)
                        settings -> Screen.Settings
                        writing -> Screen.Wildcard
                        answering != null -> Screen.Past(answering!!)
                        openDeck != null -> Screen.Deck(openDeck!!)
                        openDate != null -> Screen.Reveal(openDate!!)
                        else -> Screen.Main(tab)
                    }

                    // Fade out, then fade in (screens are transparent, so a crossfade looks doubled).
                    // SizeTransform off: BrickSlab measures its own height.
                    AnimatedContent(
                        targetState = here,
                        transitionSpec = {
                            fadeIn(tween(Motion.SwapMs - Motion.PressMs, Motion.PressMs))
                                .togetherWith(fadeOut(tween(Motion.PressMs))) using null
                        },
                        label = "screen",
                    ) { screen ->
                    // Resolve from `screen`, not the live vars, so the outgoing screen keeps its content.
                    val day = (screen as? Screen.Reveal)?.date?.let { date ->
                        if (state.today.date == date) {
                            state.today
                        } else {
                            state.history.firstOrNull { it.date == date } ?: state.recalled.firstOrNull { it.date == date }
                        }
                    }

                    val deck = (screen as? Screen.Deck)?.id?.let { pool.deck(it) }
                    val past = (screen as? Screen.Past)?.date?.let { date -> state.history.firstOrNull { it.date == date } }

                    val kit = (screen as? Screen.Sitting)?.instrumentId?.let { Instruments[it] }

                    when {
                        kit != null -> InstrumentScreen(
                            instrument = kit,
                            you = BrickColor.of(state.you.colorArgb),
                            onBack = { sitting = null },
                            onSeal = { responses ->
                                vm.repo.submitInstrument(kit.id, responses)
                                sitting = null
                            },
                            resume = state.instrumentRuns
                                .lastOrNull { it.instrumentId == kit.id }
                                ?.draft.orEmpty(),
                            onSaveDraft = { vm.repo.saveInstrumentDraft(kit.id, it) },
                        )

                        past != null -> PastAnswerScreen(
                            state = state,
                            day = past,
                            onBack = { answering = null },
                            onSubmit = { values, guess ->
                                vm.repo.answerPast(past.date, past.question.id, values, guess)
                                answering = null
                            },
                        )

                        deck != null -> DeckScreen(
                            state = state,
                            quiz = deck,
                            onBack = { openDeck = null },
                            onSubmit = { answers, guesses ->
                                vm.repo.submitQuizRun(deck.id, answers, guesses)
                            },
                            onKeep = vm.repo::keep,
                        )

                        day != null -> RevealScreen(
                            state = state,
                            day = day,
                            onBack = { openDate = null },
                            onReact = { vm.repo.react(day.date, it) },
                            animateIn = openedReveal != day.date.toString(),
                            onOpened = { openedReveal = day.date.toString() },
                            onTalkedAbout = vm.repo::markTalkedAbout,
                            onNote = { vm.repo.noteDay(day.date, it) },
                            onKeep = vm.repo::keep,
                            themes = QuizTheme.entries.filter { it.label in pool.themes },
                            onPickTomorrow = vm.repo::pickTomorrow,
                            onWrite = { writing = true },
                        )

                        screen is Screen.Wildcard -> WildcardScreen(
                            state = state,
                            onBack = { writing = false },
                            onSubmit = vm.repo::addWildcard,
                        )

                        screen is Screen.Settings -> SettingsScreen(
                            state = state,
                            onPickColor = vm.repo::setColor,
                            onBack = { settings = false },
                            account = account.email ?: account.displayName,
                            onSignOut = onSignOut,
                            onUnpair = onUnpair,
                            onSetAnniversary = vm.repo::setAnniversary,
                            onSetProfile = vm.repo::setProfile,
                            onSetRelationship = vm.repo::setRelationship,
                            onSetTopicHidden = vm.repo::setTopicHidden,
                            onSetTopicInterest = vm.repo::setTopicInterest,
                            onSetFollowing = vm.repo::setFollowing,
                            onSetHomeZip = vm.repo::setHomeZip,
                            onSetSource = vm.repo::setSource,
                            onSetQuietHours = vm.repo::setQuietHours,
                            onRemoveSpecialDate = vm.repo::removeSpecialDate,
                            onRetake = { settings = false; sitting = it },
                        )

                        screen !is Screen.Main -> Unit

                        screen.tab == Tab.TODAY -> TodayScreen(
                            state = state,
                            onSubmit = vm.repo::submitToday,
                            onOpenReveal = { openDate = state.today.date },
                            onSetMood = vm.repo::setMood,
                            onReplyToMood = vm.repo::replyToMood,
                            onOpenDay = { openDate = it },
                            onAnswerDay = { answering = it },
                        )

                        screen.tab == Tab.DECKS -> DecksScreen(
                            state = state,
                            decks = pool.playable,
                            onOpenDeck = { openDeck = it },
                            themes = QuizTheme.entries.filter { it.label in pool.themes },
                            onPickTomorrow = vm.repo::pickTomorrow,
                            onTakeInstrument = { sitting = it },
                            onWrite = { writing = true },
                        )

                        // This week's build and past weeks. A toggle rather than one
                        // scrolling list: dragging bricks inside a scroll fights the scroll.
                        screen.tab == Tab.SHELF -> Column(Modifier.fillMaxSize()) {
                            var past by rememberSaveable { mutableStateOf(false) }
                            Row(
                                Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                BrickChip(label = "This week", on = !past, color = BrickColor.of(state.you.colorArgb), onClick = { past = false })
                                BrickChip(label = "Past weeks", on = past, color = BrickColor.of(state.you.colorArgb), onClick = { past = true })
                            }
                            if (past) {
                                HistoryScreen(
                                    state = state,
                                    // Only revealed days open.
                                    onOpenDay = { if (it.isRevealed) openDate = it.date },
                                    onNoteWeek = vm.repo::noteWeek,
                                    onNameWeek = vm.repo::nameWeek,
                                    onOpenKept = { key ->
                                        Kept.dateOf(key)?.let { openDate = it }
                                        Kept.deckOf(key)?.let { openDeck = it }
                                    },
                                )
                            } else {
                                BuildScreen(
                                    state = state,
                                    plate = state.plate,
                                    onPlace = vm.repo::placeBlock,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        else -> UsScreen(
                            state = state,
                            onSetAnniversary = vm.repo::setAnniversary,
                            onTakeInstrument = { sitting = it },
                            onOpenSettings = { settings = true },
                            onAddToBucket = vm.repo::addToBucket,
                            onTickBucket = vm.repo::tickBucket,
                            onRemoveFromBucket = vm.repo::removeFromBucket,
                            onAddSpecialDate = vm.repo::addSpecialDate,
                            onOpenDay = { if (it.isRevealed) openDate = it.date },
                        )
                    }
                    }

                }
            }
        }
}

/**
 * Bottom nav drawn as bricks: 2-stud tabs in the user's color joined by 1-stud
 * structure bricks, sized from the width. Labels sit below the nubs.
 */
@Composable
private fun BottomBar(
    current: Tab,
    color: BrickColor,
    enabled: Boolean,
    onSelect: (Tab) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val tabShape = BrickShape(2, BrickHeight.MEDIUM)
    val joinShape = BrickShape(1, BrickHeight.MEDIUM)
    // Joiner color depends on the theme (black on dark, neutral on light).
    val joinAsset = BrickArt.assetFor(structureSlug(), joinShape)
    val atlas = rememberBricks(listOf(color.asset(tabShape), joinAsset))

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        // Two studs a tab and one between each: 14 for five tabs.
        val stud = maxWidth / (Tab.entries.size * 3 - 1).toFloat()
        val barHeight = stud * (tabShape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)
        // Label offset so it centers on the body, below the nub.
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
                // No bouncy spring: this feeds an alpha, and overshoot past 1.0 breaks drawImage.
                val lift by animateFloatAsState(if (on) 1f else 0f, Motion.glide, label = "tabLift")
                val fg by animateColorAsState(
                    if (on) Duo.inkOn(Color(color.argb)) else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabFg",
                )

                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()

                Box(
                    Modifier
                        .width(stud * 2)
                        .height(barHeight)
                        .pressDepth(pressed)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            enabled = enabled,
                        ) { sounds.play(Sfx.NAV); feel.play(Feel.NAV); onSelect(entry) },
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
                            // Unselected tabs are faded.
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

