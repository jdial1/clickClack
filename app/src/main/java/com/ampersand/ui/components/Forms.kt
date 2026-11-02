package com.ampersand.ui.components

import com.ampersand.ui.theme.IconSize
import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.Space
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import androidx.compose.ui.unit.Dp
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.brick.BrickTile
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.toArgbLong
import com.ampersand.ui.theme.Stud
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import kotlin.math.floor

/** Form controls drawn as bricks: text field, radio, checkbox, dropdown, scale, button. */

private val Pad = 18.dp
private val Corner = 20.dp

// --- Text input --------------------------------------------------------------

/**
 * Text field drawn as bricks that grow with the text (see [FieldCourses]).
 * Line height is set from the brick so each text line is one course; the
 * placeholder's layout sizes the bricks while empty. Height follows content.
 */
@Composable
fun AmpField(
    value: String,
    onValueChange: (String) -> Unit,
    color: BrickColor,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) { sounds.play(Sfx.FOCUS); feel.play(Feel.FOCUS) } }

    // Lifts slightly while focused.
    val lift by animateFloatAsState(if (focused) 1f else 0f, Motion.glide, label = "fieldLift")

    // Text color contrasts with the brick color.
    val ink = inkOnBrick(color.slug)
    val asset = BrickArt.assetFor(color.slug, FieldBrick)
    val atlas = rememberBricks(listOf(asset))

    // Line count and last line's right edge of the visible text.
    var lines by remember { mutableIntStateOf(1) }
    var lastRight by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val studPx = constraints.maxWidth.toFloat() / (FieldCourses.Studs * FieldCourses.PerLine)
        // Rounded down so courses overlap rather than leave a gap.
        val coursePx = floor(studPx * FieldBrick.h * BrickArt.ROW_RATIO)
        val nubPx = studPx * BrickArt.NUB_RATIO
        val insetPx = studPx * FieldInset
        val oneBrick = FieldCourses.Studs * studPx - insetPx - studPx * FieldEdge

        val courses = FieldCourses.of(lines, lastRight, oneBrick)

        val style = MaterialTheme.typography.bodyLarge.copy(
            color = ink,
            lineHeight = with(density) { coursePx.toSp() },
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
        val textPad = with(density) {
            Modifier.padding(start = insetPx.toDp(), end = insetPx.toDp(), top = nubPx.toDp())
        }

        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = -lift * FieldLift.toPx()
                    // Alpha on one offscreen layer, or lower nubs show through translucent bricks.
                    alpha = if (enabled) 1f else Alpha.dim
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                // Bottom course first, so each course covers the nubs below it.
                for (row in courses.indices.reversed()) {
                    repeat(courses[row]) { brick ->
                        drawBrickArt(
                            atlas = atlas,
                            asset = asset,
                            studs = FieldCourses.Studs,
                            rows = FieldBrick.h,
                            left = brick * FieldCourses.Studs * studPx,
                            bottom = nubPx + (row + 1) * coursePx,
                            studPx = studPx,
                        )
                    }
                }
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                interactionSource = interaction,
                textStyle = style,
                cursorBrush = SolidColor(ink),
                onTextLayout = { layout ->
                    // While empty, the placeholder's layout sizes the bricks.
                    if (value.isNotEmpty()) {
                        lines = layout.lineCount
                        lastRight = layout.getLineRight(layout.lineCount - 1)
                    }
                },
                modifier = Modifier.fillMaxWidth().then(textPad),
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = style.copy(color = ink.copy(alpha = Alpha.secondary)),
                    onTextLayout = { layout ->
                        lines = layout.lineCount
                        lastRight = layout.getLineRight(layout.lineCount - 1)
                    },
                    modifier = Modifier.fillMaxWidth().then(textPad),
                )
            }
        }
    }
}

/** Brick shape for text fields: four studs, one course per line. */
private val FieldBrick = BrickShape(FieldCourses.Studs, BrickHeight.MEDIUM)

/** Horizontal text inset, as a fraction of a stud. */
private const val FieldInset = 0.4f

/** Margin before the first brick's right edge that triggers a second brick, in studs. */
private const val FieldEdge = 0.2f

/** Lift distance while focused. */
private val FieldLift = 2.dp

// --- Radio -------------------------------------------------------------------

