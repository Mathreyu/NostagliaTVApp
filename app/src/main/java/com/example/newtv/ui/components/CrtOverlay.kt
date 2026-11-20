package com.example.newtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

/**
 * Subtle CRT treatment drawn over the content: horizontal scanlines, a soft vignette and
 * rounded "glass" corners. All geometry is built once per size in drawWithCache.
 */
fun Modifier.crtOverlay(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    return drawWithCache {
        val w = size.width
        val h = size.height
        val step = 3.dp.toPx().coerceAtLeast(2f)
        val thick = (step / 3f).coerceAtLeast(1f)
        val scan = Path()
        var y = 0f
        while (y < h) {
            scan.addRect(Rect(0f, y, w, y + thick))
            y += step
        }
        val vignette = Brush.radialGradient(
            0.62f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.40f),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h) * 0.75f
        )
        val corner = NostalgiaShapes.ScreenCorner.toPx()
        val corners = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, w, h))
            addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(corner)))
        }
        val scanColor = Color.Black.copy(alpha = 0.14f)
        onDrawWithContent {
            drawContent()
            drawPath(scan, scanColor)
            drawRect(vignette)
            drawPath(corners, Color.Black)
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun CrtOverlayPreview() {
    NewTvTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(NostalgiaColors.GuideBlue)
                .crtOverlay()
        ) {
            Text("CRT OVERLAY", style = osdTextStyle(48), modifier = Modifier.align(Alignment.Center))
        }
    }
}
