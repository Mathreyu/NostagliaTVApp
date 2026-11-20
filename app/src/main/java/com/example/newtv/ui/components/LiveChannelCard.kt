package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.newtv.ui.ChannelAccents
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaFocus
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

/**
 * A channel tile: channel bug + name, what is on now, and a thin progress bar for the
 * current slot. Unfocused border is the channel accent at low alpha; focus is white + glow + scale.
 */
@Composable
fun LiveChannelCard(
    number: Int,
    name: String,
    nowTitle: String,
    nowSubtitle: String?,
    progress: Float,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = NostalgiaShapes.Medium
    val focusBorder = Border(BorderStroke(NostalgiaFocus.BorderWidth, NostalgiaColors.Focus), 0.dp, shape)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = NostalgiaFocus.Scale),
        glow = CardDefaults.glow(
            focusedGlow = Glow(NostalgiaFocus.GlowColor, NostalgiaFocus.GlowElevation)
        ),
        border = CardDefaults.border(
            border = Border(BorderStroke(2.dp, accent.copy(alpha = 0.35f)), 0.dp, shape),
            focusedBorder = focusBorder,
            pressedBorder = focusBorder
        ),
        colors = CardDefaults.colors(
            containerColor = NostalgiaColors.GuideBlueDeep,
            focusedContainerColor = NostalgiaColors.GuideBlue
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                ChannelBug(number = number, accent = accent)
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = NostalgiaColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = nowTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!nowSubtitle.isNullOrBlank()) {
                    Text(
                        text = nowSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NostalgiaColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Black.copy(alpha = 0.45f), NostalgiaShapes.Small)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(accent, NostalgiaShapes.Small)
                )
            }
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun LiveChannelCardPreview() {
    NewTvTheme {
        Row(
            Modifier.background(NostalgiaColors.Background).padding(48.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ChannelAccents.take(3).forEachIndexed { i, c ->
                LiveChannelCard(
                    number = i + 3,
                    name = "Saturday Morning",
                    nowTitle = "Cosmic Cat Adventures",
                    nowSubtitle = "S2E05 - The Moon Heist",
                    progress = 0.3f + 0.2f * i,
                    accent = c,
                    onClick = {},
                    modifier = Modifier.width(260.dp).height(150.dp)
                )
            }
        }
    }
}
