package com.ampersand.ui.theme

import androidx.compose.ui.unit.Dp

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.ampersand.R

/** Both partners' colors and their [blend], which drives the theme. */
@Immutable
data class DuoColors(
    val you: Color,
    val them: Color,
) {
    val blend: Color = vividBlend(you, them)

    /** Readable ink for text sitting on [color]. */
    fun inkOn(color: Color): Color =
        if (color.luminance() > 0.5f) Color(0xFF0E1226) else Color.White
}

val LocalDuoColors = staticCompositionLocalOf {
    DuoColors(you = DefaultYou, them = DefaultThem)
}

val Duo: DuoColors
    @Composable get() = LocalDuoColors.current

@Composable
fun AmpersandTheme(
    you: Color = DefaultYou,
    them: Color = DefaultThem,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val duo = DuoColors(you, them)
    val scheme = if (darkTheme) darkSchemeFrom(duo) else lightSchemeFrom(duo)

    CompositionLocalProvider(LocalDuoColors provides duo) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AmpersandTypography,
            shapes = AmpersandShapes,
            content = content,
        )
    }
}

// --- Scheme ------------------------------------------------------------------
// Surfaces separate by tone, not outlines.

private fun darkSchemeFrom(duo: DuoColors): ColorScheme {
    val blend = duo.blend
    return darkColorScheme(
        // Untinted: the blend is already clamped to a lightness that carries white text.
        primary = blend,
        onPrimary = duo.inkOn(blend),
        primaryContainer = blend.shade(0.45f),
        onPrimaryContainer = blend.tint(0.65f),
        secondary = duo.you,
        onSecondary = duo.inkOn(duo.you),
        secondaryContainer = duo.you.shade(0.55f),
        onSecondaryContainer = duo.you.tint(0.60f),
        tertiary = duo.them,
        onTertiary = duo.inkOn(duo.them),
        tertiaryContainer = duo.them.shade(0.55f),
        onTertiaryContainer = duo.them.tint(0.60f),

        background = NightBase.mixWith(blend, 0.06f),
        onBackground = NightInk,
        // Card tone, well above the background so cards need no border.
        surface = NightCard.mixWith(blend, 0.07f),
        onSurface = NightInk,
        surfaceVariant = NightCardHigh.mixWith(blend, 0.09f),
        onSurfaceVariant = NightMuted,
        surfaceContainerHighest = NightCardHigh.mixWith(blend, 0.12f),

        outline = blend.shade(0.20f),
        outlineVariant = NightCardHigh.mixWith(blend, 0.14f),
    )
}

private fun lightSchemeFrom(duo: DuoColors): ColorScheme {
    val blend = duo.blend
    return lightColorScheme(
        primary = blend.shade(0.12f),
        onPrimary = Color.White,
        primaryContainer = blend.tint(0.84f),
        onPrimaryContainer = blend.shade(0.58f),
        secondary = duo.you.shade(0.06f),
        onSecondary = Color.White,
        secondaryContainer = duo.you.tint(0.85f),
        onSecondaryContainer = duo.you.shade(0.55f),
        tertiary = duo.them.shade(0.06f),
        onTertiary = Color.White,
        tertiaryContainer = duo.them.tint(0.85f),
        onTertiaryContainer = duo.them.shade(0.55f),

        background = DayBase.mixWith(blend, 0.03f),
        onBackground = DayInk,
        surface = Color.White,
        onSurface = DayInk,
        surfaceVariant = DayCard.mixWith(blend, 0.05f),
        onSurfaceVariant = DayMuted,
        surfaceContainerHighest = DayCard.mixWith(blend, 0.09f),

        outline = blend.tint(0.30f),
        outlineVariant = DayCard.mixWith(blend, 0.10f),
    )
}

/**
 * Wordmark font (Lilita One, SIL Open Font License), used only on the splash.
 * Chosen to avoid any toy company's trademark typeface, like the CC0 art.
 */
val Wordmark = FontFamily(Font(R.font.lilita_one, FontWeight.Normal))

// --- Shape and elevation -----------------------------------------------------

val AmpersandShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Fixed stud sizes. Full-width surfaces compute their stud from the width
 * instead (the page is 12 studs wide).
 */
object Stud {
    /** Markers and pips: depth dots, score pips, a person's initial. */
    val pip = 13.dp

    /** A small brick carrying something: a tray piece, the streak pill. */
    val small = 24.dp

    /** A brick you press that hugs its own label. */
    val chip = 30.dp

    /** The splash only. */
    val hero = 46.dp
}

/**
 * The only gaps in the app. Anything between two of these is a decision nobody
 * meant to make. See DESIGN_SYSTEM.md for which to reach for.
 */
object Space {
    /** Between a label and the line it belongs to, inside a pill. */
    val hair = 2.dp

    /** Between lines of one thing. */
    val xs = 4.dp

    /** Between an icon and its label; tight rows. */
    val s = 8.dp

    /** Between items in a group. */
    val m = 12.dp

    /** A panel's own padding; a label above what it heads. */
    val l = 16.dp

    /** Between sections. */
    val xl = 24.dp

    /** Between unrelated sections. */
    val xxl = 32.dp

    /** Top and bottom of a page. */
    val page = 48.dp
}

/** Named margins, so a screen cannot pick its own. */
object Inset {
    /** Every screen's side margin. The page is 12 studs inside this. */
    val screen = Space.xl
}

