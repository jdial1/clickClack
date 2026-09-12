package com.ampersand.ui.theme

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils

/**
 * The two personal colours, and the colour they make together.
 *
 * This is the identity of the app. Your surfaces lean your hue, theirs lean
 * theirs, and the hero surfaces are painted in the gradient *between* the two --
 * which is the whole idea of the product, said in colour.
 */
@Immutable
data class DuoColors(
    val you: Color,
    val them: Color,
) {
    val blend: Color = vividBlend(you, them)

    /** The signature gradient: you on one end, them on the other. */
    val sweep: Brush = Brush.linearGradient(listOf(you, blend, them))

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
// Surfaces are filled and layered rather than outlined. Contrast comes from
// stacking tones, not from hairlines -- outlines at 1dp read as "form", and this
// should read as "app".

private fun darkSchemeFrom(duo: DuoColors): ColorScheme {
    val blend = duo.blend
    return darkColorScheme(
        // Not tinted lighter: the blend is already clamped to a lightness that
        // carries white text, and lifting it here would undo that.
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
        // The card tone. Deliberately well above the background so a filled card
        // separates with no border at all.
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

// --- Shape and elevation -----------------------------------------------------

val AmpersandShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

object Radius {
    val chip = RoundedCornerShape(14.dp)
    val control = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(28.dp)
    val hero = RoundedCornerShape(32.dp)
    val pill = RoundedCornerShape(100.dp)
}

/**
 * How long things take, in one place.
 *
 * The register is *physical, not showy*: everything in this app is a brick, and
 * a brick has weight, a short travel and a hard stop. Nothing here celebrates.
 * If a number below grows past its comment the app stops feeling like objects
 * and starts feeling like a slideshow, which is the failure mode to watch for.
 *
 * Two springs, not a family of easings. [settle] is a thing arriving and
 * bedding in -- it overshoots a little, the way the splash bricks recoil.
 * [glide] is a thing moving that never stopped -- a ghost following a finger,
 * a row shuffling past another. Almost everything is one or the other.
 */
object Motion {
    /** A key going down is instant; coming back up is allowed to be seen. */
    const val PressMs = 60
    const val LiftMs = 110

    /** One surface replacing another: a screen, a panel, a question. */
    const val SwapMs = 200

    /** The beat between children arriving in a staggered group. */
    const val StaggerMs = 70

    /** The beat between courses as a tower builds itself. */
    const val BrickMs = 90

    /** Refusing something. Two cycles, and over before it is a cartoon. */
    const val ShakeMs = 180

    /** One nub of travel. More than this and buttons read as squashy. */
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

/**
 * A wide, soft, *coloured* shadow rather than the default black one.
 *
 * This is most of why the reference designs feel like they float: the glow picks
 * up the element's own hue instead of dropping grey underneath it. Coloured
 * shadows need API 28; below that it degrades to the standard black shadow,
 * which still reads fine.
 */
fun Modifier.glow(
    color: Color,
    shape: Shape,
    elevation: androidx.compose.ui.unit.Dp = 18.dp,
    alpha: Float = 0.45f,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = color.copy(alpha = alpha),
    spotColor = color.copy(alpha = alpha),
)

// --- Colour helpers ----------------------------------------------------------

/**
 * The midpoint of two colours, in HSL rather than RGB.
 *
 * Averaging channels is the obvious way to blend and it is wrong here: two
 * near-complementary colours (terracotta and teal, say) average to grey, so the
 * couple whose colours sit opposite each other on the wheel gets a mud-coloured
 * app while everyone else gets a vivid one. That is a lottery, not a design.
 *
 * So: walk the *shorter* arc between the two hues and land halfway, keep the
 * mean saturation with a floor under it, and keep the mean lightness. Coral and
 * indigo then meet at magenta instead of at sludge, and every pair produces a
 * hero gradient worth looking at.
 */
private fun vividBlend(a: Color, b: Color): Color {
    val hslA = FloatArray(3).also { ColorUtils.colorToHSL(a.toArgb(), it) }
    val hslB = FloatArray(3).also { ColorUtils.colorToHSL(b.toArgb(), it) }

    var delta = hslB[0] - hslA[0]
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f

    val hue = (hslA[0] + delta / 2f + 360f) % 360f
    // Floor stops two washed-out picks producing a washed-out blend.
    val sat = ((hslA[1] + hslB[1]) / 2f).coerceAtLeast(0.45f)
    // Ceiling kept low deliberately. The blend is the hero background and the
    // selected-tab fill, and both carry white text -- a lighter magenta looks
    // punchier in isolation and drops the contrast under 3:1 in use.
    val light = ((hslA[2] + hslB[2]) / 2f).coerceIn(0.34f, 0.56f)

    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, light)))
}

internal fun Color.tint(t: Float): Color = lerp(this, Color.White, t)

internal fun Color.shade(t: Float): Color = lerp(this, Color(0xFF080B18), t)

internal fun Color.mixWith(other: Color, t: Float): Color = lerp(this, other, t)

/**
 * 0xAARRGGBB as a Long, which is what the model stores and what Firestore will
 * hold. Not `Color.value` -- that is a packed float representation carrying a
 * colour space, and it does not round-trip through `Color(Long)`.
 */
fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

// --- Base palette ------------------------------------------------------------

/** Coral and indigo. Their blend is an orchid, and the gradient between them
 *  is the app's signature surface. */
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

/**
 * The only four colours, taken from [com.ampersand.model.BrickColor].
 *
 * Not a free picker any more: the bricks exist in these colourways and nothing
 * else, so offering arbitrary hues meant the chrome could never match the art.
 */
val ColorChoices: List<Color> = com.ampersand.model.BrickColor.entries.map { Color(it.argb) }

// --- Type --------------------------------------------------------------------
// Geometric sans, heavy where it matters. The references all lead with weight
// and size rather than with a display face, and questions read as something the
// app is asking rather than something printed.

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