/** Single choice. Each option has its own palette color; unselected options are dimmed. */
@Composable
fun AmpRadioGroup(
    options: List<String>,
    selected: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val shape = BrickShape(1, BrickHeight.MEDIUM)
    val colors = remember(options.size) {
        List(options.size) { BrickColor.at(it) }
    }
    val atlas = rememberBricks(colors.map { it.asset(shape) })

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.m)) {
        options.forEachIndexed { index, option ->
            val on = option == selected
            val brick = colors[index]
            val alpha by animateFloatAsState(if (on) 1f else Alpha.unlit, label = "radioBrick")

            BrickSlab(
                slugs = listOf(if (on) brick.slug else structureSlug()),
                studs = 12,
                height = BrickHeight.MEDIUM,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                    ) {
                        // Tapping the selected option does nothing.
                        if (!on) {
                            sounds.play(Sfx.SELECT)
                            feel.play(Feel.SELECT)
                            onSelect(option)
                        }
                    },
            ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Space.m, end = Pad, top = Space.xl, bottom = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrickTile(
                    shape = shape,
                    color = brick,
                    studSize = Stud.chip,
                    atlas = atlas,
                    alpha = alpha,
                )
                Spacer(Modifier.width(Space.m))
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = inkOnBrick(if (on) brick.slug else structureSlug()),
                )
            }
            }
        }
    }
}

// --- Checkbox ----------------------------------------------------------------

/** Multiple choice, same brick anatomy as the radio. */
@Composable
fun AmpCheckboxGroup(
    options: List<String>,
    selected: Set<String>,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    max: Int? = null,
    onToggle: (String) -> Unit,
) {
    // Incremented on each blocked tap to retrigger the shake.
    var refused by remember { mutableIntStateOf(0) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.m)) {
        options.forEach { option ->
            val on = option in selected
            val blocked = max != null && !on && selected.size >= max
            ChoiceRow(
                label = option,
                selected = on,
                accent = accent,
                enabled = enabled && !blocked,
                onClick = { onToggle(option) },
                onBlocked = { refused++ },
            ) { checked, ink -> CheckBox(checked, ink) }
        }
        if (max != null) {
            Text(
                "Pick up to $max.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = Space.xs, top = Space.hair)
                    .shake(refused),
            )
        }
    }
}

@Composable
private fun CheckBox(on: Boolean, ink: Color) {
    val progress by animateFloatAsState(
        if (on) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkFill",
    )
    val resting = MaterialTheme.colorScheme.onSurfaceVariant
    // Resolved here: the Canvas draw scope is not a composable context.
    val tickOnFill = Duo.inkOn(ink)
    Canvas(Modifier.size(IconSize.medium)) {
        val stroke = 2.dp.toPx()
        val radius = CornerRadius(7.dp.toPx())
        drawRoundRect(
            color = if (on) ink.copy(alpha = progress) else Color.Transparent,
            cornerRadius = radius,
        )
        drawRoundRect(
            color = if (on) ink else resting,
            cornerRadius = radius,
            style = Stroke(width = stroke),
        )
        if (progress > 0f) {
            // Draw the two legs of the check in sequence.
            val w = size.width
            val p1 = Offset(w * 0.27f, w * 0.52f)
            val p2 = Offset(w * 0.44f, w * 0.69f)
            val p3 = Offset(w * 0.74f, w * 0.34f)
            val tickInk = if (on) tickOnFill else ink
            val leg1 = (progress / 0.4f).coerceAtMost(1f)
            drawLine(
                tickInk, p1,
                Offset(p1.x + (p2.x - p1.x) * leg1, p1.y + (p2.y - p1.y) * leg1),
                strokeWidth = stroke, cap = StrokeCap.Round,
            )
            if (progress > 0.4f) {
                val leg2 = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
                drawLine(
                    tickInk, p2,
                    Offset(p2.x + (p3.x - p2.x) * leg2, p2.y + (p3.y - p2.y) * leg2),
                    strokeWidth = stroke, cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** Shared anatomy. `indicator` receives (selected, inkColor). */
@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    onBlocked: () -> Unit = {},
    indicator: @Composable (Boolean, Color) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    // Accent brick when selected, neutral brick otherwise.
    val slug = if (selected) BrickColor.of(accent.toArgbLong()).slug else structureSlug()
    val ink = inkOnBrick(slug)

    BrickSlab(
        slugs = listOf(slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        alpha = if (enabled) 1f else Alpha.dim,
        modifier = Modifier
            .fillMaxWidth()
            // Blocked rows stay clickable so they can give reject feedback.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (enabled) {
                    sounds.play(if (selected) Sfx.DESELECT else Sfx.SELECT)
                    feel.play(if (selected) Feel.DESELECT else Feel.SELECT)
                    onClick()
                } else {
                    sounds.play(Sfx.DESELECT)
                    feel.play(Feel.REJECT)
                    onBlocked()
                }
            },
    ) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Pad, end = Pad, top = Space.xl, bottom = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        indicator(selected, ink)
        Spacer(Modifier.width(Space.m))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = ink,
        )
    }
    }
}

