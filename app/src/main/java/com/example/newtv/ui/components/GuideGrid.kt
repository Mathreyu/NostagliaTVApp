package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class GuideCellModel(
    val id: String,
    val title: String,
    val subtitle: String?,
    val start: Instant,
    val end: Instant
)

data class GuideRowModel(
    val id: String,
    val number: Int,
    val name: String,
    val accent: Color,
    val cells: List<GuideCellModel>
)

private val MinCellWidth = 40.dp
private const val SLOT_MINUTES = 30L
private const val MIN_WINDOW_MINUTES = 180L

/** Sizing of a [GuideGrid]: the full guide uses [Regular], the Home preview uses [Compact]. */
data class GuideMetrics(
    val channelColumnWidth: Dp,
    val rowHeight: Dp,
    val headerHeight: Dp,
    val headerTextSize: Int,
    val showEpisodeLine: Boolean,
    val rowGap: Dp,
    /** Lines the channel name may use in the channel column (compact guide wraps to two). */
    val channelNameLines: Int = 1
) {
    companion object {
        val Regular = GuideMetrics(230.dp, 72.dp, 44.dp, 26, true, 4.dp)
        /** Single-channel strip on the schedule screen. */
        val Strip = GuideMetrics(230.dp, 60.dp, 38.dp, 24, true, 3.dp, channelNameLines = 2)
        val Compact = GuideMetrics(224.dp, 44.dp, 30.dp, 22, false, 3.dp, channelNameLines = 2)
    }
}

/**
 * Classic cable guide: sticky time header, channel column on the left, and a vertical list of
 * rows whose cells are sized by duration. All rows and the header share one horizontal scroll.
 * Cells are D-pad focusable; focused cell inverts to white.
 */
