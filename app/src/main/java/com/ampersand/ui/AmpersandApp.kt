package com.ampersand.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ampersand.data.FirestoreCoupleRepository
import com.ampersand.data.createCoupleRepository
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
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.history.HistoryScreen
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
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.glow
import com.ampersand.ui.today.TodayScreen
import com.ampersand.ui.us.UsScreen
import java.time.LocalDate

/**
 * Holds the couple repository for one pairing.
 *
 * Built per couple rather than once, because the Firestore implementation
 * attaches its listeners to a specific couple id and a specific pair of uids.
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

/**
 * Holds the auth repository across configuration changes.
 *
 * Separate from [AmpersandViewModel] because it needs a Context to build and
 * that one deliberately does not take any -- and because signing out has to
 * survive without tearing down the couple state with it.
 */
class AuthViewModel(val auth: AuthRepository) : ViewModel() {
    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { AuthViewModel(createAuthRepository(context)) }
        }
    }
}

/**
 * Pairing, keyed to the signed-in account.
 *
 * Separate from [AuthViewModel] because it cannot be built until there *is* an
 * account: the Firestore implementation watches `users/{uid}`, and there is no
 * uid to watch before sign-in.
 */
class PairingViewModel(val pairing: PairingRepository) : ViewModel() {
    companion object {
        fun factory(context: Context, uid: String, name: String) = viewModelFactory {
            initializer { PairingViewModel(createPairingRepository(context, uid, name)) }
        }
    }
}

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
fun AmpersandApp() {
    val context = LocalContext.current
    val authVm: AuthViewModel = viewModel(factory = AuthViewModel.factory(context))
    val auth by authVm.auth.state.collectAsStateWithLifecycle()

    val sounds = rememberSoundKit()
    val feel = rememberFeelKit()

    AmpersandTheme {
      CompositionLocalProvider(LocalSounds provides sounds, LocalFeel provides feel) {
        // The splash is gated on its own animation finishing, not on auth, so
        // the click-clack always plays in full. It is under a second, and
        // cutting it short the moment a cached session resolves would mean the
        // animation is only ever seen by people who are signed out.
        // Saveable, not remembered. The Activity is recreated on rotation, and
        // a plain remember replayed the whole drop -- and now the two landing
        // thumps with it -- every time the phone turned.
        var splashDone by rememberSaveable { mutableStateOf(false) }
        if (!splashDone) {
            // Fixed colours: this plays before anyone is signed in, so there is
            // no profile to take them from yet.
            SplashScreen(
                you = BrickColor.RED,
                them = BrickColor.BLUE,
                onDone = { splashDone = true },
            )
            return@CompositionLocalProvider
        }

        when (auth) {
            // Nothing on screen rather than a spinner. This resolves from the
            // cached session in a frame or two, and a spinner that fast is just
            // a flicker.
            AuthState.Loading -> Surface(color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize())
            }

            AuthState.SignedOut -> SignedOutScreen(authVm.auth)

            is AuthState.SignedIn -> {
                val account = auth as AuthState.SignedIn
                PairedGate(
                    account = account,
                    onSignOut = authVm.auth::signOut,
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
                    error = "Could not open the Google sheet."
                    return@SignInScreen
                }
                busy = true
                error = null
                scope.launch {
                    val result = auth.signInWithGoogle(activity)
                    busy = false
                    // A dismissed sheet comes back as success with no session,
                    // so only a real failure is worth saying anything about.
                    error = result.exceptionOrNull()?.let {
                        it.message ?: "Sign-in failed."
                    }
                }
            },
        )
    }
}

/**
 * Signed in, but the product needs two people.
 *
 * Keyed on the uid so switching accounts builds a new pairing repository rather
 * than leaving the previous account's listener attached.
 */
