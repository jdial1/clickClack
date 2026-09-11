package com.ampersand.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickAtlas
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Duo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Gap = 8.dp
private val RankShape = BrickShape(6, BrickHeight.MEDIUM)

/**
 * Drag to put things in order — a stack of 6-stud bricks, one per item.
 *
 * Notes on why it is built this way, because the obvious version does not work:
 *
 * - Rows are wrapped in [key] so a row keeps its identity across a reorder.
 *   Without that, Compose reuses slots positionally and every row visually
 *   teleports instead of moving.
 * - The gesture is keyed on the *item*, not on the list. Keying on the list
 *   restarts `pointerInput` the instant the order changes, which cancels the
 *   drag on the first swap -- the single most common bug in hand-rolled
 *   reorderable lists.
 * - The live list is read through [rememberUpdatedState] so the gesture always
 *   resolves the item's current index rather than the one captured at drag
 *   start.
 * - Everything except the row under the finger animates its placement, so the
 *   stack reflows rather than snapping.
 */
@Composable
fun AmpDragRank(
    items: List<String>,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onReorder: (List<String>) -> Unit,
) {
    val sounds = LocalSounds.current
    val latest by rememberUpdatedState(items)
    var dragging by remember { mutableStateOf<String?>(null) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // One colour per position, so the stack reads as a set of loose bricks and
    // a swap is obvious at a glance.
    val colors = remember(items.size) { List(items.size) { BrickColor.at(it) } }
    val atlas = rememberBricks(colors.map { it.asset(RankShape) })

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val studDp = maxWidth / RankShape.w
        val rowHeight = studDp * (RankShape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)
        val slotPx = with(LocalDensity.current) { (rowHeight + Gap).toPx() }

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            items.forEachIndexed { index, item ->
                key(item) {
                    val isDragging = dragging == item

                    RankRow(
                        position = index + 1,
                        label = item,
                        brick = colors[index],
                        atlas = atlas,
                        rowHeight = rowHeight,
                        isDragging = isDragging,
                        enabled = enabled,
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .then(
                                if (isDragging) {
                                    Modifier.offset { IntOffset(0, offsetY.roundToInt()) }
                                } else {
                                    Modifier.animatePlacement()
                                },
                            ),
                        handleModifier = Modifier.pointerInput(item, enabled) {
                            if (!enabled) return@pointerInput
                            detectDragGestures(
                                onDragStart = {
                                    sounds.play(Sfx.PICK_UP)
                                    dragging = item
                                    offsetY = 0f
                                },
                                onDragEnd = {
                                    sounds.play(Sfx.PLACE)
                                    dragging = null
                                    offsetY = 0f
                                },
                                onDragCancel = {
                                    dragging = null
                                    offsetY = 0f
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    offsetY += delta.y
                                    val from = latest.indexOf(item)
                                    if (from < 0) return@detectDragGestures
                                    val shift = (offsetY / slotPx).roundToInt()
                                    val to = (from + shift).coerceIn(0, latest.lastIndex)
                                    if (to != from) {
                                        sounds.play(Sfx.TAP)
                                        onReorder(
                                            latest.toMutableList()
                                                .apply { add(to, removeAt(from)) },
                                        )
                                        // Keep the row under the finger: its slot
                                        // just moved by (to - from).
                                        offsetY -= (to - from) * slotPx
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RankRow(
    position: Int,
    label: String,
    brick: BrickColor,
    atlas: BrickAtlas,
    rowHeight: Dp,
    isDragging: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    handleModifier: Modifier,
) {
    val lift by animateFloatAsState(
        if (isDragging) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "lift",
    )
    val ink = Duo.inkOn(Color(brick.argb))

    Box(
        modifier
            .fillMaxWidth()
            .height(rowHeight)
            .graphicsLayer {
                scaleX = 1f + 0.02f * lift
                scaleY = 1f + 0.02f * lift
                shadowElevation = 14f * lift
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawBrickArt(
                atlas = atlas,
                asset = brick.asset(RankShape),
                studs = RankShape.w,
                rows = RankShape.h,
                left = 0f,
                bottom = size.height,
                studPx = size.width / RankShape.w,
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                // Clear the nubs along the top edge.
                .padding(start = 16.dp, top = rowHeight * BrickArt.NUB_RATIO),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = ink.copy(alpha = 0.65f),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                handleModifier.size(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Grip(
                    color = if (enabled) ink.copy(alpha = 0.7f) else ink.copy(alpha = 0.3f),
                )
            }
        }
    }
}

/** Six dots. The universally understood "you can move this". */
@Composable
private fun Grip(color: Color) {
    Canvas(Modifier.size(width = 12.dp, height = 18.dp)) {
        val r = 1.6.dp.toPx()
        val colX = listOf(r, size.width - r)
        val rowY = listOf(r, size.height / 2f, size.height - r)
        colX.forEach { x ->
            rowY.forEach { y ->
                drawCircle(color = color, radius = r, center = Offset(x, y))
            }
        }
    }
}

/**
 * Animates a child to its new position whenever layout moves it.
 *
 * Compose has no built-in equivalent outside lazy lists, and this is what makes
 * the reorder read as movement rather than a jump.
 */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember {
        mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null)
    }
    this
        .onPlaced { coordinates -> target = coordinates.positionInParent().round() }
        .offset {
            val anim = animatable
                ?: Animatable(target, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != target) {
                scope.launch {
                    anim.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
            anim.value - target
        }
}
