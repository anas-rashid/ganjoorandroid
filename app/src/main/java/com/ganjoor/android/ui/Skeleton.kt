package com.ganjoor.android.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Placeholders shaped like what is about to arrive, so a part of the screen that is still loading
 * looks like that part and nothing else does. Everything already on screen stays put around it.
 */

/** Widths for placeholder lines, varied so a list of them reads as text rather than as stripes. */
private val WIDTHS = listOf(0.82f, 0.64f, 0.9f, 0.7f, 0.76f, 0.58f, 0.86f, 0.68f)

/** One slow pulse shared by every shape in a placeholder, so they breathe together. */
@Composable
private fun pulse(): Float {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton alpha",
    )
    return alpha
}

@Composable
private fun Bone(modifier: Modifier, alpha: Float, shape: Shape = RoundedCornerShape(6.dp)) {
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    )
}

/** Rows of a list: a title line and, with [twoLines], a shorter line under it. */
@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    rows: Int = 8,
    twoLines: Boolean = true,
    leadingCircle: Dp? = null,
) {
    val alpha = pulse()
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(rows) { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingCircle != null) {
                    Bone(Modifier.size(leadingCircle), alpha, CircleShape)
                    Box(Modifier.size(16.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bone(Modifier.fillMaxWidth(WIDTHS[i % WIDTHS.size]).height(14.dp), alpha)
                    if (twoLines) {
                        Bone(Modifier.fillMaxWidth(WIDTHS[(i + 3) % WIDTHS.size] * 0.7f).height(10.dp), alpha)
                    }
                }
            }
        }
    }
}

/** A poem on its way: a breadcrumb line, then couplets, the first half to the start, the second to the end. */
@Composable
fun SkeletonPoem(modifier: Modifier = Modifier, couplets: Int = 6) {
    val alpha = pulse()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Bone(Modifier.fillMaxWidth(0.5f).height(14.dp), alpha)
        Box(Modifier.height(8.dp))
        repeat(couplets) { i ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Bone(Modifier.fillMaxWidth(WIDTHS[i % WIDTHS.size]).height(18.dp), alpha)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Bone(Modifier.fillMaxWidth(WIDTHS[(i + 2) % WIDTHS.size]).height(18.dp), alpha)
                }
            }
        }
    }
}

/** Cards on their way, for the poet grid ([circle] portraits) or a book's contents. */
@Composable
fun SkeletonCards(modifier: Modifier = Modifier, cards: Int = 6, circle: Boolean = false) {
    val alpha = pulse()
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat((cards + 1) / 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (circle) Bone(Modifier.size(84.dp), alpha, CircleShape)
                        Bone(Modifier.fillMaxWidth(0.6f).height(14.dp), alpha)
                    }
                }
            }
        }
    }
}

/** The poets column on its way: portrait discs over short names. */
@Composable
fun SkeletonRail(modifier: Modifier = Modifier, disc: Dp = 56.dp) {
    val alpha = pulse()
    Column(
        modifier.fillMaxSize().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(8) {
            Bone(Modifier.size(disc), alpha, CircleShape)
            Bone(Modifier.fillMaxWidth(0.5f).aspectRatio(5f), alpha)
        }
    }
}
