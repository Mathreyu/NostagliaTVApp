package com.example.newtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.newtv.ui.Spacing

/**
 * Common frame of every main screen: the overscan safe area (48 x 27 dp) with the persistent
 * [OsdTopBar] on top and the screen's [content] below.
 */
@Composable
fun ScreenScaffold(
    kidSafe: Boolean,
    modifier: Modifier = Modifier,
    spacing: Dp = Spacing.M,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.SafeHorizontal, vertical = Spacing.SafeVertical),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        OsdTopBar(kidSafe = kidSafe)
        content()
    }
}
