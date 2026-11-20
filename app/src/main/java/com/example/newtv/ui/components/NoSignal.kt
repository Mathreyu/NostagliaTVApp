package com.example.newtv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.newtv.R
import com.example.newtv.ui.ChannelAccents
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

private val BarGray = Color(0xFFC9CEDD)

/** The seven classic bars (neutral, then channel accents and red), left to right. */
private val Bars: List<Color> = listOf(
    BarGray,
    ChannelAccents[3],
    ChannelAccents[1],
    ChannelAccents[2],
    ChannelAccents[0],
    NostalgiaColors.Error,
    ChannelAccents[4]
)

/** SMPTE-like colour bars: seven bars, a reversed castellation strip and a black/white pluge row. */
@Composable
fun ColorBars(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .border(2.dp, NostalgiaColors.GuideLine, NostalgiaShapes.Small)
    ) {
        val w = size.width
        val h = size.height
        val barW = w / Bars.size
        val mainH = h * 0.67f
        val stripH = h * 0.08f
        Bars.forEachIndexed { i, c ->
            drawRect(c, Offset(i * barW, 0f), Size(barW + 1f, mainH))
        }
        // Reversed strip: blue / black / magenta / black ... under the main bars.
        val strip = listOf(
            ChannelAccents[4], Color.Black, ChannelAccents[0], Color.Black,
            ChannelAccents[1], Color.Black, BarGray
        )
        strip.forEachIndexed { i, c ->
            drawRect(c, Offset(i * barW, mainH), Size(barW + 1f, stripH))
        }
        // Bottom row: dark blue block, white block, dark purple block, black.
        val bottomTop = mainH + stripH
        val bottomH = h - bottomTop
        val blocks = listOf(
            NostalgiaColors.GuideBlueDeep to 0.2f,
            Color.White to 0.2f,
            Color(0xFF2A1450) to 0.2f,
            Color.Black to 0.4f
        )
        var x = 0f
        blocks.forEach { (c, frac) ->
            drawRect(c, Offset(x, bottomTop), Size(w * frac + 1f, bottomH))
            x += w * frac
        }
    }
}

/**
 * Friendly empty state: colour bars next to an OSD "NO SIGNAL" heading, a message and the
 * relevant [actions].
 */
@Composable
fun NoSignalCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    barsWidth: Dp = 300.dp,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .background(NostalgiaColors.Background.copy(alpha = 0.85f), NostalgiaShapes.Large)
            .border(2.dp, NostalgiaColors.GuideLine, NostalgiaShapes.Large)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorBars(Modifier.width(barsWidth).height(barsWidth * 0.56f))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = osdTextStyle(56, NostalgiaColors.Error), maxLines = 1)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = NostalgiaColors.TextMuted
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp),
                content = actions
            )
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun NoSignalPreview() {
    NewTvTheme {
        Column(Modifier.background(NostalgiaColors.Background).padding(48.dp)) {
            NoSignalCard(
                title = stringResource(R.string.no_signal),
                message = stringResource(R.string.no_signal_no_channels)
            ) {
                NostalgiaButton(text = stringResource(R.string.add_channel), onClick = {})
            }
        }
    }
}
