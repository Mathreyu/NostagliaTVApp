package com.example.newtv.ui.screens

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.newtv.R
import com.example.newtv.engine.Program
import com.example.newtv.ui.LiveBoard
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.Spacing
import com.example.newtv.ui.components.GuideCellModel
import com.example.newtv.ui.components.GuideGrid
import com.example.newtv.ui.components.GuideMetrics
import com.example.newtv.ui.components.GuideRowModel
import com.example.newtv.ui.components.LiveChannelCard
import com.example.newtv.ui.components.NoSignalCard
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.OsdHeading
import com.example.newtv.ui.components.ScreenScaffold
import com.example.newtv.ui.components.rememberInitialFocus
import com.example.newtv.ui.components.tryRequestFocus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val GUIDE_VISIBLE_MINUTES = 120
private const val GUIDE_WINDOW_MINUTES = 150

/** "Live" home: one card per channel, a compact "coming up" guide and the action row. */
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onChannelSelected: (String) -> Unit,
    onChannelFocused: (String) -> Unit,
    onEnableKidSafe: () -> Unit,
    onExitKidSafe: () -> Unit,
    onAddChannel: () -> Unit,
    onParentSettings: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val channels = remember(uiState.channels) { LiveBoard.ordered(uiState.channels) }
    val rowState = rememberLazyListState()
    val tileFocus = remember { FocusRequester() }
    val buttonFocus = rememberInitialFocus(enabled = channels.isEmpty())

    // Restore focus to the last focused channel (or the first one) when Home appears.
    val restoreIndex = channels.indexOfFirst { it.id == uiState.lastFocusedChannelId }.coerceAtLeast(0)
    LaunchedEffect(channels.isNotEmpty()) {
        if (channels.isNotEmpty()) {
            rowState.scrollToItem(restoreIndex)
            withFrameNanos { }
            tileFocus.tryRequestFocus()
        }
    }

    ScreenScaffold(kidSafe = uiState.kidSafeEnabled, spacing = Spacing.S) {
        if (channels.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                NoSignalCard(
                    title = stringResource(R.string.no_signal),
                    message = stringResource(
                        if (uiState.kidSafeEnabled) R.string.no_signal_kid_safe else R.string.no_signal_no_channels
                    )
                ) {
                    HomeActions(
                        uiState.kidSafeEnabled, onEnableKidSafe, onExitKidSafe, onAddChannel,
                        onParentSettings, Modifier.focusRequester(buttonFocus)
                    )
                }
            }
        } else {
            LiveSection(uiState, channels, rowState, tileFocus, restoreIndex, onChannelSelected, onChannelFocused, zone)
            GuideSection(uiState, channels, onChannelSelected, zone, Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                HomeActions(
                    uiState.kidSafeEnabled, onEnableKidSafe, onExitKidSafe, onAddChannel,
                    onParentSettings, Modifier.focusRequester(buttonFocus)
                )
            }
        }
    }
}

@Composable
private fun LiveSection(
    uiState: MainUiState,
    channels: List<com.example.newtv.data.ChannelEntity>,
    rowState: androidx.compose.foundation.lazy.LazyListState,
    tileFocus: FocusRequester,
    restoreIndex: Int,
    onChannelSelected: (String) -> Unit,
    onChannelFocused: (String) -> Unit,
    zone: ZoneId
) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
        OsdHeading(stringResource(R.string.home_live_now))
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(Spacing.L),
            contentPadding = PaddingValues(horizontal = Spacing.S, vertical = 10.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                val program = uiState.nowPlaying[channel.id]
                val isRestoreTarget = channel.id == channels[restoreIndex].id
                LiveChannelCard(
                    number = channel.number,
                    name = channel.name,
                    nowTitle = program?.show?.title ?: stringResource(R.string.no_signal),
                    nowSubtitle = if (program != null) {
                        stringResource(
                            R.string.live_subtitle,
                            stringResource(R.string.episode_code, program.season, program.episode),
                            timeFormat.format(program.endTime.atZone(zone))
                        )
                    } else {
                        stringResource(R.string.no_signal_no_shows)
                    },
                    progress = program?.progress(uiState.now) ?: 0f,
                    accent = Color(channel.accentColor),
                    onClick = { onChannelSelected(channel.id) },
                    modifier = Modifier
                        .width(264.dp)
                        .height(120.dp)
                        .then(if (isRestoreTarget) Modifier.focusRequester(tileFocus) else Modifier)
                        .onFocusChanged { if (it.hasFocus) onChannelFocused(channel.id) }
                )
            }
        }
    }
}

@Composable
private fun GuideSection(
    uiState: MainUiState,
    channels: List<com.example.newtv.data.ChannelEntity>,
    onChannelSelected: (String) -> Unit,
    zone: ZoneId,
    modifier: Modifier
) {
    val start = remember(uiState.now, zone) { LiveBoard.guideStart(uiState.now, zone) }
    val noSignal = stringResource(R.string.no_signal)
    val res = LocalContext.current.resources
    val rows = remember(channels, uiState.guide, start, noSignal) {
        channels.map { channel ->
            val programs = uiState.guide[channel.id].orEmpty()
            GuideRowModel(
                id = channel.id,
                number = channel.number,
                name = channel.name,
                accent = Color(channel.accentColor),
                cells = if (programs.isEmpty()) {
                    // No shows: one "no signal" cell across the window keeps the row selectable.
                    listOf(
                        GuideCellModel(
                            "${channel.id}-none", noSignal, null, start,
                            start.plusSeconds(GUIDE_WINDOW_MINUTES * 60L)
                        )
                    )
                } else {
                    programs.map { it.toGuideCell(res) }
                }
            )
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
        OsdHeading(stringResource(R.string.home_coming_up))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val metrics = GuideMetrics.Compact
            val slotWidth = (maxWidth - metrics.channelColumnWidth) / GUIDE_VISIBLE_MINUTES
            GuideGrid(
                rows = rows,
                startTime = start,
                slotWidthPerMinute = slotWidth,
                onCellClick = { rowId, _ -> onChannelSelected(rowId) },
                metrics = metrics,
                fixedWindowMinutes = GUIDE_WINDOW_MINUTES,
                now = uiState.now,
                zone = zone,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

internal fun Program.toGuideCell(res: Resources): GuideCellModel = GuideCellModel(
    id = "${channel.id}-${startTime.epochSecond}",
    title = show.title,
    subtitle = res.getString(R.string.episode_code, season, episode),
    start = startTime,
    end = endTime
)

@Composable
private fun HomeActions(
    kidSafe: Boolean,
    onEnableKidSafe: () -> Unit,
    onExitKidSafe: () -> Unit,
    onAddChannel: () -> Unit,
    onParentSettings: () -> Unit,
    primaryModifier: Modifier
) {
    if (kidSafe) {
        NostalgiaButton(
            text = stringResource(R.string.exit_kid_safe),
            onClick = onExitKidSafe,
            modifier = primaryModifier
        )
        NostalgiaButton(
            text = stringResource(R.string.parent_settings),
            onClick = onParentSettings,
            secondary = true
        )
    } else {
        NostalgiaButton(
            text = stringResource(R.string.enable_kid_safe),
            onClick = onEnableKidSafe,
            modifier = primaryModifier
        )
        NostalgiaButton(
            text = stringResource(R.string.add_channel),
            onClick = onAddChannel,
            secondary = true
        )
    }
}
