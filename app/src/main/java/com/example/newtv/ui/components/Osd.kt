package com.example.newtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.newtv.ui.ChannelAccents
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** OSD-style channel number badge, e.g. "CH 07". */
@Composable
fun ChannelBug(number: Int, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(NostalgiaColors.Background.copy(alpha = 0.85f), NostalgiaShapes.Small)
            .border(2.dp, accent, NostalgiaShapes.Small)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "CH %02d".format(number),
            style = osdTextStyle(22, accent),
            maxLines = 1
        )
    }
}

private val ClockFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** Current local time in OSD style; recomposes once a minute, aligned to the minute boundary. */
@Composable
fun OsdClock(
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault()
) {
    var now by remember { mutableStateOf(ZonedDateTime.now(zone)) }
    LaunchedEffect(zone) {
        while (true) {
            now = ZonedDateTime.now(zone)
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }
    Text(
        text = now.format(ClockFormat).uppercase(),
        style = osdTextStyle(30),
        maxLines = 1,
        modifier = modifier
    )
}

enum class OsdTone { Info, Error }

/** OSD-style message banner for info and error messages. */
@Composable
fun OsdBanner(text: String, tone: OsdTone = OsdTone.Info, modifier: Modifier = Modifier) {
    val color = if (tone == OsdTone.Error) NostalgiaColors.Error else NostalgiaColors.OsdGreen
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(NostalgiaColors.Background.copy(alpha = 0.92f), NostalgiaShapes.Small)
            .border(2.dp, color.copy(alpha = 0.7f), NostalgiaShapes.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(8.dp)
                .background(color)
        )
        Text(
            text = (if (tone == OsdTone.Error) "ERROR  " else "INFO  ") + text,
            style = osdTextStyle(26, color),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun OsdPreview() {
    NewTvTheme {
        Column(
            Modifier
                .background(NostalgiaColors.Background)
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChannelAccents.forEachIndexed { i, c -> ChannelBug(number = i * 3 + 2, accent = c) }
            }
            OsdClock()
            OsdBanner("Saturday Morning is on the air")
            OsdBanner("Wrong PIN. Try again.", OsdTone.Error)
        }
    }
}
