package com.example.newtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Surface

@Composable
fun NostalgiaBackground(content: @Composable () -> Unit) {
    val gridColor = Color(0xFF7A3FFF)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3B0A78),
            Color(0xFF0B0B14)
        )
    )
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .drawBehind {
                val step = 64f
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = gridColor.copy(alpha = 0.35f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.5f
                    )
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = gridColor.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f
                    )
                    y += step
                }
            }
    ) {
        content()
    }
}