@Composable
fun GuideGrid(
    rows: List<GuideRowModel>,
    startTime: Instant,
    slotWidthPerMinute: Dp,
    onCellClick: (rowId: String, cellId: String) -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    metrics: GuideMetrics = GuideMetrics.Regular,
    /** Fixed window length in minutes; when null it covers all cells (at least three hours). */
    fixedWindowMinutes: Int? = null,
    /** Cells that contain this instant get an accent edge ("on now"). */
    now: Instant? = null
) {
    val scroll = rememberScrollState()
    val windowMinutes = remember(rows, startTime, fixedWindowMinutes) {
        if (fixedWindowMinutes != null) {
            fixedWindowMinutes.toLong()
        } else {
            val lastEnd = rows.maxOfOrNull { r -> r.cells.maxOfOrNull { it.end } ?: startTime } ?: startTime
            val mins = Duration.between(startTime, lastEnd).toMinutes()
            // Round up to whole slots, at least three hours.
            (((maxOf(mins, MIN_WINDOW_MINUTES) + SLOT_MINUTES - 1) / SLOT_MINUTES) * SLOT_MINUTES)
        }
    }
    val rowHeight = metrics.rowHeight
    val headerHeight = metrics.headerHeight
    val totalWidth = slotWidthPerMinute * windowMinutes.toFloat()
    val labelFormat = remember(zone) { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(zone) }

    Column(modifier = modifier) {
        // Time header.
        Row(Modifier.fillMaxWidth().height(headerHeight)) {
            Box(Modifier.width(metrics.channelColumnWidth))
            Box(Modifier.weight(1f).height(headerHeight).horizontalScroll(scroll, enabled = false)) {
                Box(Modifier.width(totalWidth).height(headerHeight)) {
                    var m = 0L
                    while (m < windowMinutes) {
                        val x = slotWidthPerMinute * m.toFloat()
                        Row(
                            Modifier.offset(x = x).height(headerHeight),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(2.dp).height(headerHeight).background(NostalgiaColors.GuideLine))
                            Text(
                                text = labelFormat.format(startTime.plusSeconds(m * 60)).uppercase(),
                                style = osdTextStyle(metrics.headerTextSize),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        m += SLOT_MINUTES
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(NostalgiaColors.GuideLine))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(metrics.rowGap),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = metrics.rowGap)
        ) {
            items(rows, key = { it.id }) { row ->
                Row(Modifier.fillMaxWidth().height(rowHeight)) {
                    ChannelHeaderCell(row, metrics)
                    Box(Modifier.weight(1f).height(rowHeight).horizontalScroll(scroll)) {
                        Box(Modifier.width(totalWidth).height(rowHeight)) {
                            row.cells.forEach { cell ->
                                val startMin = Duration.between(startTime, cell.start).toMinutes().coerceAtLeast(0L)
                                val endMin = Duration.between(startTime, cell.end).toMinutes().coerceAtMost(windowMinutes)
                                if (endMin > startMin) {
                                    GuideCell(
                                        cell = cell,
                                        accent = row.accent.takeIf { now != null && now >= cell.start && now < cell.end },
                                        showSubtitle = metrics.showEpisodeLine,
                                        modifier = Modifier
                                            .offset(x = slotWidthPerMinute * startMin.toFloat())
                                            .width(
                                                (slotWidthPerMinute * (endMin - startMin).toFloat() - 4.dp)
                                                    .coerceAtLeast(MinCellWidth)
                                            )
                                            .height(rowHeight),
                                        onClick = { onCellClick(row.id, cell.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelHeaderCell(row: GuideRowModel, metrics: GuideMetrics) {
    Row(
        modifier = Modifier
            .width(metrics.channelColumnWidth)
            .height(metrics.rowHeight)
            .padding(end = 6.dp)
            .background(NostalgiaColors.Surface, NostalgiaShapes.Small)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChannelBug(number = row.number, accent = row.accent)
        Text(
            text = row.name,
            style = if (metrics.channelNameLines > 1 && metrics.rowHeight < 56.dp) {
                MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
            } else {
                MaterialTheme.typography.titleSmall
            },
            maxLines = metrics.channelNameLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GuideCell(
    cell: GuideCellModel,
    accent: Color?,
    showSubtitle: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = NostalgiaShapes.Small
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NostalgiaColors.GuideBlue,
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
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(NostalgiaFocus.GlowColor, 10.dp)
        )
    ) {
        Row(Modifier.fillMaxSize()) {
        if (accent != null) Box(Modifier.fillMaxHeight().width(5.dp).background(accent))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cell.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showSubtitle && !cell.subtitle.isNullOrBlank()) {
                Text(
                    text = cell.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun GuideGridPreview() {
    val start = Instant.parse("2026-01-10T14:00:00Z")
    fun cell(id: String, t: String, s: String?, from: Long, to: Long) =
        GuideCellModel(id, t, s, start.plusSeconds(from * 60), start.plusSeconds(to * 60))
    val rows = listOf(
        GuideRowModel("a", 2, "Saturday Morning", ChannelAccents[0], listOf(
            cell("a1", "Cosmic Cat", "S2E05", 0, 30), cell("a2", "Robo Pals", "S1E11", 30, 90),
            cell("a3", "Dino Dash", null, 90, 150), cell("a4", "Hero Hour", "Movie", 150, 240))),
        GuideRowModel("b", 3, "After School", ChannelAccents[1], listOf(
            cell("b1", "Sitcom Block", "S4E02", 0, 60), cell("b2", "Quiz Kids", "S1E01", 60, 120),
            cell("b3", "Talk Time", null, 120, 240))),
        GuideRowModel("c", 4, "Late Night", ChannelAccents[2], listOf(
            cell("c1", "Midnight Movie", "1996", 0, 150), cell("c2", "Static", null, 150, 240)))
    )
    NewTvTheme {
        Box(Modifier.background(NostalgiaColors.Background).padding(horizontal = 48.dp, vertical = 27.dp)) {
            GuideGrid(rows = rows, startTime = start, slotWidthPerMinute = 6.dp, onCellClick = { _, _ -> })
        }
    }
}
