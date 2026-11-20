package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.newtv.ui.ChannelAccents
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaFocus
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = osdTextStyle(30)
    )
}

@Composable
fun TileCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    accentSeed: String,
    modifier: Modifier = Modifier
) {
    val accent = accentColor(accentSeed)
    val shape = NostalgiaShapes.Medium
    val focusBorder = Border(BorderStroke(NostalgiaFocus.BorderWidth, NostalgiaColors.Focus), 0.dp, shape)
    Card(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(height),
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        glow = CardDefaults.glow(
            focusedGlow = Glow(NostalgiaFocus.GlowColor, NostalgiaFocus.GlowElevation)
        ),
        border = CardDefaults.border(
            border = Border(BorderStroke(2.dp, accent.copy(alpha = 0.55f)), 0.dp, shape),
            focusedBorder = focusBorder,
            pressedBorder = focusBorder
        ),
        colors = CardDefaults.colors(
            containerColor = NostalgiaColors.Surface,
            focusedContainerColor = NostalgiaColors.SurfaceRaised
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.30f), Color.Transparent)
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = NostalgiaColors.TextMuted,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

/** List row. When [onClick] is given the row is focusable and selectable (D-pad scrolls lists). */
@Composable
fun TileRow(
    title: String,
    subtitle: String,
    accentSeed: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accent = accentColor(accentSeed)
    val shape = NostalgiaShapes.Medium
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .width(96.dp)
                    .background(accent.copy(alpha = 0.30f), NostalgiaShapes.Small)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = NostalgiaColors.TextMuted
                )
            }
        }
    }
    if (onClick == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(NostalgiaColors.Surface, shape)
                .border(2.dp, accent.copy(alpha = 0.55f), shape)
        ) { content() }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = NostalgiaColors.Surface,
                focusedContainerColor = NostalgiaColors.SurfaceRaised,
                contentColor = NostalgiaColors.TextPrimary,
                focusedContentColor = NostalgiaColors.TextPrimary
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(2.dp, accent.copy(alpha = 0.55f)), 0.dp, shape),
                focusedBorder = Border(BorderStroke(NostalgiaFocus.BorderWidth, NostalgiaColors.Focus), 0.dp, shape)
            ),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(NostalgiaFocus.GlowColor, NostalgiaFocus.GlowElevation)
            )
        ) { content() }
    }
}

/** Stable accent for a string seed, always from [ChannelAccents]. */
internal fun accentColor(seed: String): Color {
    val index = (seed.hashCode() and Int.MAX_VALUE) % ChannelAccents.size
    return ChannelAccents[index]
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun TilePreview() {
    NewTvTheme {
        Column(
            Modifier.background(NostalgiaColors.Background).padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("Featured")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TileCard("Saturday Cartoons", "Ch 03", {}, 240.dp, 120.dp, "a")
                TileCard("After School", "Ch 05", {}, 240.dp, 120.dp, "b")
            }
            TileRow("Late Night", "Ch 09", "c")
        }
    }
}
