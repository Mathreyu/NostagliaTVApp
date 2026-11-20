package com.example.newtv.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.newtv.ui.NostalgiaColors

/**
 * Fades the top and/or bottom edge of a scroller into the window surface while more content
 * lies beyond it, so a partly clipped row reads as "keep scrolling" instead of a layout bug.
 */
fun Modifier.scrollEdgeFades(
    canScrollBackward: () -> Boolean,
    canScrollForward: () -> Boolean
): Modifier = drawWithContent {
    drawContent()
    val fade = 28.dp.toPx()
    val surface = NostalgiaColors.Surface
    if (canScrollBackward()) {
        drawRect(
            Brush.verticalGradient(listOf(surface, Color.Transparent), startY = 0f, endY = fade),
            size = Size(size.width, fade)
        )
    }
    if (canScrollForward()) {
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, surface), startY = size.height - fade, endY = size.height),
            topLeft = Offset(0f, size.height - fade),
            size = Size(size.width, fade)
        )
    }
}
