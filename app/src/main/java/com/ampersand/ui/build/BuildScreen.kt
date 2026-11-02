package com.ampersand.ui.build

import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.Inset
import com.ampersand.ui.theme.Space
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.geometry.Size

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.model.Block
import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickAtlas
import com.ampersand.ui.brick.drawBrick
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.Stud
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Month = DateTimeFormatter.ofPattern("MMM d")

/** Stud width in the tray. */
private val TrayStud = Stud.small

/** The tray's height, full or empty. */
private val TrayHeight = 78.dp

/** The shared baseplate: drag earned bricks from your tray onto it. Bricks keep their owner's color. */
@Composable
fun BuildScreen(
    state: CoupleState,
    plate: BuildPlate,
    onPlace: (blockId: String, x: Int, y: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val atlas = rememberBricks(plate.blocks.map { it.asset })
    // Your bricks, plus any of theirs left a week: either of you can finish the other's corner.
    val tray = plate.placeable(state.you.uid, state.today.date)

    // All positions are relative to this screen's root so tray drags resolve against the plate.
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var plateOrigin by remember { mutableStateOf(Offset.Zero) }
    var studPx by remember { mutableStateOf(0f) }

    var dragging by remember { mutableStateOf<Block?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    // Pickup point, where a refused drop animates back to.
    var grabbedAt by remember { mutableStateOf(Offset.Zero) }
    // While a dropped brick animates, freeze its target and ignore new drags so a second grab can't hijack it.
    var flying by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val scope = rememberCoroutineScope()

    // detectDragGestures keeps its original lambdas, so read the plate through updated state.
    val currentPlate by rememberUpdatedState(plate)

    fun resolveTarget(): Pair<Int, Int>? {
        val block = dragging ?: return null
        if (studPx <= 0f) return null
        val p = currentPlate
        val rowPx = studPx * BrickArt.ROW_RATIO
        val occupied = p.occupied()

        val ghostLeft = dragPos.x - block.shape.w * studPx / 2f
        val ghostTop = dragPos.y - block.shape.h * rowPx / 2f
        // Clamped: a brick hanging a little off the edge lands at the edge instead of being refused.
        val x = ((ghostLeft - plateOrigin.x) / studPx).roundToInt().coerceIn(0, (p.gridW - block.shape.w).coerceAtLeast(0))
        val fromTop = ((ghostTop - plateOrigin.y) / rowPx).roundToInt()
        val y = p.gridH - block.shape.h - fromTop

        return when {
            p.canPlace(block.shape, x, y, occupied) -> x to y
            // If the exact row is illegal, drop down that column instead.
            else -> p.dropRow(block.shape, x, occupied)?.let { x to it }
        }
    }

    /**
     * On release, animate the floating brick to its target cell, or back to [grabbedAt] with a reject haptic.
     * [onPlace] runs only after the animation so the plate never draws the brick twice.
     */
    fun commitDrag() {
        val block = dragging ?: return
        if (flying) return
        val landing = resolveTarget()
        flying = true
        frozen = landing

        scope.launch {
            val rowPx = studPx * BrickArt.ROW_RATIO
            val destination = if (landing != null) {
                val (x, y) = landing
                // The body's centre, where the plate will draw it: no nub offset, or it lands low and hops up.
                Offset(
                    plateOrigin.x + x * studPx + block.shape.w * studPx / 2f,
                    plateOrigin.y +
                        (currentPlate.gridH - y - block.shape.h) * rowPx +
                        block.shape.h * rowPx / 2f,
                )
            } else {
                feel.play(Feel.REJECT)
                grabbedAt
            }

            val from = dragPos
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = if (landing != null) {
                    Motion.settle
                } else {
                    // A refused brick wobbles home: the material says no, and no text does.
                    androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    )
                },
            ) { p, _ -> dragPos = lerp(from, destination, p) }

            if (landing != null) {
                sounds.play(Sfx.PLACE)
                feel.play(Feel.PLACE)
                onPlace(block.id, landing.first, landing.second)
            }
            dragging = null
            flying = false
            frozen = null
        }
    }

    val target = frozen ?: resolveTarget()

    // Stud snap: feel each new cell the brick would land in. Haptic only, never sound.
    androidx.compose.runtime.LaunchedEffect(target) {
        if (dragging != null && !flying && target != null) feel.play(Feel.TICK)
    }

    Box(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootOrigin = it.positionInRoot() },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Inset.screen)) {
            Spacer(Modifier.height(Space.m))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(
                    "${plate.startedOn.format(Month)} — ${plate.endsOn().format(Month)}",
                )
                SectionLabel(
                    "${plate.daysLeft(state.today.date)} days left",
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(Space.m))

            Plate(
                plate = plate,
                atlas = atlas,
                target = target,
                dragging = dragging,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(BrickArt.aspect(plate.gridW, plate.gridH))
                    .onGloballyPositioned {
                        plateOrigin = it.positionInRoot() - rootOrigin
                        studPx = it.size.width.toFloat() / plate.gridW
                    },
            )

            Spacer(Modifier.height(Space.l))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionLabel("Your bricks")
                SectionLabel(
                    if (tray.isEmpty()) "All placed" else "${tray.size} to place",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(Space.m))

            // Same height as the brick row, so placing your last brick never moves the page.
            if (tray.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(TrayHeight)
                        .clip(Radius.control)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Answer today's question to earn another.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(TrayHeight)
                        // Frozen while a brick is in the air, so the tray never slides under your finger.
                        .horizontalScroll(rememberScrollState(), enabled = dragging == null),
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // key() is required: without it a slot is reused for the next block, its gesture
                    // detector restarts on the same touch, and one drag places the whole tray.
                    tray.forEach { block ->
                        key(block.id) {
                            TrayBrick(
                                block = block,
                                atlas = atlas,
                                faded = dragging?.id == block.id,
                                onDragStart = { at ->
                                    if (!flying) {
                                        sounds.play(Sfx.PICK_UP)
                                        feel.play(Feel.PICK_UP)
                                        dragging = block
                                        dragPos = at - rootOrigin
                                        grabbedAt = at - rootOrigin
                                    }
                                },
                                onDrag = { delta -> if (!flying) dragPos += delta },
                                onDragEnd = { commitDrag() },
                                onDragCancel = { commitDrag() },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.xl))
        }

        // Dragged brick, drawn last so it sits on top.
        dragging?.let { block ->
            val rowPx = studPx * BrickArt.ROW_RATIO
            val nubPx = studPx * BrickArt.NUB_RATIO
            val wDp = with(density) { (block.shape.w * studPx).toDp() }
            val hDp = with(density) { (block.shape.h * rowPx + nubPx).toDp() }
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (dragPos.x - block.shape.w * studPx / 2f).roundToInt(),
                            (dragPos.y - block.shape.h * rowPx / 2f - nubPx).roundToInt(),
                        )
                    }
                    .size(wDp, hDp),
            ) {
                Canvas(Modifier.size(wDp, hDp)) {
                    drawBrick(
                        block = block,
                        atlas = atlas,
                        left = 0f,
                        bottom = size.height,
                        studPx = studPx,
                        alpha = Alpha.strong,
                    )
                }
            }
        }
    }
}

