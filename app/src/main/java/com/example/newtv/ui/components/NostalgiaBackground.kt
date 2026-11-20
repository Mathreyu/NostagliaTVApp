package com.example.newtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.tv.material3.LocalContentColor
import com.example.newtv.ui.NostalgiaColors

/** Full-screen CRT backdrop: dark blue gradient with scanlines, vignette and rounded glass corners. */
@Composable
fun NostalgiaBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(NostalgiaColors.BackgroundTop, NostalgiaColors.Background)
                )
            )
            .crtOverlay()
    ) {
        CompositionLocalProvider(LocalContentColor provides NostalgiaColors.TextPrimary) {
            content()
        }
    }
}
