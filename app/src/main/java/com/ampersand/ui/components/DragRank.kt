package com.ampersand.ui.components

import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.Space
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
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Duo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Gap = 8.dp
private val RankShape = BrickShape(6, BrickHeight.MEDIUM)

/**
 * Drag-to-reorder list of 6-stud bricks, one per item.
 *
 * - Rows are wrapped in [key] so they keep identity across a reorder.
 * - `pointerInput` is keyed on the item, not the list; keying on the list cancels the drag on the first swap.
 * - [rememberUpdatedState] gives the gesture the item's current index.
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
    val feel = LocalFeel.current
    val latest by rememberUpdatedState(items)
    var dragging by remember { mutableStateOf<String?>(null) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Color per item (not per position), keyed on the set so reorders don't recolor.
    val colorOf = remember(items.toSet()) {
        items.withIndex().associate { (i, item) -> item to BrickColor.at(i) }
    }
    val atlas = rememberBricks(colorOf.values.map { it.asset(RankShape) })

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
                        brick = colorOf.getValue(item),
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
                        // The whole brick is the handle.
                        dragModifier = Modifier.pointerInput(item, enabled) {
                            if (!enabled) return@pointerInput
                            detectDragGestures(
                                onDragStart = {
                                    sounds.play(Sfx.PICK_UP)
                                    feel.play(Feel.PICK_UP)
                                    dragging = item
                                    offsetY = 0f
                                },
                                onDragEnd = {
                                    sounds.play(Sfx.PLACE)
                                    feel.play(Feel.PLACE)
                                    dragging = null
                                    offsetY = 0f
                                },
                                onDragCancel = {
                                    sounds.play(Sfx.PLACE)
                                    feel.play(Feel.REJECT)
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
                                        feel.play(Feel.TICK)
                                        onReorder(
                                            latest.toMutableList()
                                                .apply { add(to, removeAt(from)) },
                                        )
                                        // Keep the row under the finger after its slot moved.
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
    dragModifier: Modifier,
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
            .then(dragModifier)
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
                .padding(start = Space.l, end = Space.l, top = rowHeight * BrickArt.NUB_RATIO),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = ink.copy(alpha = Alpha.quiet),
            )
            Spacer(Modifier.width(Space.m))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Animates a child to its new position when layout moves it (no built-in outside lazy lists). */
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