// --- Dropdown ----------------------------------------------------------------

/** Dropdown on a plain [DropdownMenu] so the anchor can be a brick like other controls. */
@Composable
fun AmpDropdown(
    options: List<String>,
    selected: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose",
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    var open by remember { mutableStateOf(false) }
    val caret by animateFloatAsState(if (open) 180f else 0f, label = "ddCaret")

    Box(modifier.fillMaxWidth()) {
        val anchorSlug = if (open) BrickColor.of(accent.toArgbLong()).slug else structureSlug()
        BrickSlab(
            slugs = listOf(anchorSlug),
            studs = 12,
            height = BrickHeight.MEDIUM,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                ) { sounds.play(Sfx.TAP); feel.play(Feel.TAP); open = true },
        ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = Pad, end = Pad, top = Space.xl, bottom = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = inkOnBrick(anchorSlug),
                modifier = Modifier.weight(1f),
            )
            Caret(
                color = inkOnBrick(anchorSlug),
                modifier = Modifier.rotate(caret),
            )
        }
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth(0.9f),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == selected) {
                                accent
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = { sounds.play(Sfx.SELECT); feel.play(Feel.SELECT); onSelect(option); open = false },
                )
            }
        }
    }
}

@Composable
private fun Caret(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(IconSize.mark)) {
        val s = 2.dp.toPx()
        drawLine(
            color, Offset(size.width * 0.2f, size.height * 0.38f),
            Offset(size.width * 0.5f, size.height * 0.66f),
            strokeWidth = s, cap = StrokeCap.Round,
        )
        drawLine(
            color, Offset(size.width * 0.5f, size.height * 0.66f),
            Offset(size.width * 0.8f, size.height * 0.38f),
            strokeWidth = s, cap = StrokeCap.Round,
        )
    }
}

// --- Scale -------------------------------------------------------------------

/**
 * Rating from 1 to [steps]: tap or drag along a strip of sockets to size a run of bricks.
 * Lengths without a matching brick are built from smaller ones via [BrickArt.runOf].
 */
