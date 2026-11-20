package com.example.newtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaShapes

@Composable
fun NostalgiaPanel(
    height: Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(NostalgiaColors.GuideBlueDeep, NostalgiaShapes.Medium)
            .border(2.dp, NostalgiaColors.GuideLine, NostalgiaShapes.Medium)
            .padding(12.dp)
    ) {
        content()
    }
}
