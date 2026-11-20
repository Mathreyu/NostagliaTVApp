package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.newtv.R
import com.example.newtv.engine.Program
import com.example.newtv.ui.LiveBoard
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.Spacing
import com.example.newtv.ui.components.ChannelBug
import com.example.newtv.ui.components.GuideGrid
import com.example.newtv.ui.components.GuideMetrics
import com.example.newtv.ui.components.GuideRowModel
import com.example.newtv.ui.components.ListingRow
import com.example.newtv.ui.components.NoSignalCard
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.OsdHeading
import com.example.newtv.ui.components.ScreenScaffold
import com.example.newtv.ui.components.rememberInitialFocus
import com.example.newtv.ui.osdTextStyle
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SCHEDULE_WINDOW_MINUTES = 180

/** Single-channel cable guide: a timeline strip on top and the full listing below it. */
@Composable
fun ScheduleScreen(
    uiState: MainUiState,
    channelId: String,
    onBack: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var selected by remember(channelId) { mutableStateOf<Program?>(null) }
    val firstFocus = rememberInitialFocus()
    val channel = uiState.channels.firstOrNull { it.id == channelId }
    val accent = channel?.let { Color(it.accentColor) } ?: NostalgiaColors.OsdGreen
    val programs = remember(uiState.currentProgram, uiState.schedule) {
        listOfNotNull(uiState.currentProgram) + uiState.schedule
    }
    val res = LocalContext.current.resources

    ScreenScaffold(kidSafe = uiState.kidSafeEnabled) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.L)) {
            Text(
                text = stringResource(R.string.schedule_title),
                style = osdTextStyle(40),
                maxLines = 1
            )
            if (channel != null) {
                ChannelBug(number = channel.number, accent = accent, textSize = 28)
                Text(
                    text = channel.name,
                    style = osdTextStyle(32, accent),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            NostalgiaButton(
                text = stringResource(R.string.back),
                onClick = onBack,
                secondary = true,
                modifier = if (programs.isEmpty()) Modifier.focusRequester(firstFocus) else Modifier
            )
        }

        if (channel != null && programs.isNotEmpty()) {
            val start = remember(uiState.now, zone) { LiveBoard.guideStart(uiState.now, zone) }
            val row = remember(programs, channel) {
                GuideRowModel(channel.id, channel.number, channel.name, accent, programs.map { it.toGuideCell(res) })
            }
            val metrics = GuideMetrics.Strip
            BoxWithConstraints(Modifier.fillMaxWidth().height(metrics.headerHeight + metrics.rowHeight + metrics.rowGap * 2 + 2.dp)) {
                GuideGrid(
                    rows = listOf(row),
                    startTime = start,
                    slotWidthPerMinute = (maxWidth - metrics.channelColumnWidth) / SCHEDULE_WINDOW_MINUTES,
                    onCellClick = { _, cellId -> selected = programs.firstOrNull { it.toGuideCell(res).id == cellId } },
                    metrics = metrics,
                    fixedWindowMinutes = SCHEDULE_WINDOW_MINUTES,
                    now = uiState.now,
                    zone = zone
                )
            }
        }

        if (programs.isEmpty()) {
            NoSignalCard(
                title = stringResource(R.string.no_signal),
                message = stringResource(R.string.no_signal_no_shows),
                barsWidth = 220.dp,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        } else {
            OsdHeading(stringResource(R.string.listings))
        }
        if (programs.isNotEmpty()) {
            Column(Modifier.weight(1f)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
                    itemsIndexed(programs) { index, item ->
                        ListingRow(
                            time = formatter.format(item.startTime.atZone(zone)),
                            title = item.show.title,
                            episode = stringResource(R.string.episode_code, item.season, item.episode),
                            accent = accent,
                            live = !uiState.now.isBefore(item.startTime) && uiState.now.isBefore(item.endTime),
                            onClick = { selected = item },
                            modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier
                        )
                    }
                }
            }
        }
        selected?.let { program ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OsdBanner(
                    text = stringResource(
                        R.string.program_airs_at,
                        program.show.title,
                        program.season,
                        program.episode,
                        formatter.format(program.startTime.atZone(zone)),
                        program.channel.number
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}
