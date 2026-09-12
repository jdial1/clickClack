package com.ampersand.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.toArgbLong

/**
 * The form kit, in brick.
 *
 * One rule keeps it coherent: **things you press are bricks** -- studs on top, a
 * darker lip underneath that shortens when pressed, like a key going down.
 * **Things you type into are sockets** -- recessed, no studs, because studs
 * behind a cursor is just clutter.
 */

private val Pad = 18.dp
private val Corner = 20.dp

// --- Text input --------------------------------------------------------------

/** A socket: recessed, studless, with the accent arriving on focus. */
@Composable
fun AmpField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    val sounds = LocalSounds.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ring by animateColorAsState(
        if (focused) accent else Color.Transparent,
        label = "fieldRing",
    )

    LaunchedEffect(focused) { if (focused) sounds.play(Sfx.FOCUS) }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corner))
            // Inset rather than raised: the socket is the inverse of a brick.
            .background(lerp(MaterialTheme.colorScheme.surfaceVariant, Color.Black, 0.14f))
            .padding(bottom = 5.dp)
            .clip(RoundedCornerShape(Corner))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, ring, RoundedCornerShape(Corner))
            .padding(horizontal = Pad, vertical = 16.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            interactionSource = interaction,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(accent),
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Segmented (pill tabs) ---------------------------------------------------

/** A tray with one brick pressed into it. */
@Composable
fun AmpSegmented(
    options: List<String>,
    selected: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val sounds = LocalSounds.current
    Row(
        modifier
            .clip(RoundedCornerShape(Corner))
            .background(lerp(MaterialTheme.colorScheme.surfaceVariant, Color.Black, 0.14f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            val fg by animateColorAsState(
                if (on) Duo.inkOn(accent) else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segFg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (on) {
                            Modifier.brickFace(accent, corner = 14.dp, lip = 4.dp)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { sounds.play(Sfx.TAP); onSelect(option) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(option, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

// --- Radio -------------------------------------------------------------------

/**
 * Single choice, one 1-stud brick per option.
 *
 * Each option gets its own colour from the palette so the group reads as a
 * handful of loose bricks rather than a list. The unpicked ones are the same
 * brick at low alpha — dimmed, not greyed, so the colour still identifies the
 * option.
 */
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
    val shape = BrickShape(1, BrickHeight.MEDIUM)
    val colors = remember(options.size) {
        List(options.size) { BrickColor.at(it) }
    }
    val atlas = rememberBricks(colors.map { it.asset(shape) })

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, option ->
            val on = option == selected
            val brick = colors[index]
            val alpha by animateFloatAsState(if (on) 1f else 0.3f, label = "radioBrick")

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Corner))
                    .brickFace(
                        if (on) {
                            Color(brick.argb)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        corner = Corner,
                        studded = false,
                        pressed = on,
                    )
                    .clickable(enabled = enabled) {
                        sounds.play(if (on) Sfx.DESELECT else Sfx.SELECT)
                        onSelect(option)
                    }
                    .padding(start = 14.dp, end = Pad, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrickTile(
                    shape = shape,
                    color = brick,
                    studSize = 30.dp,
                    atlas = atlas,
                    alpha = alpha,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (on) {
                        Duo.inkOn(Color(brick.argb))
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            val on = option in selected
            val blocked = max != null && !on && selected.size >= max
            ChoiceRow(
                label = option,
                selected = on,
                accent = accent,
                enabled = enabled && !blocked,
                onClick = { onToggle(option) },
            ) { checked, ink -> CheckBox(checked, ink) }
        }
        if (max != null) {
            Text(
                "Pick up to $max.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
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
    Canvas(Modifier.size(22.dp)) {
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
            // Two legs drawn in proportion, so the tick appears to be written.
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
    indicator: @Composable (Boolean, Color) -> Unit,
) {
    val sounds = LocalSounds.current
    val bg by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
        label = "rowBg",
    )
    val ink by animateColorAsState(
        if (selected) Duo.inkOn(accent) else MaterialTheme.colorScheme.onSurface,
        label = "rowInk",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corner))
            // Selected presses the brick down: the lip shortens.
            .brickFace(bg, corner = Corner, pressed = selected)
            .clickable(enabled = enabled) {
                sounds.play(if (selected) Sfx.DESELECT else Sfx.SELECT)
                onClick()
            }
            .padding(start = Pad, end = Pad, top = 26.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        indicator(selected, ink)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) ink else ink.copy(alpha = 0.4f),
        )
    }
}

// --- Dropdown ----------------------------------------------------------------

/**
 * Built on a plain [DropdownMenu] rather than ExposedDropdownMenuBox, so the
 * anchor is entirely ours and matches the other controls exactly.
 */
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
    var open by remember { mutableStateOf(false) }
    val caret by animateFloatAsState(if (open) 180f else 0f, label = "ddCaret")

    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Corner))
                .brickFace(
                    if (open) accent else MaterialTheme.colorScheme.surfaceVariant,
                    corner = Corner,
                    pressed = open,
                )
                .clickable(enabled = enabled) { sounds.play(Sfx.TAP); open = true }
                .padding(start = Pad, end = Pad, top = 26.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    open -> Duo.inkOn(accent)
                    selected == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            Caret(
                color = if (open) {
                    Duo.inkOn(accent)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.rotate(caret),
            )
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
                    onClick = { sounds.play(Sfx.SELECT); onSelect(option); open = false },
                )
            }
        }
    }
}

@Composable
private fun Caret(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp)) {
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
 * A rating you build rather than tick: drag the run of bricks out from 1 stud to
 * [steps].
 *
 * The pack has no 3- or 5-stud brick, so a run of that length is assembled the
 * way you would out of the box -- a 2 and a 1 -- via [BrickArt.runOf]. Empty
 * studs show as sockets on the baseplate behind, so the full range is always
 * visible and the target is the whole strip rather than a small handle.
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
    val brick = remember(accent) { BrickColor.of(accent.toArgbLong()) }
    val assets = remember(brick) { BrickArt.WIDTHS.map { brick.asset(BrickShape(it, BrickHeight.MEDIUM)) } }
    val atlas = rememberBricks(assets)
    val socket = MaterialTheme.colorScheme.surfaceVariant
    val socketInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    // Live value follows the finger; the committed value only changes on a
    // stud boundary, which is what makes the drag feel like it snaps.
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
                    // One gesture loop, not detectHorizontalDragGestures plus
                    // detectTapGestures: two pointerInput blocks on the same
                    // node fight over the down event and neither wins, so the
                    // strip rendered correctly but ignored every drag. Handling
                    // press and drag in one place also means a tap is simply a
                    // drag of zero distance.
                    .pointerInput(enabled, steps) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            var last = studAt(down.position.x)
                            sounds.play(Sfx.PICK_UP)
                            onSelect(last)

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
                                        onSelect(next)
                                    }
                                }
                            }
                            sounds.play(Sfx.PLACE)
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

                // The run itself, largest bricks first.
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

        Spacer(Modifier.height(10.dp))
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
 * The primary action, laid as a real course of bricks.
 *
 * [slug] must be a pack colourway; the theme's `primary` is the blend of the two
 * of you and has no artwork, so callers pass one of the four instead.
 */
@Composable
fun AmpButton(
    label: String,
    slug: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sfx: Sfx = Sfx.PRESS,
    onClick: () -> Unit,
) {
    val sounds = LocalSounds.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Disabled is a real brick, and the *quiet* brick for whichever ground it
    // sits on: black on navy, neutral on a pale background. Two wrong answers
    // were tried first. A faded slab became a ghost that bled into the control
    // above it. A neutral brick in both themes made the disabled button the
    // brightest thing on a dark screen.
    //
    // The label is drawn at full opacity, not faded. Fading it is what produces
    // grey text on a black brick, and the brick is already saying the control is
    // off -- the words do not have to be hard to read as well.
    val offSlug = structureSlug()

    BrickSlab(
        slugs = listOf(if (enabled) slug else offSlug),
        studs = 12,
        height = BrickHeight.HIGH,
        alpha = if (pressed) 0.82f else 1f,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) { sounds.play(sfx); onClick() },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = inkOnBrick(if (enabled) slug else offSlug),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 14.dp, bottom = 22.dp),
        )
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
