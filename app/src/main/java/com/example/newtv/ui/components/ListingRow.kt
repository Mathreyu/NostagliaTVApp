package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaFocus
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

/**
 * One line of a cable-guide listing: time, title, episode code. Focusable; focus inverts to
 * white like a guide cell. [live] marks the airing slot with an accent edge.
 */
@Composable
fun ListingRow(
    time: String,
    title: String,
    episode: String,
    accent: Color,
    live: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = NostalgiaShapes.Small
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NostalgiaColors.GuideBlueDeep,
            contentColor = NostalgiaColors.TextPrimary,
            focusedContainerColor = NostalgiaColors.Focus,
            focusedContentColor = NostalgiaColors.OnFocus,
            pressedContainerColor = NostalgiaColors.FocusGlow,
            pressedContentColor = NostalgiaColors.OnFocus
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, NostalgiaColors.GuideLine), 0.dp, shape),
            focusedBorder = Border(BorderStroke(NostalgiaFocus.BorderWidth, NostalgiaColors.FocusGlow), 0.dp, shape)
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(NostalgiaFocus.GlowColor, 10.dp))
    ) {
        Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(if (live) accent else Color.Transparent)
            )
            // Colour comes from the surface so focus can invert it; Unspecified inherits it.
            Text(
                text = time,
                style = osdTextStyle(28, Color.Unspecified, glow = false),
                maxLines = 1,
                modifier = Modifier.padding(start = 14.dp).width(150.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = episode,
                style = osdTextStyle(26, Color.Unspecified, glow = false),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}
