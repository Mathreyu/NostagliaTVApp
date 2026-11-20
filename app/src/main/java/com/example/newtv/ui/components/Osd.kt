package com.example.newtv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.example.newtv.R
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
fun ChannelBug(number: Int, accent: Color, modifier: Modifier = Modifier, textSize: Int = 22) {
    Box(
        modifier = modifier
            .background(NostalgiaColors.Background.copy(alpha = 0.85f), NostalgiaShapes.Small)
            .border(2.dp, accent, NostalgiaShapes.Small)
            .padding(horizontal = (textSize / 3).dp, vertical = (textSize / 10).dp)
    ) {
        Text(
            text = stringResource(R.string.channel_bug, number),
            style = osdTextStyle(textSize, accent),
            maxLines = 1
        )
    }
}

private val ClockFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** Pins every [OsdClock] below it to a fixed time (previews and screenshot tests). */
val LocalFixedClockTime = staticCompositionLocalOf<ZonedDateTime?> { null }

/**
 * Current local time in OSD style; recomposes once a minute, aligned to the minute boundary.
 * A non-null [fixedTime] is shown as-is without ticking (previews and screenshot tests).
 */
@Composable
fun OsdClock(
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    fixedTime: ZonedDateTime? = LocalFixedClockTime.current
) {
    var now by remember { mutableStateOf(fixedTime ?: ZonedDateTime.now(zone)) }
    if (fixedTime == null) {
        LaunchedEffect(zone) {
            while (true) {
                now = ZonedDateTime.now(zone)
                delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
            }
        }
    }
    Text(
        text = (fixedTime ?: now).format(ClockFormat).uppercase(),
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

/** Padlock glyph drawn in [color]; used by the PIN pad title and the kid-safe badge. */
@Composable
fun PadlockIcon(modifier: Modifier = Modifier, color: Color = NostalgiaColors.OsdGreen) {
    Canvas(modifier.size(34.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f
        // Shackle.
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.06f),
            size = Size(w * 0.52f, h * 0.5f),
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        // Body.
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.42f),
            size = Size(w * 0.72f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        drawCircle(NostalgiaColors.Background, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.64f))
    }
}

/** "KID-SAFE" lock badge shown in the top bar while kid-safe mode is on. */
@Composable
fun KidSafeBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(2.dp, NostalgiaColors.OsdGreen, NostalgiaShapes.Small)
            .padding(start = 8.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PadlockIcon(Modifier.size(22.dp))
        Text(text = stringResource(R.string.kid_safe_badge), style = osdTextStyle(24), maxLines = 1)
    }
}

/** Persistent top bar: wordmark on the left, kid-safe badge and clock on the right. */
@Composable
fun OsdTopBar(
    kidSafe: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Five channel-accent bars: the "colour bars" motif.
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(24.dp)) {
                ChannelAccents.forEach { c ->
                    Box(Modifier.width(6.dp).fillMaxHeight().background(c))
                }
            }
            Text(
                text = stringResource(R.string.app_wordmark),
                style = osdTextStyle(36),
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp).weight(1f)
            )
            if (kidSafe) {
                KidSafeBadge()
                Spacer(Modifier.width(20.dp))
            }
            OsdClock()
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(NostalgiaColors.GuideLine))
    }
}

/** Section heading in OSD green followed by a rule, e.g. "LIVE NOW ────". */
@Composable
fun OsdHeading(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = text.uppercase(), style = osdTextStyle(26), maxLines = 1)
        Box(
            Modifier
                .padding(start = 12.dp)
                .weight(1f)
                .height(2.dp)
                .background(NostalgiaColors.GuideLine)
        )
    }
}

/** Muted one-line help text, as at the bottom of a cable-box setup menu. */
@Composable
fun HelpLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = osdTextStyle(22, NostalgiaColors.TextMuted, glow = false),
        maxLines = 2,
        modifier = modifier
    )
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
            OsdTopBar(kidSafe = true)
            OsdBanner("Saturday Morning is on the air")
            OsdBanner("Wrong PIN. Try again.", OsdTone.Error)
        }
    }
}