@Composable
private fun PairedGate(
    account: AuthState.SignedIn,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val hasFirebase = remember(context) { hasFirebaseConfigFor(context) }
    // Loaded once. The decks are both the thing you play and the pool the daily
    // question is drawn from, so there is exactly one list.
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
            // Asked once, and only after pairing: permission to interrupt
            // somebody lands better when there is already a reason to.
            var askedReminder by remember { mutableStateOf(Reminder.hasBeenAsked(context)) }
            if (!askedReminder) {
                ReminderSetupScreen(
                    color = BrickColor.RED,
                    onDone = { askedReminder = true },
                )
                return@PairedGate
            }

            // Keyed on the couple: a new pairing gets a new repository and a
            // new set of listeners rather than the previous couple's.
            val vm: AmpersandViewModel = viewModel(
                key = "couple_${p.coupleId}",
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
            ) {
                SignedInApp(
                    vm = vm,
                    state = state,
                    account = account,
                    pool = pool,
                    onSignOut = onSignOut,
                    onUnpair = { scope.launch { pairVm.pairing.unpair() } },
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
 * Where you are, as one value.
 *
 * The screen used to be chosen by reading three pieces of state inside a
 * `when`, which is fine for drawing and useless for animating: a transition
 * needs one thing to compare, and comparing `CoupleState` would restart it
 * every time Firestore pushed anything at all.
 */
private sealed interface Screen {
    data class Deck(val id: String) : Screen
    data class Reveal(val date: LocalDate) : Screen
    data class Main(val tab: Tab) : Screen
}

/** The whole product, once we know who is using it. */
@Composable
private fun SignedInApp(
    vm: AmpersandViewModel,
    state: CoupleState,
    account: AuthState.SignedIn,
    pool: QuestionPool,
    onSignOut: () -> Unit,
    onUnpair: () -> Unit,
) {
        // Point both halves of the couple at the real accounts. Keyed on the
        // uids so this runs once per pairing rather than every recomposition.
        LaunchedEffect(account.uid, account.displayName) {
            vm.repo.bindLocalUser(account.uid, account.displayName)
        }

        // Leave the receiver a note about what is actually outstanding. It
        // fires with no app process and cannot go and ask Firestore, so the
        // alternative is a reminder that says the same thing whether or not
        // there is anything to do.
        val context = LocalContext.current
        val waiting = state.decksAwaitingYou(pool.decks.map { it.id })
        val unanswered = state.today.answerFor(account.uid) == null
        LaunchedEffect(waiting, unanswered, state.them.name) {
            Reminder.cacheOutstanding(
                context = context,
                questionPending = unanswered,
                decksWaiting = waiting,
                partnerName = state.them.name,
            )
        }

        var tab by remember { mutableStateOf(Tab.TODAY) }
        // The *date* of the open day, not the day itself. Holding the entry
        // would freeze it at the moment it was opened, so a reaction added on
        // the reveal screen would never appear on the reveal screen.
        var openDate by remember { mutableStateOf<LocalDate?>(null) }
        // The deck you are playing, if any. A quiz id rather than the Quiz, so
        // the screen re-resolves it and a deck that gains a question mid-play
        // does not leave a stale copy on screen.
        var openDeck by remember { mutableStateOf<String?>(null) }
        // Which day has already been unpacked. Hoisted above the screen because
        // the screen is disposed every time you leave it, and a flag inside it
        // would let the reveal put on its whole performance again on the way
        // back in. Saveable so a rotation does not do the same.
        var openedReveal by rememberSaveable { mutableStateOf<String?>(null) }

        val sounds = LocalSounds.current
        val feel = LocalFeel.current

        // Back walks back through the app rather than out of it.
        //
        // Everything here is one composable swapping its contents, so there was
        // no back stack for the system to pop: a press from four screens deep
        // closed the app. Out of a deck or a reveal first, then off whichever
        // tab you are on, and only from Today does the press fall through and
        // leave -- which is where leaving should mean leaving.
        BackHandler(enabled = openDeck != null || openDate != null || tab != Tab.TODAY) {
            // Going back is a navigation, and only the forward direction ever
            // said so. Note the handler is disabled on Today, so leaving the
            // app stays silent -- which is right: the app has no opinion about
            // you closing it.
            sounds.play(Sfx.NAV)
            feel.play(Feel.NAV)
            when {
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
                        enabled = openDate == null && openDeck == null,
                        onSelect = { tab = it; openDate = null; openDeck = null },
                    )
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
                    val here: Screen = when {
                        openDeck != null -> Screen.Deck(openDeck!!)
                        openDate != null -> Screen.Reveal(openDate!!)
                        else -> Screen.Main(tab)
                    }

                    // One surface leaving, then the next arriving -- never both
                    // at once. Every screen here is a transparent column on the
                    // same brick wall, so a true crossfade draws two sets of
                    // cards at half opacity over one background and reads as a
                    // double exposure rather than as a transition.
                    //
                    // SizeTransform off: BrickSlab reports its own height, and a
                    // container animating between two measured heights around
                    // one is a fight nobody wins.
                    AnimatedContent(
                        targetState = here,
                        transitionSpec = {
                            fadeIn(tween(Motion.SwapMs - Motion.PressMs, Motion.PressMs))
                                .togetherWith(fadeOut(tween(Motion.PressMs))) using null
                        },
                        label = "screen",
                    ) { screen ->
                    // Resolved from the *target* screen, not from the live vars:
                    // reading those would make the outgoing screen draw the
                    // incoming one's content for the length of the transition.
                    val day = (screen as? Screen.Reveal)?.date?.let { date ->
                        if (state.today.date == date) {
                            state.today
                        } else {
                            state.history.firstOrNull { it.date == date }
                        }
                    }

                    val deck = (screen as? Screen.Deck)?.id?.let { pool.deck(it) }

                    when {
                        deck != null -> DeckScreen(
                            state = state,
                            quiz = deck,
                            onBack = { openDeck = null },
                            onSubmit = { answers, guesses ->
                                vm.repo.submitQuizRun(deck.id, answers, guesses)
                            },
                        )

                        day != null -> RevealScreen(
                            state = state,
                            day = day,
                            onBack = { openDate = null },
                            onReact = { vm.repo.react(day.date, it) },
                            animateIn = openedReveal != day.date.toString(),
                            onOpened = { openedReveal = day.date.toString() },
                        )

                        screen !is Screen.Main -> Unit

                        screen.tab == Tab.TODAY -> TodayScreen(
                            state = state,
                            decks = pool.decks,
                            onSubmit = vm.repo::submitToday,
                            onOpenReveal = { openDate = state.today.date },
                            onOpenDeck = { openDeck = it },
                        )

                        screen.tab == Tab.BUILD -> BuildScreen(
                            state = state,
                            plate = state.plate,
                            onPlace = vm.repo::placeBlock,
                        )

                        screen.tab == Tab.SHELF -> HistoryScreen(
                            state = state,
                            onOpenDay = { openDate = it.date },
                        )

                        else -> UsScreen(
                            state = state,
                            onPickColor = vm.repo::setColor,
                            onSetAnniversary = vm.repo::setAnniversary,
                            account = account.email ?: account.displayName,
                            onSignOut = onSignOut,
                            onUnpair = onUnpair,
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
    val feel = LocalFeel.current
    val tabShape = BrickShape(2, BrickHeight.MEDIUM)
    val joinShape = BrickShape(1, BrickHeight.MEDIUM)
    // Structure, not a choice -- and which brick counts as structure depends on
    // the ground. Black joins the bar on navy and punches holes in it on a pale
    // one, where the neutral brick does the same job.
    val joinAsset = BrickArt.assetFor(structureSlug(), joinShape)
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
                // Not a bouncy spring: this value feeds an alpha a few lines down, and an
                // overshoot past 1.0 goes straight into drawImage.
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
                            // Dimmed inside a deck or a reveal, and now actually
                            // unreachable there: the visual said disabled while
                            // the tap still went through.
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

