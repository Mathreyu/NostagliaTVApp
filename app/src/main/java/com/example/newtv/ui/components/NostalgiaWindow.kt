package com.example.newtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

private val Bezel = Color(0xFF1C2038)
private val BezelEdge = Color(0xFF3A4066)

/**
 * TV-frame container: a chunky bezel around a dark "screen" with an OSD title bar
 * (title in phosphor green, a power LED on the right).
 */
@Composable
fun NostalgiaWindow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val outer = NostalgiaShapes.Large
    Column(
        modifier = modifier
            .background(Bezel, outer)
            .border(2.dp, BezelEdge, outer)
            .padding(8.dp)
            .background(NostalgiaColors.Surface, NostalgiaShapes.Medium)
            .border(2.dp, NostalgiaColors.GuideLine, NostalgiaShapes.Medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = title.uppercase(), style = osdTextStyle(32), modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(10.dp)
                    .background(NostalgiaColors.Error, CircleShape)
            )
            Box(Modifier.width(2.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(NostalgiaColors.GuideLine)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun WindowPreview() {
    NewTvTheme {
        Box(Modifier.background(NostalgiaColors.Background).padding(48.dp)) {
            NostalgiaWindow(title = "Channel setup") {
                Text("Pick a channel to edit")
                NostalgiaButton(text = "Back", onClick = {}, secondary = true)
            }
        }
    }
}