@Composable
fun AmpScale(
    value: Int?,
    accent: Color,
    modifier: Modifier = Modifier,
    steps: Int = 6,
    lowLabel: String = "Not at all",
    highLabel: String = "Completely",
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    // The gesture outlives recompositions; without this it wrote to the previous question.
    val select by rememberUpdatedState(onSelect)
    val brick = remember(accent) { BrickColor.of(accent.toArgbLong()) }
    val assets = remember(brick) { BrickArt.WIDTHS.map { brick.asset(BrickShape(it, BrickHeight.MEDIUM)) } }
    val atlas = rememberBricks(assets)
    val socket = MaterialTheme.colorScheme.surfaceVariant
    val socketInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.dim)

    val current = (value ?: 1).coerceIn(1, steps)

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val studDp = maxWidth / steps
            val studPx = with(LocalDensity.current) { studDp.toPx() }
            val height = studDp * (BrickHeight.MEDIUM.rows * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)

            fun studAt(x: Float): Int =
                ((x / studPx) + 0.5f).toInt().coerceIn(1, steps)

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    // One gesture loop for tap and drag: separate tap/drag detectors
                    // on one node fight over the down event and drags get ignored.
                    .pointerInput(enabled, steps, studPx) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            var last = studAt(down.position.x)
                            sounds.play(Sfx.PICK_UP)
                            feel.play(Feel.PICK_UP)
                            select(last)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: break
                                if (!change.pressed) break
                                if (change.positionChanged()) {
                                    change.consume()
                                    val next = studAt(change.position.x)
                                    if (next != last) {
                                        last = next
                                        sounds.play(Sfx.TAP)
                                        feel.play(Feel.TICK)
                                        select(next)
                                    }
                                }
                            }
                            sounds.play(Sfx.PLACE)
                            feel.play(Feel.PLACE)
                        }
                    },
            ) {
                val rowPx = studPx * BrickArt.ROW_RATIO
                val nubPx = studPx * BrickArt.NUB_RATIO
                val bottom = size.height

                // Baseplate sockets for the full range.
                repeat(steps) { i ->
                    drawRoundRect(
                        color = socket,
                        topLeft = Offset(i * studPx + studPx * 0.06f, nubPx),
                        size = Size(studPx * 0.88f, rowPx * 2),
                        cornerRadius = CornerRadius(studPx * 0.12f),
                    )
                    drawCircle(
                        color = socketInk,
                        radius = studPx * 0.08f,
                        center = Offset((i + 0.5f) * studPx, nubPx + rowPx),
                    )
                }

                // The selected run, largest bricks first.
                var x = 0f
                BrickArt.runOf(current).forEach { w ->
                    drawBrickArt(
                        atlas = atlas,
                        asset = brick.asset(BrickShape(w, BrickHeight.MEDIUM)),
                        studs = w,
                        rows = BrickHeight.MEDIUM.rows,
                        left = x,
                        bottom = bottom,
                        studPx = studPx,
                    )
                    x += w * studPx
                }
            }
        }

        Spacer(Modifier.height(Space.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                lowLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$current of $steps",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                highLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/**
 * Primary button drawn as a brick. [slug] must be a brick asset color; the
 * theme's blended `primary` has no artwork.
 */
@Composable
fun AmpButton(
    label: String,
    slug: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Null when the caller plays its own sound. */
    sfx: Sfx? = Sfx.RELEASE,
    feelOnClick: Feel = Feel.RELEASE,
    /** An action icon. Buttons with one are grey rather than [slug], to keep colour for what matters. */
    icon: UiIcon? = null,
    onClick: () -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val interaction = remember { MutableInteractionSource() }
    val lit: String? = if (icon != null) null else slug
    val pressed by interaction.collectIsPressedAsState()

    // Generic press sound on down; [sfx] plays on click, which only fires if the press completes.
    LaunchedEffect(pressed) {
        if (pressed) {
            sounds.play(Sfx.PRESS)
            feel.play(Feel.PRESS)
        }
    }

    // Disabled uses the theme's structure brick at full opacity, label included.
    val offSlug = structureSlug()

    BrickSlab(
        slugs = listOf(if (enabled) lit else offSlug),
        studs = 12,
        height = BrickHeight.HIGH,
        snug = true,
        alpha = if (pressed) Alpha.strong else 1f,
        modifier = modifier
            .fillMaxWidth()
            .pressDepth(pressed)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) { sfx?.let(sounds::play); feel.play(feelOnClick); onClick() },
    ) {
        Row(
            Modifier
                .align(Alignment.Center)
                .padding(top = 14.dp, bottom = 22.dp), // course-fit: one course of HIGH brick, nub included
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                IconBrick(it, size = IconSize.medium)
                Spacer(Modifier.width(Space.s))
            }
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = inkOnBrick(if (enabled) lit else offSlug),
            )
        }
    }
}

/** A real 2-stud brick, for palettes and legends. */
@Composable
fun ColorChip(
    label: String,
    color: BrickColor,
    modifier: Modifier = Modifier,
    studSize: Dp = 26.dp,
) {
    val shape = BrickShape(2, BrickHeight.MEDIUM)
    val atlas = rememberBricks(listOf(color.asset(shape)))
    Box(modifier, contentAlignment = Alignment.Center) {
        BrickTile(shape = shape, color = color, studSize = studSize, atlas = atlas)
        Text(
            text = label.take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = inkOnBrick(color.slug),
            modifier = Modifier.padding(top = studSize * BrickArt.NUB_RATIO),
        )
    }
}
