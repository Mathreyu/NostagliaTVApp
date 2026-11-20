package com.example.newtv.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Widens a container by [horizontal] on both sides without moving its siblings, so a scrolling
 * or clipping container can leave room for focus scale and glow while its content (given the
 * same amount as padding) still lines up with the screen grid.
 */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = if (constraints.hasBoundedWidth) constraints.minWidth + 2 * extra else constraints.minWidth,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + 2 * extra else constraints.maxWidth
        )
    )
    val width = (placeable.width - 2 * extra).coerceAtLeast(0)
    layout(width, placeable.height) { placeable.place(-extra, 0) }
}