/** Corner sizes for things that take a [Dp]. Prefer [Radius] for a shape. */
object Corner {
    /** Thin bars only, where a larger corner would swallow the bar. */
    val hair = 3.dp
    val small = 8.dp
    val medium = 12.dp
    val control = 20.dp
    val card = 28.dp
}

/**
 * How see-through something is. Six steps, each with a job; art that animates between
 * them uses [rise].
 */
object Alpha {
    /** A wash behind everything: the sign-in wall, an owner's tint. */
    const val faint = 0.12f

    /** Bricks not earned yet: an empty pip, an unlit stud. */
    const val unlit = 0.25f

    /** Off: disabled, not selected. */
    const val dim = 0.4f

    /** Resting, or waiting on someone else. Still readable. */
    const val quiet = 0.6f

    /** Secondary text on brick, where the theme's muted colour cannot be used. */
    const val secondary = 0.8f

    /** Nearly solid: a card or a sticky header over the wall; a pressed brick. */
    const val strong = 0.9f

    /** From [from] up to fully solid as [t] goes from 0 to 1. */
    fun rise(from: Float, t: Float): Float = from + (1f - from) * t
}

/** Icon and art sizes. A brick icon is pixel art, so fewer sizes also means less blur. */
object IconSize {
    /** Inside a line of small text. */
    val mark = 14.dp

    /** A marker next to a label. */
    val small = 20.dp

    /** The default: actions and buttons. */
    val medium = 22.dp

    /** The icon that leads a row: weather, a mood, a special day. */
    val large = 32.dp

    /** The icon that is the point of a tile: a deck, a theme. */
    val tile = 44.dp
}

object Radius {
    val hair = RoundedCornerShape(Corner.hair)
    val small = RoundedCornerShape(Corner.small)
    val medium = RoundedCornerShape(Corner.medium)
    val control = RoundedCornerShape(Corner.control)
    val card = RoundedCornerShape(Corner.card)
}

/**
 * Animation durations and springs. [settle] overshoots slightly (arrivals);
 * [glide] has no bounce (following a finger, reflow, anything feeding alpha).
 */
object Motion {
    /** Press-down duration. */
    const val PressMs = 60

    /** One screen or panel replacing another. */
    const val SwapMs = 200

    /** Delay between children in a staggered group. */
    const val StaggerMs = 70

    /** Delay between courses as a tower builds. */
    const val BrickMs = 90

    /** Rejection shake duration. */
    const val ShakeMs = 180

    /** Press travel distance. */
    val Depth = 3.dp

    val settle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    val glide: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

// --- Color helpers -----------------------------------------------------------

/**
 * Midpoint of two colors in HSL (shorter hue arc, clamped saturation and lightness).
 * RGB averaging turns near-complementary pairs gray.
 */
private fun vividBlend(a: Color, b: Color): Color {
    val hslA = FloatArray(3).also { ColorUtils.colorToHSL(a.toArgb(), it) }
    val hslB = FloatArray(3).also { ColorUtils.colorToHSL(b.toArgb(), it) }

    var delta = hslB[0] - hslA[0]
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f

    val hue = (hslA[0] + delta / 2f + 360f) % 360f
    val sat = ((hslA[1] + hslB[1]) / 2f).coerceAtLeast(0.45f)
    // Low ceiling keeps white text on the blend above 3:1 contrast.
    val light = ((hslA[2] + hslB[2]) / 2f).coerceIn(0.34f, 0.56f)

    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, light)))
}

internal fun Color.tint(t: Float): Color = lerp(this, Color.White, t)

internal fun Color.shade(t: Float): Color = lerp(this, Color(0xFF080B18), t)

internal fun Color.mixWith(other: Color, t: Float): Color = lerp(this, other, t)

/**
 * 0xAARRGGBB as a Long, as stored in the model and Firestore. Not `Color.value`,
 * which doesn't round-trip through `Color(Long)`.
 */
fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

// --- Base palette ------------------------------------------------------------

/** Default colors before a couple's colors load. */
internal val DefaultYou = Color(0xFFF54E4F)   // pack red
internal val DefaultThem = Color(0xFF108BE2)  // pack blue

internal val NightBase = Color(0xFF0E1226)
internal val NightCard = Color(0xFF1A2043)
internal val NightCardHigh = Color(0xFF252C56)
internal val NightInk = Color(0xFFEDEFFA)
internal val NightMuted = Color(0xFF9098C4)

internal val DayBase = Color(0xFFF3F4FC)
internal val DayCard = Color(0xFFE8EBF7)
internal val DayInk = Color(0xFF11152B)
internal val DayMuted = Color(0xFF6A7299)

/** The border on a special day's card. The app's only gold, so it stays special. */
internal val SpecialGold = Color(0xFFFFC94D)

/** The four pickable colors, from [com.ampersand.model.BrickColor] (brick art exists only in these). */
val ColorChoices: List<Color> = com.ampersand.model.BrickColor.entries.map { Color(it.argb) }

// --- Type --------------------------------------------------------------------

/** The tab bar: a 2-stud tile has to fit "Shelf" without shrinking the brick. */
val NavLabel = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.2.sp)

/** A partner code, read aloud from one phone to another. */
val InviteCodeText = TextStyle(fontSize = 26.sp)

val AmpersandTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.5).sp,
        ),
        headlineMedium = headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        ),
        headlineSmall = headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
        ),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
        ),
    )
}