/** The baseplate and all placed bricks, in one Canvas. */
@Composable
private fun Plate(
    plate: BuildPlate,
    atlas: BrickAtlas,
    target: Pair<Int, Int>?,
    dragging: Block?,
    modifier: Modifier = Modifier,
) {
    val duo = Duo
    val studHint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.unlit)
    val plateBg = MaterialTheme.colorScheme.surfaceVariant

    // Control radius, not card: a 28dp corner visibly clips the baseplate row.
    Canvas(modifier.clip(Radius.control).background(plateBg)) {
        val stud = size.width / plate.gridW
        val row = stud * BrickArt.ROW_RATIO

        // Faint stud grid: every column, every other row to keep it light.
        for (gx in 0 until plate.gridW) {
            for (gy in 0 until plate.gridH step 2) {
                drawCircle(
                    color = studHint,
                    radius = stud * 0.05f,
                    center = Offset((gx + 0.5f) * stud, (plate.gridH - gy - 1f) * row),
                )
            }
        }

        // Landing hint, drawn under placed bricks.
        if (target != null && dragging != null) {
            val (tx, ty) = target
            drawRoundRect(
                color = duo.blend.copy(alpha = Alpha.dim),
                topLeft = Offset(tx * stud, (plate.gridH - ty - dragging.shape.h) * row),
                size = Size(dragging.shape.w * stud, dragging.shape.h * row),
                cornerRadius = CornerRadius(stud * 0.1f),
            )
        }

        // Bottom rows first so upper bricks overlap the nubs below.
        plate.placed
            .sortedBy { it.y }
            .forEach { p ->
                val b = plate.block(p.blockId) ?: return@forEach
                drawBrick(
                    block = b,
                    atlas = atlas,
                    left = p.x * stud,
                    bottom = (plate.gridH - p.y) * row,
                    studPx = stud,
                )
            }
    }
}

@Composable
private fun TrayBrick(
    block: Block,
    atlas: BrickAtlas,
    faded: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val density = LocalDensity.current
    val studPx = with(density) { TrayStud.toPx() }
    val rowDp = TrayStud * BrickArt.ROW_RATIO
    val nubDp = TrayStud * BrickArt.NUB_RATIO
    var origin by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        Modifier
            .size(TrayStud * block.shape.w, rowDp * block.shape.h + nubDp)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragStart = { local -> onDragStart(origin + local) },
                    onDrag = { change, delta -> change.consume(); onDrag(delta) },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            },
    ) {
        drawBrick(
            block = block,
            atlas = atlas,
            left = 0f,
            bottom = size.height,
            studPx = studPx,
            alpha = if (faded) Alpha.unlit else 1f,
        )
    }
}

@Preview(name = "Build", showBackground = true, heightDp = 900)
@Composable
private fun PreviewBuild() = AmpersandTheme(darkTheme = true) {
    val s = MockCouple.state()
    Surface(color = MaterialTheme.colorScheme.background) {
        BuildScreen(s, s.plate, { _, _, _ -> })
    }
}
